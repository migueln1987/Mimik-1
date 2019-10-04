package tapeItems

import VCRConfig
import mimikMockHelpers.RecordedInteractions
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.stream.JsonWriter
import helpers.attractors.RequestAttractors
import helpers.content
import helpers.isTrue
import helpers.reHost
import helpers.toOkRequest
import helpers.toReplayResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import mimikMockHelpers.InteractionUseStates
import mimikMockHelpers.QueryResponse
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.ResponseBody
import okhttp3.internal.http.HttpMethod
import okreplay.* // ktlint-disable no-wildcard-imports
import java.io.File
import java.io.Writer
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class BlankTape private constructor(config: (BlankTape) -> Unit = {}) : Tape {
    class Builder(config: (Builder) -> Unit = {}) {
        var tapeName: String? = null
        var routingURL: String? = null
        var attractors: RequestAttractors? = null
        /**
         * Allows this tape to accept new recordings
         *
         * Default: true
         */
        var allowLiveRecordings: Boolean? = true

        init {
            config.invoke(this)
        }

        fun build() = BlankTape { tape ->
            tapeName?.also { tape.tapeName = it }
            tape.attractors = attractors
            tape.routingUrl = routingURL
            tape.mode = if (allowLiveRecordings == true)
                TapeMode.READ_WRITE else TapeMode.READ_ONLY
            tape.file = File(
                VCRConfig.getConfig.tapeRoot.get(),
                tape.name.toJsonName
            )
        }

        private val String.toJsonName: String
            get() = replace(" ", "_")
                .replace("""/(\w)""".toRegex()) {
                    it.groups[1]?.value?.toUpperCase() ?: it.value
                }
                .replace("/", "")
                .replace(".", "-")
                .plus(".json")
    }

    companion object {
        @Transient
        var isTestRunning = false

        var tapeRoot: TapeRoot = VCRConfig.getConfig.tapeRoot
        internal val gson = Gson()

        @Transient
        private val defaultResponse = object : Response {
            override fun code() = HttpStatusCode.Gone.value
            override fun protocol() = Protocol.HTTP_1_1

            override fun getEncoding() = ""
            override fun getCharset() = Charset.defaultCharset()

            override fun headers() = okhttp3.Headers.of("", "")
            override fun header(name: String?) = ""
            override fun getContentType() = ""

            override fun hasBody() = false
            override fun body() = byteArrayOf()
            override fun bodyAsText() = ""

            override fun newBuilder() = TODO()
            override fun toYaml() = TODO()
        }
    }

    var tapeName: String? = null
    val usingCustomName
        get() = tapeName.isNullOrBlank().not()

    @Transient
    var file: File? = null

    var attractors: RequestAttractors? = null
    var routingUrl: String? = null

    var chapters: MutableList<RecordedInteractions> = mutableListOf()

    private var tapeMode: TapeMode? = TapeMode.READ_WRITE
        get() = if (field == null) TapeMode.READ_WRITE else field

    val httpRoutingUrl: HttpUrl?
        get() = HttpUrl.parse(routingUrl ?: "")

    /**
     * routingUrl has data, but HttpUrl is unable to parse it
     */
    val isUrlValid: Boolean
        get() = !routingUrl.isNullOrBlank() && httpRoutingUrl != null

    override fun getName() = tapeName ?: hashCode().toString()

    private enum class SearchPreferences {
        ALL, MockOnly, LiveOnly, AwaitOnly
    }

    fun updateNameByURL(url: String) {
        tapeName = String.format(
            "%s_%d",
            url.removePrefix("/"),
            url.hashCode()
        )
    }

    override fun setMatchRule(matchRule: MatchRule?) {}

    override fun getMatchRule(): MatchRule =
        ComposedMatchRule.of(MatchRules.method, MatchRules.queryParams, FilteredBodyRule)

    override fun setMode(mode: TapeMode?) {
        tapeMode = mode
    }

    override fun getMode() = tapeMode ?: TapeMode.READ_WRITE
    override fun isReadable() = mode.isReadable
    override fun isWritable() = mode.isWritable
    override fun isSequential() = mode.isSequential

    override fun size() = chapters.size

    /**
     * Returns a Response from the given request.
     *
     * Note: if [isTestRunning] is true, the response body will contain the request body
     */
    private fun miniResponse(
        request: okhttp3.Request,
        status: HttpStatusCode = HttpStatusCode.OK
    ): okhttp3.Response {
        return okhttp3.Response.Builder().also {
            it.request(request)
            it.protocol(Protocol.HTTP_1_1)
            it.code(status.value)
            it.header(HttpHeaders.ContentType, "text/plain")
            if (HttpMethod.requiresRequestBody(request.method()))
                it.body(
                    ResponseBody.create(
                        MediaType.parse("text/plain"),
                        if (isTestRunning) request.body().content else ""
                    )
                )
            it.message(status.description)
        }.build()
    }

    /**
     * Converts a okHttp request (with context of a tape) into a Interceptor Chain
     */
    fun requestToChain(request: okhttp3.Request): Interceptor.Chain? {
        return object : Interceptor.Chain {
            override fun request(): okhttp3.Request = if (httpRoutingUrl == null)
                request else request.reHost(httpRoutingUrl)

            override fun proceed(request: okhttp3.Request) = getData(request)

            override fun writeTimeoutMillis() = TODO()
            override fun call() = TODO()
            override fun withWriteTimeout(timeout: Int, unit: TimeUnit) = TODO()
            override fun connectTimeoutMillis() = TODO()
            override fun connection() = TODO()
            override fun withConnectTimeout(timeout: Int, unit: TimeUnit) = TODO()
            override fun withReadTimeout(timeout: Int, unit: TimeUnit) = TODO()
            override fun readTimeoutMillis() = TODO()
        }
    }

    fun getData(request: okhttp3.Request): okhttp3.Response {
        return if (isTestRunning)
            miniResponse(request)
        else
            try {
                OkHttpClient().newCall(request).execute()
            } catch (e: Exception) {
                miniResponse(request, HttpStatusCode.ServiceUnavailable)
            }
    }

    override fun seek(request: okreplay.Request) =
        findBestMatch(request).status == HttpStatusCode.Found

    override fun play(request: okreplay.Request): Response {
        val okRequest: okhttp3.Request = request.toOkRequest

        val awaitMock = findBestMatch(okRequest, SearchPreferences.AwaitOnly)
        if (awaitMock.status == HttpStatusCode.Found) {
            awaitMock.item?.also {
                println(
                    "== Await Request ==\n-Url: %s\n-Body:\n%s\n-Headers:\n%s"
                        .format(
                            okRequest.url(),
                            okRequest.body().content,
                            okRequest.headers().toString()
                        )
                )
                val responseData = getData(okRequest)
                println(
                    "== Await Response ==\n- Code %d from %s"
                        .format(responseData.code(), okRequest.url())
                )
                return if (responseData.isSuccessful) {
                    it.response = responseData.toReplayResponse
                    if (it.mockUses > 0) it.mockUses--
                    saveFile()

                    val bodyIsImage = it.responseData.tapeHeaders[HttpHeaders.ContentType]
                        ?.contains("image").isTrue()
                    println(
                        "== Await Response Data ==\n-Body:\n%s\n-Headers:\n%s"
                            .format(
                                if (bodyIsImage) "[image]" else it.responseData.body,
                                it.responseData.headers.toString()
                            )
                    )

                    it.response
                } else {
                    miniResponse(okRequest, HttpStatusCode.BadGateway).toReplayResponse
                }
            }
        }

        val limitedMock = findBestMatch(okRequest, SearchPreferences.MockOnly)
        if (limitedMock.status == HttpStatusCode.Found) {
            limitedMock.item?.also {
                it.mockUses--
                return it.response
            }
        }

        val liveMock = findBestMatch(okRequest, SearchPreferences.LiveOnly)
        if (liveMock.status == HttpStatusCode.Found) {
            liveMock.item?.also {
                return it.response
            }
        }

        return defaultResponse
    }

    private fun findBestMatch(
        request: okhttp3.Request,
        preference: SearchPreferences = SearchPreferences.ALL
    ): QueryResponse<RecordedInteractions> {
        val filteredChapters = chapters.asSequence()
            .filter {
                when (it.mockUses) {
                    InteractionUseStates.DISABLE.state,
                    InteractionUseStates.DISABLEDMOCK.state ->
                        false
                    else -> true
                }
            }
            .filter {
                when (preference) {
                    SearchPreferences.ALL ->
                        true

                    SearchPreferences.AwaitOnly ->
                        it.awaitResponse

                    SearchPreferences.LiveOnly ->
                        it.mockUses == InteractionUseStates.ALWAYS.state

                    SearchPreferences.MockOnly ->
                        it.mockUses in (1..Int.MAX_VALUE)
                }
            }
            .associateBy({ it }, { it.attractors })

        if (filteredChapters.isEmpty()) return QueryResponse {
            status = HttpStatusCode.NotFound
        }

        return RequestAttractors.findBest(
            filteredChapters,
            request.url().encodedPath(),
            request.url().query(),
            if (HttpMethod.requiresRequestBody(request.method()))
                request.body().content else null
        )
    }

    private fun findBestMatch(
        request: okreplay.Request,
        preference: SearchPreferences = SearchPreferences.ALL
    ) = findBestMatch(request.toOkRequest, preference)

    override fun record(request: Request, response: Response) {
        chapters.add(RecordedInteractions(request, response))

        saveFile()
    }

    fun saveFile() {
        val tree = gson.toJsonTree(this).asJsonObject
        val keepChapters = tree.getAsJsonArray("chapters")
            .filter {
                ((it as JsonObject)["mockUses"] as? JsonPrimitive)
                    ?.let { jsonMockUses ->
                        when (jsonMockUses.asInt) {
                            InteractionUseStates.ALWAYS.state,
                            InteractionUseStates.DISABLE.state
                            -> true // export non memory-only chapters
                            else -> false
                        }
                    } ?: false
            }
            .let { jsonList ->
                JsonArray().apply { jsonList.forEach { add(it) } }
            }

        tree.add("chapters", keepChapters)

        file?.also { outFile ->
            val canSaveFile = if (outFile.exists())
                outFile.canWrite()
            else {
                outFile.parentFile.mkdirs()
                outFile.createNewFile()
            }

            if (canSaveFile)
                outFile.bufferedWriter().jWriter
                    .also { gson.toJson(tree, it) }
                    .close()
        }
    }

    /**
     * Creates a JsonWriter from an input Writer
     */
    private val Writer.jWriter: JsonWriter
        get() = JsonWriter(this).apply {
            setIndent(" ")
            isHtmlSafe = false
        }

    override fun isDirty() = false

    /**
     * Creates a new Recorded Interaction and adds it to this tape
     */
    fun createNewInteraction(interaction: (RecordedInteractions) -> Unit = {}) =
        RecordedInteractions {
            interaction.invoke(it) // in case we want to change anything
            chapters.add(it)
        }

    init {
        config.invoke(this)
    }
}
