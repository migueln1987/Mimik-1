import com.google.gson.Gson
import helpers.*
import helpers.attractors.RequestAttractors
import io.ktor.application.ApplicationCall
import io.ktor.features.callId
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mimikMockHelpers.MockUseStates
import mimikMockHelpers.QueryResponse
import networkRouting.TestingManager.observe
import networkRouting.TestingManager.TestManager
import okreplay.OkReplayInterceptor
import tapeItems.BlankTape
import java.util.Collections
import java.util.Date
import kotlin.io.println

class TapeCatalog : OkReplayInterceptor() {
    private val config = VCRConfig.getConfig

    val tapes: MutableList<BlankTape> = mutableListOf()
    val backingList = mutableListOf<String>()
    var processingRequests = Collections.synchronizedList(backingList)

    companion object {
        var isTestRunning = false

        val Instance by lazy { TapeCatalog().also { it.loadTapeData() } }
    }

    init {
        BlankTape.tapeRoot = config.tapeRoot
    }

    /**
     * Loads all the *.json tapes within the okreplay.tapeRoot directory
     */
    fun loadTapeData() {
        val root = config.tapeRoot.get() ?: return
        val gson = Gson()

        tapes.clear()
        root.fileListing().asSequence()
            .map { it to it.readText() }
            .mapNotNull {
                try {
                    gson.fromJson(it.second, BlankTape::class.java)
                        ?.also { tape ->
                            tape.file = it.first
                        }
                } catch (e: Exception) {
                    println(e.toString())
                    null
                }
            }
            .forEach { tapes.add(it) }
    }

    /**
     * Finds the tape which contains this request (by query match)
     *
     * @return
     * - HttpStatusCode.Found (302) = item
     * - HttpStatusCode.NotFound (404) = item
     * - HttpStatusCode.Conflict (409) = null item
     */
    fun findResponseByQuery(request: okhttp3.Request, tapeLimit: List<String>? = null): QueryResponse<BlankTape> {
        if (tapes.isEmpty()) return QueryResponse()

        val path = request.url().encodedPath().removePrefix("/")
        val queries = request.url().query()
        val headers = request.headers().toStringPairs()
        val body = request.body()?.content()

        val validChapters = tapes.asSequence()
            .filter { tapeLimit?.contains(it.name) ?: true }
            .flatMap { it.chapters.asSequence() }
            .filter { MockUseStates.isEnabled(it.mockUses) }
            .associateWith { it.attractors }

        val foundChapter = RequestAttractors.findBest(
            validChapters,
            path, queries, headers, body
        )

        val foundTape = tapes.firstOrNull {
            it.chapters.contains(foundChapter.item)
        }

        return QueryResponse {
            item = foundTape
            status = foundTape?.let { HttpStatusCode.Found } ?: HttpStatusCode.NotFound
        }
    }

    /**
     * Returns the most likely tape which can accept the [request]
     *
     * @return
     * - HttpStatusCode.Found (302) = item
     * - HttpStatusCode.NotFound (404) = item
     * - HttpStatusCode.Conflict (409) = null item
     */
    fun findTapeByQuery(request: okhttp3.Request): QueryResponse<BlankTape> {
        val path = request.url().encodedPath().removePrefix("/")
        val queries = request.url().query()
        val headers = request.headers().toStringPairs()

        val validTapes = tapes.asSequence()
            .filter { it.mode.isWritable }
            .associateBy({ it }, { it.attractors })

        return RequestAttractors.findBest(
            validTapes,
            path, queries, headers
        )
    }

    suspend fun processCall(call: ApplicationCall): okhttp3.Response {
        val callRequest: okhttp3.Request = call.toOkRequest()
        val callUrl = callRequest.url().toString()

        println("Requests: ${processingRequests.size}")
        while (processingRequests.contains(callUrl)) {
            println("... waiting for request $callUrl to finish")
            delay(20)
        }

        processingRequests.add(callUrl)
        println("Active Requests: ${processingRequests.size}")

        val bounds = TestManager.getManagerByID(call.callId)

        try {
            if (bounds != null) {
                val response = when {
                    bounds.expireTime?.after(Date()).isTrue() ->
                        callRequest.createResponse(HttpStatusCode.Forbidden) {
                            "Testing bounds expired"
                        }
                    !bounds.isEnabled ->
                        callRequest.createResponse(HttpStatusCode.Forbidden) {
                            "Test with handle ${bounds.handle} is stopped."
                        }
                    bounds.tapes.isEmpty() ->
                        callRequest.createResponse(HttpStatusCode.Forbidden) {
                            "Test with handle ${bounds.handle} has no tapes."
                        }
                    else -> null
                }
                if (response != null) return response
            }

            findResponseByQuery(callRequest, bounds?.tapes).item?.also { tape ->
                println("Using response tape ${tape.name}")
                tape.requestToChain(callRequest)?.also ChainAlso@{ chain ->
                    start(config, tape)
                    withContext(Dispatchers.IO) {
                        bounds.observe(tape) {
                            intercept(chain)
                        }
                    }?.also { return it }
                }

                return callRequest.createResponse(HttpStatusCode.PreconditionFailed) {
                    R.getProperty("processCall_InvalidUrl")
                }
            }

            if (bounds != null) return callRequest.createResponse(HttpStatusCode.Forbidden) {
                "Test bounds [${bounds.handle}] has no matching recordings."
            }

            val hostTape = findTapeByQuery(callRequest)
            return when (hostTape.status) {
                HttpStatusCode.Found -> {
                    hostTape.item?.let {
                        println("Using tape ${it.name}")

                        it.requestToChain(callRequest)?.let { chain ->
                            start(config, it)
                            withContext(Dispatchers.IO) { intercept(chain) }
                        } ?: callRequest.createResponse(HttpStatusCode.PreconditionFailed) {
                            R.getProperty("processCall_InvalidUrl")
                        }
                    } ?: let {
                        callRequest.createResponse(HttpStatusCode.Conflict) {
                            R.getProperty("processCall_ConflictingTapes")
                        }
                    }
                }

                else -> {
                    BlankTape.Builder().build().also { tape ->
                        println("Creating new tape/mock of ${tape.name}")
                        tape.createNewInteraction { mock ->
                            mock.requestData = callRequest.toTapeData
                            mock.attractors = RequestAttractors(mock.requestData)
                        }
                        tape.saveFile()
                        tapes.add(tape)
                    }
                    callRequest.createResponse(hostTape.status) { hostTape.responseMsg.orEmpty() }
                }
            }
        } finally {
            println("Releasing lock: ${callRequest.url()}")
            processingRequests.remove(callUrl)
        }
    }
}
