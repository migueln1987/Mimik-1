package tapeItems

import VCRConfig
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.stream.JsonWriter
import helpers.*
import helpers.attractors.RequestAttractors
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import mimikMockHelpers.MockUseStates
import mimikMockHelpers.QueryResponse
import mimikMockHelpers.RecordedInteractions
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.ResponseBody
import okhttp3.internal.http.HttpMethod
import okreplay.*
import java.io.File
import java.io.Writer
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class BlankTape private constructor(config: (BlankTape) -> Unit = {}) : Tape {
    class Builder(val reBuild: BlankTape? = null, config: (Builder) -> Unit = {}) {
        var tapeName: String? = null
            set(value) {
                field = if (value?.isBlank().isTrue())
                    null else value
                if (reBuild != null)
                    reBuild.tapeName = field
            }

        var routingURL: String? = null
            set(value) {
                field = value
                if (reBuild != null)
                    reBuild.routingUrl = value
            }

        var attractors: RequestAttractors? = null
            set(value) {
                field = value
                if (reBuild != null) {
                    reBuild.attractors
                        ?.also { it.append(value) }
                        ?: also { reBuild.attractors = value }
                }
            }

        /**
         * Allows this tape to accept new recordings
         *
         * Default: true
         */
        var allowLiveRecordings: Boolean? = true
            set(value) {
                field = value
                if (reBuild != null) {
                    reBuild.mode = if (value == true)
                        TapeMode.READ_WRITE else TapeMode.READ_ONLY
                }
            }

        init {
            config.invoke(this)
        }

        /**
         * If [reBuild] is not null, then builder values will be applied to it, else a new tape will be made
         */
        fun build(): BlankTape {
            val returnTape = if (reBuild != null) {
                if (reBuild.file?.exists().isTrue())
                    reBuild.file?.delete()
                reBuild.file = File(
                    VCRConfig.getConfig.tapeRoot.get(),
                    reBuild.name.toJsonName
                )
                reBuild
            } else {
                BlankTape { tape ->
                    tape.tapeName = tapeName
                    tape.attractors = attractors
                    tape.mode = if (allowLiveRecordings == true)
                        TapeMode.READ_WRITE else TapeMode.READ_ONLY
                    tape.file = File(
                        VCRConfig.getConfig.tapeRoot.get(),
                        tape.name.toJsonName
                    )
                }
            }

            if (!routingURL.isNullOrBlank())
                returnTape.routingUrl = routingURL?.ensurePrefix("http", "http://")

            return returnTape
        }
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

        /**
         * Will attempt to create a new builder from a non-null [reBuild], else a new tape will be made
         */
        fun reBuild(reBuild: BlankTape? = null, config: (Builder) -> Unit = {}) =
            Builder(reBuild, config).build()
    }

    var tapeName: String? = null
    val hasNameSet
        get() = tapeName.isNullOrBlank().not()

    @Transient
    var file: File? = null

    var attractors: RequestAttractors? = null
    var routingUrl: String? = null

    var alwaysLive: Boolean? = false

    var chapters: MutableList<RecordedInteractions> = mutableListOf()

    private var tapeMode: TapeMode? = TapeMode.READ_WRITE
        get() = if (field == null) TapeMode.READ_WRITE else field

    val httpRoutingUrl: HttpUrl?
        get() = HttpUrl.parse(routingUrl.orEmpty())

    /**
     * routingUrl has data, but HttpUrl is unable to parse it
     */
    val isUrlValid: Boolean
        get() = !routingUrl.isNullOrBlank() && httpRoutingUrl != null

    override fun getName() = tapeName ?: file?.nameWithoutExtension ?: hashCode().toString()

    private enum class SearchPreferences {
        /**
         * Interactions which will ALWAYS use a live response
         */
        AlwaysLive,
        /**
         * Mocks waiting for a response body
         */
        AwaitOnly,
        /**
         * Mocks which have a limited number of uses before expiration
         */
        LimitedOnly,
        /**
         * Normal mock interactions
         */
        MockOnly,
        ALL
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
                        if (isTestRunning) request.body().content() else ""
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

        val alwaysLive = findBestMatch(okRequest, SearchPreferences.AlwaysLive)
        if (alwaysLive.status == HttpStatusCode.Found) {
            alwaysLive.item?.also {
                println(
                    "== Live Request ==\n" +
                            "-Name\n %s\n-Can Complete: %b\n" +
                            "-Url: %s\n-Body:\n %s\n-Headers:\n %s%s\n",
                    it.name,
                    isUrlValid,
                    okRequest.url(),
                    okRequest.body().content("{null}").valueOrIsEmpty,
                    okRequest.headers().toString().valueOrIsEmpty,
                    if (it.mockUses >= 0)
                        "-Uses: ${it.mockUses}" else ""
                )

                if (isUrlValid) {
                    val responseData = getData(okRequest)
                    println(
                        "== Live Response ==\n-Code %d from %s\n",
                        responseData.code(), okRequest.url()
                    )
                    if (responseData.isSuccessful) {
                        if (it.mockUses > 0) it.mockUses--
                        println(
                            "== Live Response Data ==\n-Body:\n %s\n-Headers:\n %s%s\n",
                            if (it.responseData.isImage)
                                "[image]" else it.responseData?.body,
                            it.responseData?.headers.toString(),
                            if (it.mockUses >= 0)
                                "\n-Remaining Uses: ${it.mockUses}" else ""
                        )
                        return responseData.toReplayResponse
                    }
                }
            }

            return miniResponse(okRequest, HttpStatusCode.BadGateway).toReplayResponse
        }

        val awaitMock = findBestMatch(okRequest, SearchPreferences.AwaitOnly)
        if (awaitMock.status == HttpStatusCode.Found) {
            awaitMock.item?.also {
                println(
                    "== Await Request ==\n-Name\n %s\n-Url: %s\n-Body:\n %s\n-Headers:\n %s%s",
                    it.name,
                    okRequest.url(),
                    okRequest.body().content("{null}").valueOrIsEmpty,
                    okRequest.headers().toString().valueOrIsEmpty,
                    if (it.mockUses >= 0)
                        "\n-Uses: ${it.mockUses}" else ""
                )
                val responseData = getData(okRequest)
                println(
                    "== Await Response ==\n-Code (%d) from %s\n",
                    responseData.code(), okRequest.url()
                )
                return if (responseData.isSuccessful) {
                    it.response = responseData.toReplayResponse
                    if (it.mockUses > 0) it.mockUses--
                    saveFile()

                    println(
                        "== Await Response Data ==\n-Body:\n %s\n-Headers:\n %s%s",
                        if (it.responseData.isImage)
                            "[image]" else it.responseData?.body,
                        it.responseData?.headers.toString(),
                        if (it.mockUses >= 0)
                            "\n-Remaining Uses: ${it.mockUses}" else ""
                    )

                    it.response
                        ?: miniResponse(okRequest, HttpStatusCode.NoContent).toReplayResponse
                } else {
                    miniResponse(okRequest, HttpStatusCode.BadGateway).toReplayResponse
                }
            }
        }

        val limitedMock = findBestMatch(okRequest, SearchPreferences.LimitedOnly)
        if (limitedMock.status == HttpStatusCode.Found) {
            limitedMock.item?.also {
                it.mockUses--
                println(
                    "== Limited Mock ==\n-Name\n %s\n-Remaining Uses\n %s\n",
                    it.name, it.mockUses
                )
                return it.response
                    ?: miniResponse(okRequest, HttpStatusCode.NoContent).toReplayResponse
            }
        }

        val activeMock = findBestMatch(okRequest, SearchPreferences.MockOnly)
        if (activeMock.status == HttpStatusCode.Found) {
            activeMock.item?.also {
                println(
                    "== Mock ==\n-Name\n %s\n",
                    it.name
                )
                return it.response
                    ?: miniResponse(okRequest, HttpStatusCode.NoContent).toReplayResponse
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
                    MockUseStates.DISABLE.state,
                    MockUseStates.DISABLEDLIMITED.state ->
                        false
                    else -> true
                }
            }
            .filter {
                when (preference) {
                    SearchPreferences.ALL ->
                        true

                    SearchPreferences.AlwaysLive ->
                        it.alwaysLive.isTrue()

                    SearchPreferences.AwaitOnly ->
                        it.awaitResponse

                    SearchPreferences.MockOnly ->
                        it.mockUses == MockUseStates.ALWAYS.state

                    SearchPreferences.LimitedOnly ->
                        it.mockUses in (1..Int.MAX_VALUE)
                }
            }
            .associateWith { it.attractors }

        if (filteredChapters.isEmpty()) return QueryResponse {
            status = HttpStatusCode.NotFound
        }

        return RequestAttractors.findBest(
            filteredChapters,
            request.url().encodedPath(),
            request.url().query(),
            if (HttpMethod.requiresRequestBody(request.method()))
                request.body().content() else null
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

    /**
     * Saves the data to file. A new file will be created if one doesn't exist
     */
    fun saveFile() {
        val tree = gson.toJsonTree(this).asJsonObject
        val keepChapters = tree.getAsJsonArray("chapters")
            .filter {
                ((it as JsonObject)["mockUses"] as? JsonPrimitive)
                    ?.let { jsonMockUses ->
                        when (jsonMockUses.asInt) {
                            MockUseStates.ALWAYS.state,
                            MockUseStates.DISABLE.state
                            -> true // export non memory-only chapters
                            else -> false
                        }
                    } ?: false
            }
            .let { jsonList ->
                JsonArray().apply { jsonList.forEach { add(it) } }
            }

        tree.add("chapters", keepChapters)
        if (file?.exists().isTrue() && (file?.nameWithoutExtension != tapeName))
            file?.delete()

        if (file == null)
            file = File(
                VCRConfig.getConfig.tapeRoot.get(),
                name.toJsonName
            )

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
