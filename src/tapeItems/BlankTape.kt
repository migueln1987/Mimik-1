package tapeItems

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.stream.JsonWriter
import helpers.*
import helpers.attractors.*
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.*
import mimikMockHelpers.MockUseStates
import mimikMockHelpers.QueryResponse
import mimikMockHelpers.RecordedInteractions
import mimikMockHelpers.Responsedata
import networkRouting.editorPages.EditorModule.Companion.noData
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.internal.http.HttpMethod
import okreplay.*
import java.io.File
import java.io.Writer
import java.nio.charset.Charset
import java.util.ArrayDeque
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
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
            val returnTape = if (reBuild == null) {
                BlankTape { tape ->
                    tape.tapeName = tapeName
                    tape.attractors = attractors
                    tape.mode = if (allowLiveRecordings == true)
                        TapeMode.READ_WRITE else TapeMode.READ_ONLY
                    tape.alwaysLive = alwaysLive
                    tape.file = null
                }
            } else {
                var isHardTape = false
                if (reBuild.file?.exists().isTrue()) {
                    isHardTape = true
                    reBuild.file?.delete()
                }
                reBuild.file = null
                if (isHardTape)
                    reBuild.saveFile()
                reBuild
            }

            if (!routingURL.isNullOrBlank())
                returnTape.routingUrl = routingURL?.ensureHttpPrefix

            return returnTape
        }
    }

    companion object {
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
            TapeCatalog.Instance.config.tapeRoot.get(),
            name.toJsonName
        ).also { field = it }

    @Suppress("USELESS_ELVIS")
    @Transient
    var savingFile: AtomicBoolean = AtomicBoolean(false)
        get() = field ?: AtomicBoolean(false)

    var recordedDate: Date? = Date()
        get() = field ?: Date()
    var modifiedDate: Date? = null
        get() = field ?: recordedDate ?: Date()

    var attractors: RequestAttractors? = null
    var routingUrl: String? = null

    var alwaysLive: Boolean? = null

    var chapters: MutableList<RecordedInteractions> = mutableListOf()

    var byUnique: List<List<UniqueBit>>? = null

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
    var useWatchers: ArrayDeque<((RecordedInteractions, Int?) -> Int)> = ArrayDeque()
        @Synchronized
        get() {
            var localField = field
            if (localField.isNullOrEmpty()) {
                localField = ArrayDeque()
                localField.push(
                    { chap, value ->
                        if (value != null)
                            chap.mockUses = value
                        chap.mockUses
                    }
                )
            }
            field = localField
            return field
        }

    var RecordedInteractions.uses
        @Synchronized
        get() = useWatchers.peek().invoke(this, null)
        @Synchronized
        set(value) {
            useWatchers.peek().invoke(this, value)
        }

    /**
     * Converts a okHttp request (with context of a tape) into a Interceptor Chain
     */
    fun requestToChain(request: okhttp3.Request): Interceptor.Chain {
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
        findBestMatches(request.toOkRequest).status == HttpStatusCode.Found

    /**
     * Appends: [Name], Url, Headers, Body, and [Extras]
     */
    private val okhttp3.Request.logRequestData: (Name: String, Extras: String) -> String
        get() { // todo; move into logger module
            return { name, extras ->
                var bodyStr = body().content(noData).tryAsPrettyJson ?: noData

                val requestStr =
                    ("-Name: $name\n" +
                            "-Url: ${url()}\n" +
                            "-Headers:\n%s\n" +
                            "-Body:\n%s%s\n").format(
                        headers().toStringPairs()
                            .joinToString(separator = "", limit = 20, transform = { " $it\n" }).trimEnd('\n'),
                        bodyStr.limitLines(20).ensurePrefix(" "),
                        if (extras.isNotBlank()) "\n$extras" else ""
                    )
                "== Request ==\n$requestStr"
            }
        }

    /**
     * Appends: Status [Code] from [Url], {Headers and Body} from [Data], and [Extras].
     *
     * - If the Body is an image, then ```"[image]"``` is displayed
     * - Json bodies are formatted if possible
     */
    private val okhttp3.Request.logResponseData: (Data: Responsedata?, Extras: String) -> String
        get() { // todo; move into logger module
            return { data, extras ->
                val responseStr = if (data == null) extras
                else {
                    var bodyStr = if (data.isImage) " { image data }" else
                        data.body.tryAsPrettyJson ?: noData

                    ("-Code (%d) from ${url()}\n" +
                            "-Headers:\n%s\n" +
                            "-Body:\n%s%s\n").format(
                        data.code ?: 0,
                        data.headers?.toStringPairs()
                            ?.joinToString(separator = "", transform = { " $it\n" })?.trimEnd('\n') ?: noData,
                        bodyStr.limitLines(20).ensurePrefix(" "),
                        if (extras.isNotBlank()) "\n$extras" else ""
                    )
                }

                "== Response ==\n$responseStr"
            }
        }

    override fun play(request: okreplay.Request): Response {
        val okRequest: okhttp3.Request = request.toOkRequest
        val logBuilder = StringBuilder()

        val alwaysLive = findBestMatch(okRequest, SearchPreferences.AlwaysLive)
        if (alwaysLive.status == HttpStatusCode.Found) {
            println("Starting Live call for: ${okRequest.url()}")
            alwaysLive.item?.also {
                logBuilder.appendlns(
                    "=== Live ===",
                    okRequest.logRequestData.invoke(
                        it.name,
                        ("-Valid URL: $isValidURL\n" +
                                "-Network: $hasNetworkAccess%s").format(
                            if (it.uses >= 0)
                                "\n-Uses: ${it.uses}" else ""
                        )
                    )
                )

                if (isValidURL) {
                    val response = getData(okRequest)
                    val responseData = response.toTapeData

                    logBuilder.appendln(
                        okRequest.logResponseData.invoke(responseData, "")
                    )

                    if (response.isSuccessful) {
                        if (it.uses > 0) it.uses--
                        logBuilder.appendln(
                            if (it.uses >= 0)
                                "-Remaining Uses: ${it.uses}" else ""
                        )

                        println(logBuilder.toString())
                        return responseData.replayResponse
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
                logBuilder.appendlns(
                    "=== Await ===",
                    okRequest.logRequestData.invoke(
                        it.name,
                        ("-Valid URL: $isValidURL\n" +
                                "-Network: $hasNetworkAccess\n" +
                                "%s").format(
                            if (it.uses >= 0)
                                "-Uses: ${it.uses}" else ""
                        )
                    )
                )

                val response = getData(okRequest)
                val responseData = response.toTapeData
                logBuilder.appendln(
                    okRequest.logResponseData.invoke(responseData, "")
                )

                return if (response.isSuccessful) {
                    it.requestData = okRequest.toTapeData
                    it.responseData = responseData
                    if (it.uses > 0) it.uses--
                    saveFile()

                    logBuilder.appendln(
                        if (it.uses >= 0)
                            "-Remaining Uses: ${it.uses}" else ""
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
                logBuilder.appendlns(
                    "=== Limited ===",
                    okRequest.logRequestData.invoke(it.name, ""),
                    okRequest.logResponseData.invoke(
                        it.responseData,
                        if (it.uses >= 0)
                            "-Uses: ${it.uses}" else ""
                    )
                )

                return it.response
                    ?: miniResponse(okRequest, HttpStatusCode.NoContent).toReplayResponse
            }
        }

        val activeMock = findBestMatch(okRequest, SearchPreferences.MockOnly)
        if (activeMock.status == HttpStatusCode.Found) {
            activeMock.item?.also {
                logBuilder.appendlns(
                    "=== Mock ===",
                    okRequest.logRequestData.invoke(it.name, ""),
                    okRequest.logResponseData.invoke(it.responseData, "")
                )

                println(logBuilder.toString())
                return it.response
                    ?: miniResponse(okRequest, HttpStatusCode.NoContent).toReplayResponse
            }
        }

        return defaultResponse
    }

    private fun findBestMatch(
        request: okhttp3.Request,
        preference: SearchPreferences = SearchPreferences.ALL
    ) = findBestMatches(request, preference).let {
        QueryResponse<RecordedInteractions> {
            when {
                it.item == null ->
                    status = HttpStatusCode.NotFound
                it.item?.size == 1 -> {
                    status = HttpStatusCode.Found
                    item = it.item?.first()
                }
                else ->
                    status = HttpStatusCode.Conflict
            }
        }
    }

    private fun findBestMatches(
        request: okhttp3.Request,
        preference: SearchPreferences = SearchPreferences.ALL
    ): QueryResponse<List<RecordedInteractions>> {
        val requestHash = request.contentHash
        var cachedCall: RecordedInteractions? = null

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
            .filter {
                when {
                    cachedCall != null -> false
                    it.cachedCalls.contains(requestHash) -> {
                        cachedCall = it
                        false
                    }
                    else -> true
                }
            }
            .filter { it.attractors != null }
            .associateWith { it.attractors!! }

        return when {
            cachedCall != null ->
                QueryResponse(listOf(cachedCall!!))
            filteredChapters.isEmpty() ->
                QueryResponse { status = HttpStatusCode.NotFound }
            else -> RequestAttractors.findBest_many(
                filteredChapters,
                request.url().encodedPath(),
                request.url().query(),
                request.headers().toStringPairs(),
                if (HttpMethod.requiresRequestBody(request.method()))
                    request.body().content() else null
            ).also { qr ->
                if (qr.item?.size == 1)
                    qr.item?.first()?.cachedCalls?.add(request.contentHash)
            }
        }
    }

    override fun record(request: okreplay.Request, response: okreplay.Response) {
        val newChap = createNewInteraction {
            it.requestData = request.toTapeData
            it.responseData = response.toTapeData
            it.attractors = RequestAttractors(it.requestData)
            it.origionalMockUses = it.mockUses
            it.alwaysLive = alwaysLive
        }

        val okRequest = request.toOkRequest

        val logBuilder = StringBuilder()
        logBuilder.appendlns(
            "=== New Chapter ===",
            okRequest.logRequestData.invoke(newChap.name, ""),
            okRequest.logResponseData.invoke(newChap.responseData, "")
        )

        println(logBuilder.toString())
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
                    savingFile.set(true)
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
                    savingFile.set(false)
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
        RecordedInteractions { nChap ->
            interaction.invoke(nChap)
            appendIfUnique(nChap)
            chapters.add(nChap)
        }

    private fun appendIfUnique(chap: RecordedInteractions) {
        /* List of actions:
        1. Loop through each byUnique list {known as "uList"}
          - Look for first list who's contents (all) match this chapter's requestData
        2. Map uList's search string(s) to literal result string(s) {known as litList}
        3. Determine if any existing chapters (requestData) contain any of the unique literal strings
          - exit if any pass
        4. Append uList's contents to chap's attractors
          - if chap doesn't already contain uList's items (exact value)
        */

        val litList = byUnique?.firstNotNullResult { it.uniqueAllOrNull(chap) }
        val notUnique = chapters.any { litList.uniqueAllOrNull(it) != null }
        if (litList.isNullOrEmpty() || notUnique) return

        val queryMatchers = mutableListOf<RequestAttractorBit>()
        val headerMatchers = mutableListOf<RequestAttractorBit>()
        val bodyMatchers = mutableListOf<RequestAttractorBit>()

        litList.forEach {
            when (it.uniqueType) {
                UniqueTypes.Query -> queryMatchers.add(RequestAttractorBit(it.searchStr!!))
                UniqueTypes.Header -> headerMatchers.add(RequestAttractorBit(it.searchStr!!))
                UniqueTypes.Body -> bodyMatchers.add(RequestAttractorBit(it.searchStr!!))
                else -> Unit
            }
        }

        if (chap.attractors == null)
            chap.attractors = RequestAttractors()

        chap.attractors?.also { attrs ->
            if (queryMatchers.isNotEmpty()) {
                attrs.queryMatchers = attrs.queryMatchers.orEmpty().toMutableList()
                    .apply { removeIf { it.allowAllInputs.isTrue() } }
                    .append(queryMatchers)
            }
            if (headerMatchers.isNotEmpty()) {
                attrs.headerMatchers = attrs.headerMatchers.orEmpty().toMutableList()
                    .apply { removeIf { it.allowAllInputs.isTrue() } }
                    .append(headerMatchers)
            }
            if (bodyMatchers.isNotEmpty()) {
                attrs.bodyMatchers = attrs.bodyMatchers.orEmpty().toMutableList()
                    .apply { removeIf { it.allowAllInputs.isTrue() } }
                    .append(bodyMatchers)
            }
        }

        chap.cachedCalls.clear()
    }

    init {
        config.invoke(this)
    }
}
