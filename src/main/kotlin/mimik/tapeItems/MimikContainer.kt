package mimik.tapeItems

import R
import VCRConfig
import mimik.helpers.attractors.HookAttractor
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import kolor.blue
import kolor.cyan
import kolor.green
import kolor.red
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import mimik.helpers.attractors.RequestAttractors
import mimik.helpers.printlnF
import mimik.helpers.toOkRequest
import mimik.helpers.toTapeData
import mimik.networkRouting.testingManager.Container
import mimik.networkRouting.testingManager.TestManager
import mimik.networkRouting.testingManager.boundActions
import mimik.networkRouting.testingManager.observe
import okhttp3.createResponse
import okreplay.OkReplayInterceptor
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Root data object of Mimik
 */
object MimikContainer {
    //    val store by lazy { MyObjectBox.builder().name("mimik.db").build() }
    val containers: MutableList<Container> = mutableListOf()
    var tapeCatalog = TapeCatalog()
        private set
    var logs: MutableList<String>? = null
    var connectionHolding: MutableList<HookAttractor>? = null
    val config by lazy { VCRConfig.getConfig }

    private val okreplay by lazy { OkReplayInterceptor() }
    private val requestList = mutableMapOf<String, Semaphore>()
    private val processingRequests: MutableMap<String, Semaphore> by lazy { Collections.synchronizedMap(requestList) }

    val responseLinker by lazy { ResponseLinkerUtil() }

    fun init() {
        tapeCatalog.loadTapeData()
    }

    fun reset() {
        containers.clear()
        tapeCatalog = TapeCatalog()
        init()
    }

    suspend fun processCall(call: ApplicationCall): okhttp3.Response {
        val callRequest: okhttp3.Request = call.toOkRequest()

//        if (containers.isEmpty()) {
//            return callRequest.createResponse(HttpStatusCode.PreconditionFailed) {
//                "Missing holding containers"
//                    .also { println(it.red()) }
//            }
//        }

        val callUrl = callRequest.url.toString()

        // Add a semaphore to key [callUrl], if no key exists.
        // Each key allows 1 active value, remainder are queued behind the value (semaphore)
        processingRequests.computeIfAbsent(callUrl) { Semaphore(1) }

        return processingRequests.getValue(callUrl).withPermit {
            println("Adding url lock for $callUrl".blue())
            val startTime = System.currentTimeMillis()

            try {
                doCallAction(call, callRequest)
            } finally {
                printlnF(
                    "Releasing lock (%d ms): %s".blue(),
                    System.currentTimeMillis() - startTime,
                    callUrl
                )
            }
        }
    }

    private suspend fun doCallAction(
        call: ApplicationCall,
        callRequest: okhttp3.Request
    ): okhttp3.Response {
        val bounds = TestManager.getManagerByID(call.callId)
        if (bounds != null) {
            when {
                Date().after(bounds.expireTime) -> {
                    val timeOver = ChronoUnit.SECONDS.between(
                        Date().toInstant(),
                        (bounds.expireTime ?: Date()).toInstant()
                    )
                    "Testing bounds for (%s) is expired. %s past.".format(
                        bounds.handle,
                        Duration.ofSeconds(timeOver).toString().removePrefix("PT")
                    )
                }

                !bounds.isEnabled.get() ->
                    "Test with handle ${bounds.handle} is not enabled (stopped)."

                bounds.tapes.isEmpty() ->
                    "Test with handle ${bounds.handle} has no tapes."

                else -> null
            }?.also { response ->
                println(response.red())
                return callRequest.createResponse(HttpStatusCode.Forbidden) { response }
            }
        }

        val (resp, chap) = tapeCatalog.findResponseByQuery(callRequest, bounds?.tapes)
        resp.item?.also { tape ->
            printlnF(
                "%sUsing response tape %s".green(),
                bounds?.handle?.let { "[$it] " } ?: "",
                tape.name
            )
            val chain = tape.requestToChain(callRequest)
            okreplay.start(config, tape)
            withContext(Dispatchers.IO) {
                bounds.observe(tape) {
                    okreplay.intercept(chain)
                        .boundActions(callRequest, bounds, chap)
                }
            }?.also { return it }

            return callRequest.createResponse(HttpStatusCode.PreconditionFailed) {
                R["processCall_InvalidUrl", ""]
                    .also { println(it.red()) }
            }
        }

        if (bounds != null) return callRequest.createResponse(HttpStatusCode.Forbidden) {
            "Test bounds [${bounds.handle}] has no matching recordings for ${callRequest.url}."
                .also { println(it.red()) }
        }

        val hostTape = tapeCatalog.findTapeByQuery(callRequest)
        return when (hostTape.status) {
            HttpStatusCode.Found -> {
                hostTape.item?.let {
                    println("Response not found; Using tape ${it.name}".cyan())

                    val chain = it.requestToChain(callRequest)
                    okreplay.start(config, it)
                    withContext(Dispatchers.IO) { okreplay.intercept(chain) }
                } ?: let {
                    callRequest.createResponse(HttpStatusCode.Conflict) {
                        R["processCall_ConflictingTapes", ""]
                            .also { println(it.red()) }
                    }
                }
            }

            else -> {
                BaseTape.Builder().build().apply {
                    println("Creating new tape: $name".green())
                    createNewInteraction { mock ->
                        mock.requestData = callRequest.toTapeData
                        mock.attractors = RequestAttractors(mock.requestData)
                    }
                    saveFile()
                    tapeCatalog.tapes.add(this)
                }
                callRequest.createResponse(hostTape.status) { hostTape.responseMsg.orEmpty() }
            }
        }
    }
}
