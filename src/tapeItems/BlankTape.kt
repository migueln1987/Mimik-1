package tapeItems

import VCRConfig
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.stream.JsonWriter
import helpers.*
import helpers.attractors.RequestAttractors
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.*
import mimikMockHelpers.MockUseStates
import mimikMockHelpers.QueryResponse
import mimikMockHelpers.RecordedInteractions
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.internal.http.HttpMethod
import okreplay.*
import java.io.File
import java.io.Writer
import java.nio.charset.Charset
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

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

        val isValidURL: Boolean
            get() = routingURL.isValidJSON

        var attractors: RequestAttractors? = null
            set(value) {
                field = value
                if (reBuild != null) {
                    reBuild.attractors
                        ?.also { it.append(value) }
                        ?: also { reBuild.attractors = value }
                    if (value == null)
                        reBuild.attractors = null
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

        var alwaysLive: Boolean? = false
            set(value) {
                field = value
                if (reBuild != null) {
                    reBuild.alwaysLive = value
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
                var isHardTape = false
                if (reBuild.file?.exists().isTrue()) {
                    isHardTape = true
                    reBuild.file?.delete()
                }
                reBuild.file = null
                if (isHardTape)
                    reBuild.saveFile()
                reBuild
            } else {
                BlankTape { tape ->
                    tape.tapeName = tapeName
                    tape.attractors = attractors
                    tape.mode = if (allowLiveRecordings == true)
                        TapeMode.READ_WRITE else TapeMode.READ_ONLY
                    tape.alwaysLive = alwaysLive
                    tape.file = File(
                        VCRConfig.getConfig.tapeRoot.get(),
                        tape.name.toJsonName
                    )
                }
            }

            if (!routingURL.isNullOrBlank())
                returnTape.routingUrl = routingURL?.ensureHttpPrefix

            return returnTape
        }
    }

    companion object {
        var tapeRoot: TapeRoot = VCRConfig.getConfig.tapeRoot
        private val gson = Gson()

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
        get() = field ?: File(
            VCRConfig.getConfig.tapeRoot.get(),
            name.toJsonName
        ).also { field = it }

    var recordedDate: Date? = Date()
        get() = field ?: Date()
    var modifiedDate: Date? = null
        get() = field ?: recordedDate ?: Date()

    var attractors: RequestAttractors? = null
    var routingUrl: String? = null

    var alwaysLive: Boolean? = null

    var chapters: MutableList<RecordedInteractions> = mutableListOf()

    private var tapeMode: TapeMode? = TapeMode.READ_WRITE
        get() = if (field == null) TapeMode.READ_WRITE else field

    val httpRoutingUrl: HttpUrl?
        get() = HttpUrl.parse(routingUrl.orEmpty())

    /**
     * routingUrl has data, but HttpUrl is unable to parse it
     */
    val isValidURL: Boolean
        get() = routingUrl.isValidURL

    fun clone(postClone: (BlankTape) -> Unit = {}) = BlankTape {
        it.tapeName = "${tapeName}_clone"
        it.recordedDate = Date()
        it.attractors = attractors?.clone()
        it.routingUrl = routingUrl
        it.alwaysLive = alwaysLive
        it.chapters = chapters.map { it.clone() }.toMutableList()
        it.tapeMode = tapeMode
    }.also { postClone.invoke(it) }

    override fun getName() = tapeName ?: RandomHost(hashCode()).valueAsChars()

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

    fun resetLimitedUses() {
        chapters.forEach { it.resetUses() }
    }

    @Transient
    var useWatcher: ((RecordedInteractions, Int?) -> Int)? = null
        get() {
            return field ?: { chap, value ->
                if (value != null)
                    chap.mockUses = value
                chap.mockUses
            }
        }

    var RecordedInteractions.uses
        get() = useWatcher!!.invoke(this, null)
        set(value) {
            useWatcher!!.invoke(this, value)
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
        return if (TapeCatalog.isTestRunning)
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
        val logBuilder = StringBuilder()

        val alwaysLive = findBestMatch(okRequest, SearchPreferences.AlwaysLive)
        if (alwaysLive.status == HttpStatusCode.Found) {
            println("Starting Live call for: ${okRequest.url()}")
            alwaysLive.item?.also {
                logBuilder.appendlnFmt(
                    {
                        "== Live Request ==\n" +
                                "-Name:\n %s\n" +
                                "-Valid URL: %b\n" +
                                "-Network: %b\n" +
                                "-Url: %s\n" +
                                "-Body:\n %s\n" +
                                "-Headers:\n %s" +
                                "%s\n"
                    },
                    it.name,
                    isValidURL,
                    hasNetworkAccess,
                    okRequest.url(),
                    okRequest.body().content("{null}").valueOrIsEmpty,
                    okRequest.headers().toString().valueOrIsEmpty,
                    if (it.uses >= 0)
                        "-Uses: ${it.uses}" else ""
                )

                if (isValidURL) {
                    val responseData = getData(okRequest)
                    logBuilder.appendlnFmt(
                        "== Live Response ==\n-Code %d from %s\n",
                        responseData.code(), okRequest.url()
                    )
                    if (responseData.isSuccessful) {
                        if (it.uses > 0) it.uses--
                        logBuilder.appendlnFmt(
                            {
                                "== Live Response Data ==\n" +
                                        "-Body:\n %s\n" +
                                        "-Headers:\n %s" +
                                        "%s\n"
                            },
                            if (it.responseData.isImage)
                                "[image]" else it.responseData?.body.tryAsJson,
                            it.responseData?.headers.toString(),
                            if (it.uses >= 0)
                                "\n-Remaining Uses: ${it.uses}" else ""
                        )
                        println(logBuilder.toString())
                        return responseData.toReplayResponse
                    }
                }
            }

            println(logBuilder.toString())
            return miniResponse(okRequest, HttpStatusCode.BadGateway).toReplayResponse
        }

        val awaitMock = findBestMatch(okRequest, SearchPreferences.AwaitOnly)
        if (awaitMock.status == HttpStatusCode.Found && isWritable) {
            awaitMock.item?.also {
                println("Queued Await request for: ${okRequest.url()}")
                logBuilder.appendlnFmt(
                    {
                        "== Await Request ==\n" +
                                "-Name: %s\n" +
                                "-Url: %s\n" +
                                "-Body:\n %s\n" +
                                "-Headers:\n %s" +
                                "%s"
                    },
                    it.name,
                    okRequest.url(),
                    okRequest.body().content("null").tryAsJson,
                    okRequest.headers().toStringPairs().joinToString("", " ", "\n"),
                    if (it.uses >= 0)
                        "\n-Uses: ${it.uses}" else ""
                )
                val responseData = getData(okRequest)
                logBuilder.appendlnFmt(
                    "== Await Response ==\n-Code (%d) from %s\n",
                    responseData.code(), okRequest.url()
                )

                return if (responseData.isSuccessful) {
                    it.requestData = okRequest.toTapeData
                    it.response = responseData.toReplayResponse
                    if (it.uses > 0) it.uses--
                    saveFile()

                    logBuilder.appendlnFmt(
                        {
                            "-Body:\n %s\n" +
                                    "-Headers:\n %s" +
                                    "%s"
                        },
                        if (it.responseData.isImage)
                            "[image]" else it.responseData?.body.tryAsJson,
                        it.responseData?.headers.toString(),
                        if (it.uses >= 0)
                            "\n-Remaining Uses: ${it.uses}" else ""
                    )

                    println(logBuilder.toString())
                    it.response
                        ?: miniResponse(okRequest, HttpStatusCode.NoContent).toReplayResponse
                } else {
                    println(logBuilder.toString())
                    miniResponse(okRequest, HttpStatusCode.BadGateway).toReplayResponse
                }
            }
        }

        val limitedMock = findBestMatch(okRequest, SearchPreferences.LimitedOnly)
        if (limitedMock.status == HttpStatusCode.Found) {
            limitedMock.item?.also {
                it.uses--
                printlnFmt(
                    "== Limited Mock ==\n" +
                            "-Name\n %s\n" +
                            "-Remaining Uses\n %s\n",
                    it.name, it.uses
                )
                return it.response
                    ?: miniResponse(okRequest, HttpStatusCode.NoContent).toReplayResponse
            }
        }

        val activeMock = findBestMatch(okRequest, SearchPreferences.MockOnly)
        if (activeMock.status == HttpStatusCode.Found) {
            activeMock.item?.also {
                printlnFmt(
                    "== Mock ==\n" +
                            "-Name\n %s\n",
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
                when (it.uses) {
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
                        it.uses == MockUseStates.ALWAYS.state

                    SearchPreferences.LimitedOnly ->
                        it.uses in (1..Int.MAX_VALUE)
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
            request.headers().toStringPairs(),
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
     * If there is an existing hard tape, then this call will update that file
     */
    fun saveIfExists() {
        if (file?.exists().isTrue())
            saveFile()
    }

    /**
     * Saves the data to file. A new file will be created if one doesn't exist
     */
    fun saveFile() {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                synchronized(this@BlankTape) {
                    val time = measureTimeMillis {
                        val tree = gson.toJsonTree(this@BlankTape).asJsonObject
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
                                    }.orFalse
                            }
                            .let { jsonList ->
                                JsonArray().apply { jsonList.forEach { add(it) } }
                            }

                        tree.add("chapters", keepChapters)

                        val outFile = file ?: return@synchronized

                        if (outFile.exists().isTrue() && (file?.nameWithoutExtension != tapeName))
                            outFile.delete()

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
                    println(
                        "Saved File (%d ms): %s".format(
                            time,
                            this@BlankTape.file?.path.orEmpty()
                        )
                    )
                }
            }
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
