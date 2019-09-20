import helpers.fileListing
import tapeItems.BlankTape
import com.google.gson.Gson
import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mimikMockHelpers.RecordedInteractions
import mimikMockHelpers.QueryResponse
import okhttp3.Protocol
import okreplay.OkReplayInterceptor
import okreplay.TapeMode
import helpers.attractors.RequestAttractors
import tapeItems.helpers.content
import tapeItems.helpers.toOkRequest

class TapeCatalog private constructor() : OkReplayInterceptor() {
    private val config = VCRConfig.getConfig

    val tapes: MutableList<BlankTape> = mutableListOf()

    companion object {
        var Instance = TapeCatalog()

        private val defaultBlankTape = BlankTape
            .Builder() { tapeName = "Default Tape" }
            .build()
    }

    init {
        BlankTape.tapeRoot = config.tapeRoot
        loadTapeData()
    }

    /**
     * Loads all the *.json tapes within the okreplay.tapeRoot directory
     */
    private fun loadTapeData() {
        val root = config.tapeRoot.get() ?: return
        val gson = Gson()

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
     */
    fun findResponseByQuery(request: okhttp3.Request): QueryResponse<BlankTape> {
        if (tapes.isEmpty()) return QueryResponse()

        val path = request.url().encodedPath()
        val params = request.url().query()
        val body = request.body()?.content ?: ""

        val validChapters = tapes.asSequence()
            .flatMap { it.chapters.asSequence() }
            .filter {
                when (it.mockUses) {
                    RecordedInteractions.UseStates.ALWAYS.state,
                    in (1..Int.MAX_VALUE) -> true
                    else -> false
                }
            }
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

        return QueryResponse { item = foundTape }
    }

    /**
     * Returns the most likely tape which can accept the [request]
     *
     * @return
     * - tape
     * - HttpStatusCode.Conflict
     * - no tape (http.OK)
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
            it.request(toOkRequest(""))
            it.protocol(Protocol.HTTP_1_1)
            it.code(status.value)
            it.message(message.invoke())
        }.build()
    }
}
