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
import helpers.reHost
import helpers.toOkRequest
import helpers.toReplayResponse
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

class BlankTape private constructor(
    var tapeName: String = hashCode().toString()
) : Tape {
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

        fun build() = BlankTape(
            tapeName ?: hashCode().toString()
        ).also {
            it.usingCustomName = tapeName != null
            it.attractors = attractors
            it.routingUrl = routingURL
            it.mode = if (allowLiveRecordings == true)
                TapeMode.READ_WRITE else TapeMode.READ_ONLY
            it.file = File(
                VCRConfig.getConfig.tapeRoot.get(),
                it.tapeName.toJsonName
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

    @Transient
    var file: File? = null

    var attractors: RequestAttractors? = null
    var routingUrl: String? = null
    private var tapeMode: TapeMode? = TapeMode.READ_WRITE

    val httpRoutingUrl: HttpUrl?
        get() = HttpUrl.parse(routingUrl ?: "")

    /**
     * routingUrl has data, but HttpUrl is unable to parse it
     */
    val isUrlValid: Boolean
        get() = !routingUrl.isNullOrBlank() && httpRoutingUrl != null

    override fun getName() = tapeName

    @Transient
    var usingCustomName = false

    private enum class searchPreferences {
        ALL, MockOnly, LiveOnly, AwaitOnly
    }

    fun updateNameByURL(url: String) {
        usingCustomName = true
        tapeName = String.format(
            "%s_%d",
            url.removePrefix("/"),
            url.hashCode()
        )
    }

    var chapters: MutableList<RecordedInteractions> = mutableListOf()

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
            it.header("Content-Type", "text/plain")
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

        val awaitMock = findBestMatch(okRequest, searchPreferences.AwaitOnly)
        if (awaitMock.status == HttpStatusCode.Found) {
            awaitMock.item?.also {
                System.out.println("Await Request: ${okRequest.url()}\n Body:\n${okRequest.body()}")
                val responseData = getData(okRequest)
                System.out.println("Await Response: ${responseData.code()} from ${okRequest.url()}")
                return if (responseData.isSuccessful) {
                    it.response = responseData.toReplayResponse
                    if (it.mockUses > 0)
                        it.mockUses--
                    saveFile()
                    it.response
                } else {
                    miniResponse(okRequest, HttpStatusCode.BadGateway).toReplayResponse
                }
            }
        }

        val limitedMock = findBestMatch(okRequest, searchPreferences.MockOnly)
        if (limitedMock.status == HttpStatusCode.Found) {
            limitedMock.item?.also {
                it.mockUses--
                return it.response
            }
        }

        val liveMock = findBestMatch(okRequest, searchPreferences.LiveOnly)
        if (liveMock.status == HttpStatusCode.Found) {
            liveMock.item?.also {
                return it.response
            }
        }

        return defaultResponse
    }

    private fun findBestMatch(
        request: okhttp3.Request,
        preference: searchPreferences = searchPreferences.ALL
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
                    searchPreferences.ALL ->
                        true

                    searchPreferences.AwaitOnly ->
                        it.awaitResponse

                    searchPreferences.LiveOnly ->
                        it.mockUses == InteractionUseStates.ALWAYS.state

                    searchPreferences.MockOnly ->
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
        preference: searchPreferences = searchPreferences.ALL
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
}
