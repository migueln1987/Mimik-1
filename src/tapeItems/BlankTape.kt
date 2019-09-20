package tapeItems

import VCRConfig
import mimikMockHelpers.RecordedInteractions
import tapeItems.helpers.filteredBody
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.stream.JsonWriter
import helpers.anyTrue
import helpers.attractors.RequestAttractors
import io.ktor.http.HttpStatusCode
import mimikMockHelpers.QueryResponse
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okreplay.* // ktlint-disable no-wildcard-imports
import tapeItems.helpers.reHost
import java.io.File
import java.io.Writer
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class BlankTape private constructor(
    var tapeName: String = hashCode().toString()
) : Tape {
    @Suppress("unused", "UNUSED_PARAMETER")
    class Builder {
        constructor()
        constructor(config: Builder.() -> Unit) {
            config.invoke(this)
        }

        var tapeName: String? = null
        var routingURL: String? = null
        var attractors: RequestAttractors? = null
        /**
         * Allows this tape to accept new recordings
         *
         * Default: true
         */
        var allowLiveRecordings: Boolean? = true

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
                "NewTapes/" + it.tapeName.toJsonName
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
        var tapeRoot: TapeRoot = VCRConfig.getConfig.tapeRoot
        internal val gson = Gson()

        @Transient
        private val defaultResponse = object : Response {
            override fun code() = 0
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
        ALL, MockOnly, LiveOnly
    }

    fun updateNameByURL(url: String) {
        tapeName = String.format(
            "%s_%d",
            url.removePrefix("/"),
            url.hashCode()
        )
    }

    var chapters: MutableList<RecordedInteractions> = mutableListOf()

    override fun setMatchRule(matchRule: MatchRule?) {}

    override fun getMatchRule(): MatchRule =
        ComposedMatchRule.of(MatchRules.method, MatchRules.queryParams, filteredBody)

    override fun setMode(mode: TapeMode?) {
        tapeMode = mode
    }

    override fun getMode() = tapeMode ?: TapeMode.READ_WRITE
    override fun isReadable() = mode.isReadable
    override fun isWritable() = mode.isWritable
    override fun isSequential() = mode.isSequential

    override fun size() = chapters.size

    /**
     * Converts a okHttp request (with context of a tape) into a Interceptor Chain
     */
    fun requestToChain(request: okhttp3.Request): Interceptor.Chain? {
        fun getData(request: okhttp3.Request) =
            OkHttpClient().newCall(request).execute()

        return object : Interceptor.Chain {
            override fun request(): okhttp3.Request {
                val tapeURL = httpRoutingUrl ?: return request
                return request.reHost(tapeURL)
                    .newBuilder()
                    .header("HOST", tapeURL.host())
                    .build()
            }

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

    override fun seek(request: okreplay.Request): Boolean {
        return findBestMatch(request).status == HttpStatusCode.Found
    }

    override fun play(request: okreplay.Request): Response {
        val limitedMock = findBestMatch(request, searchPreferences.MockOnly)
        if (limitedMock.status == HttpStatusCode.Found) {
            limitedMock.item?.also {
                it.mockUses--
                return it.response
            }
        }

        val liveMock = findBestMatch(request, searchPreferences.LiveOnly)
        if (liveMock.status == HttpStatusCode.Found) {
            liveMock.item?.also {
                return it.response
            }
        }

        return defaultResponse
    }

    private fun findBestMatch(
        request: okreplay.Request,
        preference: searchPreferences = searchPreferences.ALL
    ): QueryResponse<RecordedInteractions> {
        val filteredChapters = chapters
            .filter {
                when (it.mockUses) {
                    RecordedInteractions.UseStates.ALWAYS.state -> anyTrue(
                        preference == searchPreferences.ALL,
                        preference == searchPreferences.LiveOnly
                    )
                    in (1..Int.MAX_VALUE) -> anyTrue(
                        preference == searchPreferences.ALL,
                        preference == searchPreferences.MockOnly
                    )
                    else -> false
                }
            }
            .associateBy({ it }, { it.attractors })

        return RequestAttractors.findBest(
            filteredChapters,
            request.url().encodedPath(),
            request.url().query(),
            if (request.hasBody()) request.bodyAsText() else null
        )
    }

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
                            RecordedInteractions.UseStates.ALWAYS.state,
                            RecordedInteractions.UseStates.DISABLE.state
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
        RecordedInteractions().also {
            interaction.invoke(it) // in case we want to change anything
            chapters.add(it)
        }
}
