import com.google.gson.Gson
import helpers.*
import helpers.attractors.RequestAttractors
import io.ktor.application.ApplicationCall
import io.ktor.features.callId
import io.ktor.http.HttpStatusCode
import kolor.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mimikMockHelpers.MockUseStates
import mimikMockHelpers.QueryResponse
import mimikMockHelpers.RecordedInteractions
import networkRouting.testingManager.observe
import networkRouting.testingManager.TestManager
import networkRouting.testingManager.replaceByTest
import okreplay.OkReplayInterceptor
import tapeItems.BlankTape
import java.io.File
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.Collections
import java.util.Date
import kotlin.io.println

class TapeCatalog : OkReplayInterceptor() {
    private val config = VCRConfig.getConfig

    val tapes: MutableList<BlankTape> = mutableListOf()
    val backingList = mutableListOf<String>()
    var processingRequests = Collections.synchronizedList(backingList)

    val tapeFiles: List<File>?
        get() = config.tapeRoot.get()?.jsonFiles()

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
        val files = tapeFiles ?: return
        val gson = Gson()

        tapes.clear()
        files.asSequence()
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
    fun findResponseByQuery(
        request: okhttp3.Request,
        tapeLimit: List<String>? = null
    ): Pair<QueryResponse<BlankTape>, RecordedInteractions?> {
        if (tapes.isEmpty()) return Pair(QueryResponse(), null)

        val path = request.url().encodedPath().removePrefix("/")
        val queries = request.url().query()
        val headers = request.headers().toStringPairs()
        val body = request.body()?.content()

        val allowedTapes = tapes.asSequence()
            .filter { tapeLimit?.contains(it.name) ?: true }.toList()

        val readyChapters = allowedTapes.asSequence()
            .flatMap { it.chapters.asSequence() }
            .filter { MockUseStates.isEnabled(it.mockUses) }
            .filter { it.attractors != null }
            .associateWith { it.attractors!! }

        val validChapters = RequestAttractors.findBest_many(
            readyChapters,
            path, queries, headers, body
        )

        fun useMatchesTest(chap: RecordedInteractions, test: (Int) -> Boolean) =
            allowedTapes.first { it.chapters.contains(chap) }
                .run { test.invoke(chap.uses) }

        val items = validChapters.item
        val bestChapter = when {
            items == null -> null
            items.size == 1 -> items.first()
            else -> items.firstMatchNotNull(
                { it.alwaysLive.isTrue() },
                { it.awaitResponse },
                { useMatchesTest(it) { uses -> uses == MockUseStates.ALWAYS.state } },
                { useMatchesTest(it) { uses -> uses in (1..Int.MAX_VALUE) } }
            )
        }

        val foundTape = allowedTapes
            .firstOrNull { it.chapters.contains(bestChapter) }

        return Pair(QueryResponse {
            item = foundTape
            status = foundTape?.let { HttpStatusCode.Found } ?: HttpStatusCode.NotFound
        }, bestChapter)
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
            .filter { it.attractors != null }
            .associateBy({ it }, { it.attractors!! })

        return RequestAttractors.findBest(
            validTapes,
            path, queries, headers
        )
    }

    suspend fun processCall(call: ApplicationCall): okhttp3.Response {
        val callRequest: okhttp3.Request = call.toOkRequest()
        val callUrl = callRequest.url().toString()

        println("Requests: ${processingRequests.size}".cyan())
        while (processingRequests.contains(callUrl)) {
            println("... waiting for request $callUrl to finish".blue())
            delay(20)
        }

        println("Adding url lock for $callUrl".blue())
        processingRequests.add(callUrl)
        println("Active Requests: ${processingRequests.size}".cyan())

        val bounds = TestManager.getManagerByID(call.callId)
        val startTime = System.currentTimeMillis()

        try {
            if (bounds != null) {
                val response = when {
                    Date().after(bounds.expireTime) -> {
                        val timeOver = ChronoUnit.SECONDS.between(
                            Date().toInstant(),
                            (bounds.expireTime ?: Date()).toInstant()
                        )
                        "Testing bounds for (%s) is expired. %s past".format(
                            bounds.handle,
                            Duration.ofSeconds(timeOver).toString().removePrefix("PT")
                        )
                    }
                    !bounds.isEnabled ->
                        "Test with handle ${bounds.handle} is stopped."
                    bounds.tapes.isEmpty() ->
                        "Test with handle ${bounds.handle} has no tapes."
                    else -> {
                        printlnF(
                            "Using test bounds (%s) towards device (%s)".green(),
                            bounds.handle,
                            bounds.boundSource
                        )
                        null
                    }
                }
                if (response != null) {
                    println(response.red())
                    return callRequest.createResponse(HttpStatusCode.Forbidden) { response }
                }
            }

            val (resp, chap) = findResponseByQuery(callRequest, bounds?.tapes)
            resp.item?.also { tape ->
                println("Using response tape ${tape.name}".green())
                val chain = tape.requestToChain(callRequest)
                start(config, tape)
                withContext(Dispatchers.IO) {
                    bounds.observe(tape) {
                        intercept(chain).replaceByTest(bounds, chap)
                    }
                }?.also { return it }

                return callRequest.createResponse(HttpStatusCode.PreconditionFailed) {
                    R.getProperty("processCall_InvalidUrl")
                        .also { println(it.red()) }
                }
            }

            if (bounds != null) return callRequest.createResponse(HttpStatusCode.Forbidden) {
                "Test bounds [${bounds.handle}] has no matching recordings for $callUrl."
                    .also { println(it.red()) }
            }

            val hostTape = findTapeByQuery(callRequest)
            return when (hostTape.status) {
                HttpStatusCode.Found -> {
                    hostTape.item?.let {
                        println("Response not found; Using tape ${it.name}".cyan())

                        val chain = it.requestToChain(callRequest)
                        start(config, it)
                        withContext(Dispatchers.IO) { intercept(chain) }
                    } ?: let {
                        callRequest.createResponse(HttpStatusCode.Conflict) {
                            R.getProperty("processCall_ConflictingTapes")
                                .also { println(it.red()) }
                        }
                    }
                }

                else -> {
                    BlankTape.Builder().build().apply {
                        println("Creating new tape/mock of $name".green())
                        createNewInteraction { mock ->
                            mock.requestData = callRequest.toTapeData
                            mock.attractors = RequestAttractors(mock.requestData)
                        }
                        saveFile()
                        tapes.add(this)
                    }
                    callRequest.createResponse(hostTape.status) { hostTape.responseMsg.orEmpty() }
                }
            }
        } finally {
            val eTime = System.currentTimeMillis() - startTime
            printlnF(
                "Releasing lock (%d ms): %s".blue(),
                System.currentTimeMillis() - startTime,
                callRequest.url()
            )
            processingRequests.remove(callUrl)
        }
    }
}
