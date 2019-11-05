import com.google.gson.Gson
import helpers.*
import helpers.attractors.RequestAttractors
import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mimikMockHelpers.MockUseStates
import mimikMockHelpers.QueryResponse
import okreplay.OkReplayInterceptor
import tapeItems.BlankTape
import kotlin.io.println

class TapeCatalog : OkReplayInterceptor() {
    private val config = VCRConfig.getConfig

    val tapes: MutableList<BlankTape> = mutableListOf()

    companion object {
        val Instance by lazy {
            TapeCatalog().also { it.loadTapeData() }
        }
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
    fun findResponseByQuery(request: okhttp3.Request): QueryResponse<BlankTape> {
        if (tapes.isEmpty()) return QueryResponse()

        val path = request.url().encodedPath().removePrefix("/")
        val params = request.url().query()
        val headers = request.headers().toStringPairs()
        val body = request.body()?.content()

        val validChapters = tapes.asSequence()
            .flatMap { it.chapters.asSequence() }
            .filter { MockUseStates.isEnabled(it.mockUses) }
            .associateWith { it.attractors }

        val foundChapter = RequestAttractors.findBest(
            validChapters,
            path, params, headers, body
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
        val params = request.url().query()
        val headers = request.headers().toStringPairs()

        val validTapes = tapes
            .filter { it.mode.isWritable }
            .associateBy({ it }, { it.attractors })

        return RequestAttractors.findBest(
            validTapes,
            path, params, headers
        )
    }

    suspend fun processCall(call: ApplicationCall): okhttp3.Response {
        var callRequest = call.toOkRequest()

        findResponseByQuery(callRequest).item?.also {
            println("Using response tape ${it.name}")
            it.requestToChain(callRequest)?.also { chain ->
                start(config, it)
                return withContext(Dispatchers.IO) { intercept(chain) }
            }

            return callRequest.makeCatchResponse(HttpStatusCode.PreconditionFailed) {
                R.getProperty("processCall_InvalidUrl")
            }
        }

        val hostTape = findTapeByQuery(callRequest)
        return when (hostTape.status) {
            HttpStatusCode.Found -> {
                hostTape.item?.let {
                    println("Using tape ${it.name}")
                    if (it.isValidURL)
                        callRequest = callRequest.reHost(it.httpRoutingUrl)

                    it.createNewInteraction { mock ->
                        mock.requestData = callRequest.toTapeData
                        mock.attractors = RequestAttractors(mock.requestData)
                        mock.alwaysLive = it.alwaysLive
                    }
                    it.saveFile()

                    it.requestToChain(callRequest)?.let { chain ->
                        start(config, it)
                        withContext(Dispatchers.IO) { intercept(chain) }
                    } ?: callRequest.makeCatchResponse(HttpStatusCode.PreconditionFailed) {
                        R.getProperty("processCall_InvalidUrl")
                    }
                } ?: let {
                    callRequest.makeCatchResponse(HttpStatusCode.Conflict) {
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
                callRequest.makeCatchResponse(hostTape.status) { hostTape.responseMsg.orEmpty() }
            }
        }
    }
}
