@file:Suppress("unused", "RemoveRedundantQualifierName", "KDocUnresolvedReference")

package mimik.tapeItems

import com.google.gson.*
import com.google.gson.stream.JsonWriter
import io.ktor.http.*
import kotlinUtils.*
import kotlinUtils.collections.firstNotNullResult
import kotlinUtils.text.append
import kotlinUtils.text.appendLine
import kotlinUtils.text.appendLines
import kotlinx.coroutines.*
import mimik.helpers.*
import mimik.helpers.attractors.*
import mimik.mockHelpers.*
import mimik.networkRouting.editorPages.EditorModule.Companion.isBlank
import mimik.networkRouting.editorPages.EditorModule.Companion.noData
import okhttp3.*
import okhttp3.Headers.Companion.headersOf
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.internal.http.HttpMethod
import okreplay.*
import java.io.File
import java.io.Writer
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureTimeMillis

class BaseTape private constructor(config: (BaseTape) -> Unit = {}) : Tape {
    class Builder(val reBuild: BaseTape? = null, config: (Builder) -> Unit = {}) {
        var tapeName: String? = null
            set(value) {
                field = if (value?.isBlank().isTrue)
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
                    reBuild.attractors = value
                    if (value == null || value.isInitial)
                        reBuild.attractors = null
                }
            }

        /**
         * Allows this tape to accept new recordings
         *
         * Default: true
         */
        var allowNewRecordings: Boolean? = true
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
            config(this)
        }

        /**
         * If [reBuild] is not null, then builder values will be applied to it, else a new tape will be made
         */
        fun build(): BaseTape {
            val returnTape = if (reBuild == null) {
                BaseTape { tape ->
                    tape.tapeName = tapeName
                    tape.attractors = attractors
                    tape.mode = if (allowNewRecordings == true)
                        TapeMode.READ_WRITE else TapeMode.READ_ONLY
                    tape.alwaysLive = alwaysLive
                    tape.file = null
                }
            } else {
                var isHardTape = false
                if (reBuild.file?.exists().isTrue) {
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
        /**
         * Empty response which returns [HttpStatusCode.Gone]
         */
        @Transient
        private val defaultResponse = object : okreplay.Response {
            override fun code() = HttpStatusCode.Gone.value
            override fun protocol() = Protocol.HTTP_1_1

            override fun getEncoding() = ""
            override fun getCharset() = Charset.defaultCharset()

            override fun headers() = headersOf("Empty", "")
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
        fun reBuild(reBuild: BaseTape? = null, config: (Builder) -> Unit = {}) =
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

    @Transient
    var savingFile: AtomicBoolean = AtomicBoolean(false)
        get() {
            @Suppress("SENSELESS_COMPARISON")
            if (field == null)
                field = AtomicBoolean(false)
            return field
        }

    var recordedDate: Date? = Date()
        get() = field ?: Date()
    var modifiedDate: Date? = null
        get() = field ?: recordedDate ?: Date()

    var attractors: RequestAttractors? = null
    var routingUrl: String? = null

    var alwaysLive: Boolean? = null

    var chapters: MutableList<RecordedInteractions> = mutableListOf()

    // Todo; finish feature, "create new chapter if request's unique data is different than an existing chapter"
    var byUnique: List<List<UniqueBit>>? = null

    private var tapeMode: TapeMode? = TapeMode.READ_WRITE
        get() = if (field == null) TapeMode.READ_WRITE else field

    val httpRoutingUrl: HttpUrl?
        get() = routingUrl.orEmpty().toHttpUrlOrNull()

    /**
     * Returns true if this [String] is a valid Url
     */
    val isValidURL: Boolean
        get() = routingUrl.isValidURL

    fun clone(postClone: (BaseTape) -> Unit = {}) = BaseTape { newTape ->
        newTape.tapeName = "${tapeName}_clone"
        newTape.recordedDate = Date()
        newTape.attractors = attractors?.clone()
        newTape.routingUrl = routingUrl
        newTape.alwaysLive = alwaysLive
        newTape.chapters = chapters.map { it.clone() }.toMutableList()
        newTape.tapeMode = tapeMode
    }.also { postClone(it) }

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
                // create a "constant" default
                localField = ArrayDeque()
                localField.push { chap, value ->
                    if (value != null)
                        chap.mockUses = value
                    chap.mockUses
                }
            }
            field = localField
            return field
        }

    var RecordedInteractions.uses
        @Synchronized
        get() = useWatchers.peek()(this, null)
        @Synchronized
        set(value) {
            useWatchers.peek()(this, value)
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
            tryOrNull { OkHttpClient().newCall(request).execute() }
                ?: miniResponse(request, HttpStatusCode.ServiceUnavailable)
    }

    override fun seek(request: okreplay.Request) =
        findBestMatches(request.toOkRequest).status == HttpStatusCode.Found

    /**
     * Appends to an output [String]:
     * - [ChapName]
     * - Url
     * - Headers
     * - Body
     * - [Extras]
     */
    private fun okhttp3.Request.logRequestData(
        ChapName: String,
        Extras: () -> String = { "" }
    ): String { // todo; move into logger module
        val requestStr = StringBuilder()
            .appendLine("-Name: $ChapName")
            .appendLine("-Url: $url")
            .appendLine("-Headers:\n%s") {
                it.format(
                    headers.toStringPairs()
                        .joinToString(separator = "", limit = 20, transform = { " $it\n" })
                        .trimEnd('\n')
                )
            }
            .append("-Body:\n%s") {
                var bodyStr = body.content(noData).tryAsPrettyJson ?: noData
                if (bodyStr.isBlank())
                    bodyStr = isBlank
                it.format(
                    bodyStr.limitLines(20).ensurePrefix(" ")
                )
            }
            .appendLine {
                Extras().let { extras ->
                    if (extras.isNotBlank())
                        "\n$extras" else ""
                }
            }

        return "== Request ==\n$requestStr"
    }

    /**
     * Appends: Status [Code] from [Url], {Headers and Body} from [Data], and [Extras].
     *
     * - If the Body is an image, then ```"[image]"``` is displayed
     * - Json bodies are formatted if possible
     */
    private fun okhttp3.Request.logResponseData(
        Data: ResponseData?,
        Extras: () -> String = { "" }
    ): String { // todo; move into logger module
        val extras = Extras().let { extras ->
            if (extras.isNotBlank())
                "\n$extras" else ""
        }

        val responseStr = if (Data == null) extras
        else {
            StringBuilder()
                .appendLine("-Code (%d) from $url") {
                    it.format(Data.code ?: 0)
                }
                .appendLine("-Headers:\n%s") {
                    it.format(
                        Data.headers?.toStringPairs()
                            ?.joinToString(separator = "", transform = { " $it\n" })
                            ?.trimEnd('\n')
                            ?: noData
                    )
                }
                .append("-Body:\n%s") {
                    val bodyStr = if (Data.isImage)
                        " { image data }" else
                        Data.body.tryAsPrettyJson ?: noData
                    it.format(bodyStr.limitLines(20).ensurePrefix(" "))
                }
                .appendLine(extras)
                .toString()
        }

        return "== Response ==\n$responseStr"
    }

    override fun play(request: okreplay.Request): okreplay.Response {
        val okRequest: okhttp3.Request = request.toOkRequest
        val logBuilder = StringBuilder()

        val searchTypes = listOf(
            SearchPreferences.AlwaysLive,
            SearchPreferences.AwaitOnly,
            SearchPreferences.LimitedOnly,
            SearchPreferences.MockOnly
        )

        val channelOutput = searchTypes.asSequence()
            .map { it to findBestMatch(okRequest, it) }
            .filter { it.second.status == HttpStatusCode.Found }
            .filter {
                // AwaitOnly must have the writable state as true
                // all other preferences: return true
                !(it.first == SearchPreferences.AwaitOnly && !isWritable)
            }
            .filter {
                // AlwaysLive always returns true (as the call is made during it's usage)
                // all other preferences: must have a non-null item data
                if (it.first == SearchPreferences.AlwaysLive)
                    true else it.second.item != null
            }.firstOrNull()

        if (channelOutput == null) {
            println("Using default [Gone] response for: ${okRequest.url}")
            return defaultResponse
        }

        val chapter = channelOutput.second.item
        var returnResponse: okreplay.Response? = null

        when (channelOutput.first) {
            SearchPreferences.AlwaysLive -> {
                println("Starting Live call for: ${okRequest.url}")
                if (chapter == null) {
                    logBuilder.appendLines(
                        "=== Live ===",
                        okRequest.logRequestData("[Missing]") {
                            ("-Valid URL: $isValidURL\n" +
                                "-Network: $hasNetworkAccess")
                        }
                    )
                    returnResponse = miniResponse(okRequest, HttpStatusCode.BadGateway).toReplayResponse
                } else {
                    logBuilder.appendLines(
                        "=== Live ===",
                        okRequest.logRequestData(chapter.name) {
                            ("-Valid URL: $isValidURL\n" +
                                "-Network: $hasNetworkAccess%s").format(
                                if (chapter.uses >= 0)
                                    "\n-Uses: ${chapter.uses}" else ""
                            )
                        }
                    )

                    if (isValidURL) {
                        val response = getData(okRequest)
                        val responseData = response.toTapeData

                        logBuilder.appendLine(okRequest.logResponseData(responseData))

                        if (response.isSuccessful) {
                            if (chapter.uses > 0) chapter.uses--
                            logBuilder.appendLine(
                                if (chapter.uses >= 0)
                                    "-Remaining Uses: ${chapter.uses}" else ""
                            )

                            println(logBuilder.toString())
                            returnResponse = responseData.replayResponse
                        }
                    } else
                        returnResponse = miniResponse(okRequest, HttpStatusCode.BadGateway).toReplayResponse
                }
            }

            SearchPreferences.AwaitOnly -> {
                requireNotNull(chapter)
                println("Queued Await request for: ${okRequest.url}")
                logBuilder.appendLines(
                    "=== Await ===",
                    okRequest.logRequestData(chapter.name) {
                        ("-Valid URL: $isValidURL\n" +
                            "-Network: $hasNetworkAccess\n" +
                            "%s").format(
                            if (chapter.uses >= 0)
                                "-Uses: ${chapter.uses}" else ""
                        )
                    }
                )

                val response = getData(okRequest)
                val responseData = response.toTapeData
                logBuilder.appendLine(okRequest.logResponseData(responseData))

                if (response.isSuccessful) {
                    chapter.requestData = okRequest.toTapeData
                    chapter.responseData = responseData
                    if (chapter.uses > 0) chapter.uses--
                    saveFile()

                    logBuilder.appendLine {
                        if (chapter.uses >= 0)
                            "-Remaining Uses: ${chapter.uses}" else ""
                    }

                    println(logBuilder.toString())
                    returnResponse = chapter.response
                        ?: miniResponse(okRequest, HttpStatusCode.NoContent).toReplayResponse
                } else {
                    println(logBuilder.toString())
                    returnResponse = miniResponse(okRequest, HttpStatusCode.BadGateway).toReplayResponse
                }
            }

            SearchPreferences.LimitedOnly -> {
                requireNotNull(chapter)
                chapter.uses--
                logBuilder.appendLines(
                    "=== Limited ===",
                    okRequest.logRequestData(chapter.name),
                    okRequest.logResponseData(chapter.responseData) {
                        if (chapter.uses >= 0)
                            "-Uses: ${chapter.uses}" else ""
                    }
                )

                returnResponse = chapter.response
                    ?: miniResponse(okRequest, HttpStatusCode.NoContent).toReplayResponse
            }

            SearchPreferences.MockOnly -> {
                requireNotNull(chapter)
                logBuilder.appendLines(
                    "=== Mock ===",
                    okRequest.logRequestData(chapter.name),
                    okRequest.logResponseData(chapter.responseData)
                )

                println(logBuilder.toString())
                returnResponse = chapter.response
                    ?: miniResponse(okRequest, HttpStatusCode.NoContent).toReplayResponse
            }

            else -> Unit
        }

        if (chapter != null && returnResponse != null) {
//            val qq = TapeCatalog.Instance.responseLinker.newLink {
//                println("aa")
//                returnResponse.edit {
//                    it.headers = it.headers.newBuilder().also { builder ->
//                        builder.add("Mimik_UUID", "")
//                    }.build()
//                }.toResponse()
//            }
//            val pp = returnResponse.edit {
//                it.code = 245
//            }
//            val ee = pp.hashCode()
            return returnResponse
        }

        return defaultResponse
    }

    fun play_old(request: okreplay.Request): okreplay.Response {
        val okRequest: okhttp3.Request = request.toOkRequest
        val logBuilder = StringBuilder()

        val alwaysLive = findBestMatch(okRequest, SearchPreferences.AlwaysLive)
        if (alwaysLive.status == HttpStatusCode.Found) {
            println("Starting Live call for: ${okRequest.url}")
            alwaysLive.item?.also {
                logBuilder.appendLines(
                    "=== Live ===",
                    okRequest.logRequestData(it.name) {
                        ("-Valid URL: $isValidURL\n" +
                            "-Network: $hasNetworkAccess%s").format(
                            if (it.uses >= 0)
                                "\n-Uses: ${it.uses}" else ""
                        )
                    }
                )

                if (isValidURL) {
                    val response = getData(okRequest)
                    val responseData = response.toTapeData

                    logBuilder.appendLine(
                        okRequest.logResponseData(responseData)
                    )

                    if (response.isSuccessful) {
                        if (it.uses > 0) it.uses--
                        logBuilder.appendLine(
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
                println("Queued Await request for: ${okRequest.url}")
                logBuilder.appendLines(
                    "=== Await ===",
                    okRequest.logRequestData(it.name) {
                        ("-Valid URL: $isValidURL\n" +
                            "-Network: $hasNetworkAccess\n" +
                            "%s").format(
                            if (it.uses >= 0)
                                "-Uses: ${it.uses}" else ""
                        )
                    }
                )

                val response = getData(okRequest)
                val responseData = response.toTapeData
                logBuilder.appendLine(okRequest.logResponseData(responseData))

                if (response.isSuccessful) {
                    it.requestData = okRequest.toTapeData
                    it.responseData = responseData
                    if (it.uses > 0) it.uses--
                    saveFile()

                    logBuilder.appendLine(
                        if (it.uses >= 0)
                            "-Remaining Uses: ${it.uses}" else ""
                    )

                    println(logBuilder.toString())
                    return it.response
                        ?: miniResponse(okRequest, HttpStatusCode.NoContent).toReplayResponse
                } else {
                    println(logBuilder.toString())
                    return miniResponse(okRequest, HttpStatusCode.BadGateway).toReplayResponse
                }
            }
        }

        val limitedMock = findBestMatch(okRequest, SearchPreferences.LimitedOnly)
        if (limitedMock.status == HttpStatusCode.Found) {
            limitedMock.item?.also {
                it.uses--
                logBuilder.appendLines(
                    "=== Limited ===",
                    okRequest.logRequestData(it.name),
                    okRequest.logResponseData(it.responseData) {
                        if (it.uses >= 0)
                            "-Uses: ${it.uses}" else ""
                    }
                )

                return it.response
                    ?: miniResponse(okRequest, HttpStatusCode.NoContent).toReplayResponse
            }
        }

        val activeMock = findBestMatch(okRequest, SearchPreferences.MockOnly)
        if (activeMock.status == HttpStatusCode.Found) {
            activeMock.item?.also {
                logBuilder.appendLines(
                    "=== Mock ===",
                    okRequest.logRequestData(it.name),
                    okRequest.logResponseData(it.responseData)
                )

                println(logBuilder.toString())
                return it.response
                    ?: miniResponse(okRequest, HttpStatusCode.NoContent).toReplayResponse
            }
        }

        return defaultResponse
    }

    /**
     * Shell for [findBestMatches] which returns a single query response of:
     * - Chapter (or null)
     * - Status of the chapter: [HttpStatusCode.NotFound], [HttpStatusCode.Found], [HttpStatusCode.Found]
     */
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

    /**
     * Searches for the best match from cached calls and chapters.
     *
     * Filters:
     * - must be enabled
     * - chapter's state is within range of [preference]
     * - chapter has attractors
     * - chapter with the highest required matching attractors
     */
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
                    MockUseStates.DISABLEDLIMITED.state -> false
                    else -> true
                }
            }
            .filter {
                when (preference) {
                    SearchPreferences.ALL -> true
                    SearchPreferences.AlwaysLive -> it.alwaysLive.isTrue
                    SearchPreferences.AwaitOnly -> it.awaitResponse
                    SearchPreferences.MockOnly -> it.uses == MockUseStates.ALWAYS.state
                    SearchPreferences.LimitedOnly -> it.uses in (1..Int.MAX_VALUE)
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
                request.url.encodedPath,
                request.url.query,
                request.headers.toStringPairs(),
                if (HttpMethod.requiresRequestBody(request.method))
                    request.body.content() else null
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
            it.originalMockUses = it.mockUses
            it.alwaysLive = alwaysLive
        }

        val okRequest = request.toOkRequest

        val logBuilder = StringBuilder()
        logBuilder.appendLines(
            "=== New Chapter ===",
            okRequest.logRequestData(newChap.name),
            okRequest.logResponseData(newChap.responseData)
        )

        println(logBuilder.toString())
        saveFile()
    }

    /**
     * If there is an existing hard tape, then this call will update that file
     */
    fun saveIfExists() {
        if (file?.exists().isTrue)
            saveFile()
    }

    /**
     * Saves the data to file. A new file will be created if one doesn't exist
     */
    fun saveFile() {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                synchronized(this@BaseTape) {
                    savingFile.set(true)
                    val time = measureTimeMillis {
                        val tree = TapeCatalog.gson.toJsonTree(this@BaseTape).asJsonObject
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

                        if (outFile.exists().isTrue && (file?.nameWithoutExtension != tapeName))
                            outFile.delete()

                        val canSaveFile = if (outFile.exists())
                            outFile.canWrite()
                        else {
                            outFile.parentFile.mkdirs()
                            outFile.createNewFile()
                        }

                        if (canSaveFile)
                            outFile.bufferedWriter().jWriter
                                .also { TapeCatalog.gson.toJson(tree, it) }
                                .close()
                    }
                    savingFile.set(false)
                    println(
                        "Saved File (%d ms): %s".format(
                            time,
                            this@BaseTape.file?.path.orEmpty()
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
            interaction(nChap)
            // appendIfUnique(nChap)
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
                    .apply { removeIf { it.allowAllInputs.isTrue } }
                    .append(queryMatchers)
            }
            if (headerMatchers.isNotEmpty()) {
                attrs.headerMatchers = attrs.headerMatchers.orEmpty().toMutableList()
                    .apply { removeIf { it.allowAllInputs.isTrue } }
                    .append(headerMatchers)
            }
            if (bodyMatchers.isNotEmpty()) {
                attrs.bodyMatchers = attrs.bodyMatchers.orEmpty().toMutableList()
                    .apply { removeIf { it.allowAllInputs.isTrue } }
                    .append(bodyMatchers)
            }
        }

        chap.cachedCalls.clear()
    }

    init {
        config(this)
    }

    override fun toString(): String {
        return "\"%s\", Attractors: %s, Chapters: %d".format(
            name, attractors?.toString().orEmpty(), chapters.size
        )
    }
}
