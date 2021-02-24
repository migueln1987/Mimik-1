package mimik.tapeItems

import R
import VCRConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import javaUtils.io.jsonFiles
import kolor.*
import kotlinUtils.collections.firstMatchNotNull
import kotlinUtils.isTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import mimik.helpers.*
import mimik.helpers.attractors.RequestAttractors
import mimik.mockHelpers.*
import mimik.networkRouting.testingManager.*
import okhttp3.content
import okhttp3.createResponse
import okhttp3.toStringPairs
import okreplay.OkReplayInterceptor
import java.io.File
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*

class TapeCatalog {
    private val okreplay by lazy { OkReplayInterceptor() }
    val config by lazy { VCRConfig.getConfig }
    val tapes: MutableList<BaseTape> = mutableListOf()
    val requestList = mutableMapOf<String, Semaphore>()
    val processingRequests: MutableMap<String, Semaphore> by lazy { Collections.synchronizedMap(requestList) }

    /**
     * List of tapes to load (from JSON files)
     */
    val tapeFiles: List<File>?
        get() = config.tapeRoot.get()?.jsonFiles()

    val responseLinker by lazy { ResponseLinkerUtil() }

    companion object {
        var isTestRunning = false
        val gson: Gson by lazy {
            GsonBuilder().apply {
                registerTypeAdapterFactory(SeqActionObject.typeFactory)
            }.create()
        }

        val Instance by lazy { TapeCatalog().also { it.loadTapeData() } }
    }

    /**
     * Loads all the *.json tapes within the okreplay.tapeRoot directory
     */
    fun loadTapeData() {
        val files = tapeFiles ?: return

        tapes.clear()
        files.asSequence()
            .map { it to it.readText() }
            .mapNotNull {
                try {
                    gson.fromJson(it.second, BaseTape::class.java)
                        ?.also { tape ->
                            tape.file = it.first
                        }
                } catch (e: Exception) {
                    println(e.toString())
                    null
                }
            }
            .forEach { tapes.add(it) }

        // repair duplicate UIDs
        val chapIDs = tapes.flatMap { tape ->
            tape.chapters.map { Triple(tape, it, it.UID.orEmpty()) }
        }

        val sameIDs = chapIDs.groupingBy { it.third }.eachCount().filter { it.value > 1 }
        if (sameIDs.isNotEmpty()) {
            val resaveTapes = mutableListOf<BaseTape>()
            sameIDs.forEach { (sID, _) ->
                chapIDs.filter { it.third == sID }
                    .forEach {
                        resaveTapes.add(it.first)
                        it.second.UID = null
                        it.second.UID
                    }
            }

            resaveTapes.forEach { it.saveFile() }
        }
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
    ): Pair<QueryResponse<BaseTape>, RecordedInteractions?> {
        if (tapes.isEmpty()) return Pair(QueryResponse(), null)

        val allowedTapes = tapes.asSequence()
            .filter { tapeLimit?.contains(it.name) ?: true }.toList()

        fun allowedChaps() = allowedTapes.asSequence()
            .flatMap { it.chapters.asSequence() }
            .filter { MockUseStates.isEnabled(it.mockUses) }

        val hashContent = request.contentHash

        val cachedChapter = allowedChaps()
            .firstOrNull { it.cachedCalls.contains(hashContent) }

        val bestChapter = when {
            cachedChapter != null -> cachedChapter

            else -> {
                val readyChapters = allowedChaps()
                    .filter { it.attractors != null }
                    .associateWith { it.attractors!! }

                val path = request.url.encodedPath.removePrefix("/")
                val queries = request.url.query
                val headers = request.headers.toStringPairs()
                val body = request.body?.content()

                val validChapters = RequestAttractors.findBest_many(
                    readyChapters,
                    path, queries, headers, body
                )

                fun useMatchesTest(chap: RecordedInteractions, test: (Int) -> Boolean) =
                    allowedTapes.first { it.chapters.contains(chap) }
                        .run { test(chap.uses) }

                val items = validChapters.item
                when {
                    items == null -> null
                    items.size == 1 -> items.first()
                    else -> items.firstMatchNotNull(
                        { it.alwaysLive.isTrue },
                        { it.awaitResponse },
                        { useMatchesTest(it) { uses -> uses == MockUseStates.ALWAYS.state } },
                        { useMatchesTest(it) { uses -> uses in (1..Int.MAX_VALUE) } }
                    )
                }
            }
        }

        val foundTape = allowedTapes
            .firstOrNull { it.chapters.contains(bestChapter) }

        if (cachedChapter == null && bestChapter != null)
            bestChapter.cachedCalls.add(hashContent)

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
    fun findTapeByQuery(request: okhttp3.Request): QueryResponse<BaseTape> {
        val path = request.url.encodedPath.removePrefix("/")
        val queries = request.url.query
        val headers = request.headers.toStringPairs()

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
        val callUrl = callRequest.url.toString()

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

        val (resp, chap) = findResponseByQuery(callRequest, bounds?.tapes)
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
                R.getProperty("processCall_InvalidUrl")
                    .also { println(it.red()) }
            }
        }

        if (bounds != null) return callRequest.createResponse(HttpStatusCode.Forbidden) {
            "Test bounds [${bounds.handle}] has no matching recordings for ${callRequest.url}."
                .also { println(it.red()) }
        }

        val hostTape = findTapeByQuery(callRequest)
        return when (hostTape.status) {
            HttpStatusCode.Found -> {
                hostTape.item?.let {
                    println("Response not found; Using tape ${it.name}".cyan())

                    val chain = it.requestToChain(callRequest)
                    okreplay.start(config, it)
                    withContext(Dispatchers.IO) { okreplay.intercept(chain) }
                } ?: let {
                    callRequest.createResponse(HttpStatusCode.Conflict) {
                        R.getProperty("processCall_ConflictingTapes")
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
                    tapes.add(this)
                }
                callRequest.createResponse(hostTape.status) { hostTape.responseMsg.orEmpty() }
            }
        }
    }
}
