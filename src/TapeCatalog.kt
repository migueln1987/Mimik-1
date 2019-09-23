import helpers.fileListing
import tapeItems.BlankTape
import com.google.gson.Gson
import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mimikMockHelpers.QueryResponse
import okhttp3.Protocol
import okreplay.OkReplayInterceptor
import okreplay.TapeMode
import helpers.attractors.RequestAttractors
import helpers.content
import helpers.toOkRequest
import mimikMockHelpers.InteractionUseStates

class TapeCatalog : OkReplayInterceptor() {
    private val config = VCRConfig.getConfig

    val tapes: MutableList<BlankTape> = mutableListOf()

    companion object {
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
                    @Suppress("USELESS_ELVIS")
                    gson.fromJson(it.second, BlankTape::class.java)
                        ?.also { tape ->
                            tape.file = it.first
                            tape.mode = TapeMode.READ_WRITE
                            tape.chapters = tape.chapters ?: mutableListOf()
                            tape.tapeName = tape.tapeName ?: tape.hashCode().toString()
                            tape.chapters.forEach { chapter ->
                                chapter.updateReplayData()
                            }
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
     * - HttpStatusCode.Found
     * - HttpStatusCode.NotFound
     * - HttpStatusCode.NoContent
     */
    fun findResponseByQuery(request: okhttp3.Request): QueryResponse<BlankTape> {
        if (tapes.isEmpty()) return QueryResponse()

        val path = request.url().encodedPath()
        val params = request.url().query()
        val body = request.body()?.content ?: ""

        val validChapters = tapes.asSequence()
            .flatMap { it.chapters.asSequence() }
            .filter { InteractionUseStates.asState(it.mockUses).isEnabled }
            .associateBy({ it }, { it.attractors })

        val foundChapter = RequestAttractors.findBest(
            validChapters,
            path, params, body
        ) {
            val headersQuery = it.matchingHeaders(request)

            var first = 0
            if (headersQuery > 0) first += headersQuery
            (first to -1)
        }

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
     * - HttpStatusCode.Conflict (no tape)
     * - HttpStatusCode.OK (valid tape)
     */
    fun findTapeByQuery(request: okhttp3.Request): QueryResponse<BlankTape> {
        val path = request.url().encodedPath()
        val params = request.url().query()

        val validTapes = tapes
            .filter { it.isUrlValid && it.mode.isWritable }
            .associateBy({ it }, { it.attractors })

        return RequestAttractors.findBest(
            validTapes,
            path, params
        )
    }

    suspend fun processCall(call: ApplicationCall): okhttp3.Response {
        val callRequest = call.toOkRequest()

        findResponseByQuery(callRequest).item?.also {
            it.requestToChain(callRequest)?.also { chain ->
                start(config, it)
                return withContext(Dispatchers.IO) {
                    intercept(chain)
                }
            }

            return call.makeCatchResponse(HttpStatusCode.PreconditionFailed) {
                R.getProperty("processCall_InvalidUrl")
            }
        }

        val hostTape = findTapeByQuery(callRequest)
        return when (hostTape.status) {
            HttpStatusCode.OK -> {
                hostTape.item?.let {
                    it.requestToChain(callRequest)?.let { chain ->
                        start(config, it)
                        withContext(Dispatchers.IO) { intercept(chain) }
                    }
                        ?: call.makeCatchResponse(HttpStatusCode.PreconditionFailed) {
                            R.getProperty("processCall_InvalidUrl")
                        }
                }
                    ?: let {
                        call.makeCatchResponse(HttpStatusCode.Conflict) {
                            R.getProperty("processCall_ConflictingTapes")
                        }
                    }
            }

            else -> call.makeCatchResponse(hostTape.status) { hostTape.responseMsg ?: "" }
        }
    }

    /**
     * Returns a brief okHttp response to respond with a defined response [status] and [message]
     */
    suspend fun ApplicationCall.makeCatchResponse(
        status: HttpStatusCode,
        message: () -> String = { "" }
    ): okhttp3.Response {
        return okhttp3.Response.Builder().also {
            it.request(toOkRequest("local.host"))
            it.protocol(Protocol.HTTP_1_1)
            it.code(status.value)
            it.message(message.invoke())
        }.build()
    }
}
