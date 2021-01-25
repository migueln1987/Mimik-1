package networkRouting

import helpers.*
import helpers.attractors.RequestAttractorBit
import helpers.attractors.RequestAttractors
import helpers.attractors.UniqueBit
import helpers.attractors.UniqueTypes
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.response.*
import io.ktor.routing.Route
import io.ktor.routing.put
import io.ktor.routing.route
import mimikMockHelpers.MockUseStates
import mimikMockHelpers.QueryResponse
import mimikMockHelpers.Responsedata
import okhttp3.internal.http.HttpMethod
import tapeItems.BaseTape

/**
 * Server handle for creating mock tapes/ chapters from an API call
 *
 * @see <a href="0.0.0.0:4321/mock">mock</a>
 */
class MimikMock : RoutingContract(RoutePaths.rootPath) {

    enum class HeaderTags(private val _value: String? = null) {
        /*
        == Tag groupings ==

        Sandbox:
        == header
        - Name
        <- ID (so future calls can edit items)

        == body
        - Attractors (how to lock a "device" to a sandbox)
        - Sequence Actions (maybe?)

        Tape:
        == header
        - Name
        - Routing URL
        - Always Live
        - Allow New Recordings (tape = read_only/ read_write)
        - clear chapters (action)

        == body
        - Attractors
        - Sequence Actions (maybe?)
        - by unique (pending feature)

        Chapter:
        == header
        - Name
        - Always Live
        - sandbox-bound (memory only, don't save to file) todo; add feature
        - response code
        - output headers
        - clear cached calls

        == body
        - Attractors
        - Sequence Actions
        - Request (optional, passive)
        - Response
         */

        /**
         * Usage: [Tape], [Chapter]
         *
         * Name of tape/chapter to use (or create)
         * {Tape}{Chapter}
         */
        Name,

        /**
         * Usage: [Tape]
         *
         * Routing url where chapters will connect to for live data
         */
        Url,

        /**
         * Usage: [Tape], [Chapter]
         *
         * - When set on a tape; all requests matching the attractors will route the call as live
         * - When set on a chapter; requests will use mock chapters
         */
        Live,

        /**
         * Usage: [Tape]
         *
         * When set to [True], the tape will allow new recordings to be saved during usage.
         *
         * - Note, not the same as adding tapes via URL GUI
         */
        AllowNewRecordings,

        /**
         * Usage: [Tape], [Chapter]
         *
         * - [True]; body contains items for (any): request, response, attractors, sequences, and by unique data
         * - [False] (default); body contains response data
         */
        @Deprecated("process body to determine if it's an advanced body")
        AdvBody,

        /**
         * Usage: [Tape], [Chapter]
         *
         * When [True], the tape will be saved to file
         */
        saveToFile,

        /**
         * Usage: [Tape], [Chapter]
         *
         * When [True], this item will only be accessible by the mentioned bounds
         */
        boundOnly,

        @Deprecated("probably not even needed")
        Method,

        @Deprecated("Move to (learner) box")
        Await,

        @Deprecated("Move to (learner) box")
        Response_Code
        ;

        val value: String get() = _value.valueOrNull ?: name.toLowerCase()
    }

    operator fun Map<String, String>.get(tag: HeaderTags) = this[tag.value]
    fun Map<String, String>.containsKey(tag: HeaderTags) = this.containsKey(tag.value)

    val arrayReg = """"(?<!\\),""".toRegex()

    private enum class RoutePaths(val path: String) {
        /**
         * show help error
         */
        MOCK(""),

        /**
         * Create tape
         */
        TAPE("tape"),

        /**
         * Create chapter
         */
        CHAPTER("chapter");

        companion object {
            const val rootPath = "mock"
        }
    }

    override fun init(route: Route) {
        /*
        Create tape:
        1. pre-process data
        2. determine

        Create chapter:
        1. pre-process data
        */

        route.route(path) {
            mock
            tape
            chapter
        }
    }

    private val Route.mock: Route
        get() = route(RoutePaths.MOCK.path) {
            put {
//                call.processPutMock().apply {
//                    println(responseMsg)
//                    call.respond(status, responseMsg.orEmpty())
//                }
            }
        }

    private val Route.tape: Route
        get() = route(RoutePaths.TAPE.path) {
            put {
                call.processTapeData().apply {
                    println(responseMsg)
                    call.respond(status, responseMsg.orEmpty())
                }
            }
        }

    private val Route.chapter: Route
        get() = route(RoutePaths.CHAPTER.path) {
            put {
                call.processChapterData().apply {
                    println(responseMsg)
                    call.respond(status, responseMsg.orEmpty())
                }
            }
        }

    /**
     * Part 0: Pre-checks
     * - verify the request contains any mock data
     *
     * @return Queried mock headers
     * - Map of mock related items. key = header name, minus "mock" prefix
     * - or [HttpStatusCode.BadRequest] if the headers don't contain "mock..." items
     */
    private fun ApplicationCall.preProcessCheck(): QueryResponse<Map<String, String>> {
        val headers = request.headers
        // result
        // input: mockTape_Name: Something
        // output: tape_name: Something
        val mockParams = headers.entries().asSequence()
            .filter { it.key.startsWith("mock", true) }
            .associateBy(
                { it.key.removePrefix("mock", true).toLowerCase() },
                { it.value[0] }
            )

        // Step 0: Pre-checks
        // Validate the request contains "mock..." data
        if (mockParams.isEmpty()) return QueryResponse {
            status = HttpStatusCode.BadRequest
            responseMsg = "Missing mock params. Ex: mock{variable}: {value}"
        }

        return QueryResponse(mockParams)
    }

    private fun ApplicationCall.processTapeData(): QueryResponse<BaseTape> {
        // Step 0; pre-process data
        val (mockParams, checkStatus, checkMsg) = preProcessCheck()
        if (checkStatus == HttpStatusCode.BadRequest || mockParams.isNullOrEmpty()) {
            return QueryResponse {
                status = checkStatus
                responseMsg = checkMsg
            }
        }

        val attractors = createRequestAttractor("tape", request.headers)
//        val uniqueFilters = createUniqueFilters(request.headers)

        // Step 1: get existing tape or create a new tape
        val tapeQuery = getTape(mockParams, true)
        val tape = tapeQuery.item ?: return QueryResponse {
            status = HttpStatusCode.InternalServerError
            responseMsg = HttpStatusCode.InternalServerError.description
        }

        when (tapeQuery.status) {
            HttpStatusCode.Created -> { // set ONLY if this is a new tape
                if (attractors.hasData)
                    tape.attractors = attractors
//                if (uniqueFilters != null)
//                    tape.byUnique = uniqueFilters
            }
            HttpStatusCode.Found -> { // apply the tape's attractors to this object
                attractors.append(tape.attractors)
            }
        }

        tapeQuery.item?.alwaysLive = if (mockParams[HeaderTags.Live].isStrTrue()) true else null

        if (mockParams[HeaderTags.saveToFile].isStrTrue())
            tape.saveFile()

        // Step 4: Profit!!!
        return QueryResponse {
            val created = tapeQuery.status == HttpStatusCode.Created
            status = if (created) HttpStatusCode.Created else HttpStatusCode.Found
            responseMsg = "Tape (%s): %s".format(
                if (tapeQuery.status == HttpStatusCode.Created) "New" else "Old",
                tapeQuery.item?.name
            )
        }
    }

    private suspend fun ApplicationCall.processChapterData(): QueryResponse<BaseTape> {
        // Step 0; pre-process data
        val prepData = preProcessCheck()
        if (prepData.status == HttpStatusCode.BadRequest) {
            return QueryResponse {
                status = prepData.status
                responseMsg = prepData.responseMsg
            }
        }

        val mockParams = prepData.item.orEmpty()

        // Step 1: get existing tape or fail
        val tapeItems = mockParams.filterKeys { it.startsWith("tape") }
            .map { (key, value) -> key.removePrefix("tape") to value }.toMap()
        val tapeQuery = getTape(tapeItems)
        val tape = tapeQuery.item ?: return QueryResponse {
            status = HttpStatusCode.FailedDependency
            responseMsg = when {
                !tapeItems.containsKey("name") -> "Missing header `tape_name`"
                tapeItems.containsKey("name") -> "Unable to find tape, ${tapeItems["name"]}"
                else -> HttpStatusCode.FailedDependency.description
            }
        }

        // apply the tape's attractors to this object
        val attractors = createRequestAttractor("chap", request.headers).also {
            it.append(tape.attractors)
        }

        if (mockParams[HeaderTags.saveToFile].isStrTrue())
            tape.saveFile()

        // Step 2: Get existing chapter (to override) or create a new one
        val interactionName = mockParams[HeaderTags.Name]

        var creatingNewChapter = false
        val chapter = tape.chapters.firstOrNull { it.name == interactionName }
            ?: let {
                creatingNewChapter = true
                tape.createNewInteraction()
            }

        // determine if the body should be split (enhanced) or as-is

//        if (mockParams.containsKey(HeaderTags.AdvBody))

        // Step 3: Set the MimikMock data
//        if (!chapter.hasRequestData)
//            chapter.requestData = RequestTapedata()

//        val requestMock = RequestTapedata() { builder ->
//            builder.method = mockParams["method"]
//
//            builder.url = tape.httpRoutingUrl?.newBuilder()
//                ?.apply {
//                    attractors.routingPath?.value?.also {
//                        addPathSegments(it.removePrefix("/"))
//                    }
//                    query(mockParams["route_params"])
//                }?.build().toString()
//
//            builder.headers = mockParams
//                .filter { it.key.startsWith("headerin_") }
//                .mapKeys { it.key.removePrefix("headerin_") }
//                .toHeaders.valueOrNull
//        }

        val bodyText = tryGetBody()
        val alwaysLive = if (mockParams[HeaderTags.Live].isStrTrue()) true else null

        // Method will have a body and filter isn't allowing bodies
        if (HttpMethod.requiresRequestBody(mockParams[HeaderTags.Method].orEmpty()) &&
            (attractors.bodyMatchers.isNullOrEmpty().isTrue ||
                attractors.bodyMatchers?.all { it.hardValue.isBlank() }.isTrue)
        ) // add the default "accept all bodies" to calls requiring a body
            attractors.bodyMatchers = listOf(RequestAttractorBit { it.allowAllInputs = true })

        chapter.also { updateChapter ->
            updateChapter.alwaysLive = alwaysLive
            updateChapter.attractors = attractors
            updateChapter.cachedCalls.clear()
//            updateChapter.requestData = requestMock

            // In case we want to update an existing chapter's name
            updateChapter.chapterName = interactionName ?: updateChapter.name

            val hasAwait = mockParams.containsKey(HeaderTags.Await)
            val awaitResponse = mockParams[HeaderTags.Await].isStrTrue()
            updateChapter.responseData = if (alwaysLive.isTrue || (hasAwait && awaitResponse))
                null
            else Responsedata { rData ->
                rData.code = mockParams[HeaderTags.Response_Code]?.toIntOrNull()

                rData.headers = mockParams
                    .filter { it.key.startsWith("headerout_") }
                    .mapKeys { it.key.removePrefix("headerout_") }
                    .toHeaders.valueOrNull

                // todo 1; Beautify the input if it's a valid json?
                // todo 2; skip body if the method doesn't allow bodies
                rData.body = bodyText
            }

//            val useRequest = mockParams[HeaderTags.Uses]
//            updateChapter.mockUses = if (mockParams["readonly"].isStrTrue()) {
//                when (useRequest?.toLowerCase()) {
//                    "disable" -> MockUseStates.DISABLE
//                    else -> MockUseStates.ALWAYS
//                }.state
//            } else {
//                useRequest?.toIntOrNull()
//                    ?: when (useRequest?.toLowerCase()) {
//                        "disable" -> MockUseStates.DISABLE
//                        "always" -> MockUseStates.ALWAYS
//                        else -> MockUseStates.asState(updateChapter.mockUses)
//                    }.state
//            }
        }

        tape.saveIfExists()

        val isJson = bodyText.isValidJSON

        // Step 4: Profit!!!
        return QueryResponse {
            val created = anyTrue(
//                query.status == HttpStatusCode.Created,
                creatingNewChapter
            )
            status = if (created) HttpStatusCode.Created else HttpStatusCode.Found
            responseMsg = "Tape (%s): %s, Mock (%s): %s".format(
//                if (query.status == HttpStatusCode.Created) "New" else "Old",
//                query.item?.name,
                if (creatingNewChapter) "New" else "Old",
                chapter.name
            )
            if (!isJson && bodyText.isNullOrBlank()) {
                responseMsg += "\nNote; input body is not recognized as a valid json.\n" +
                    bodyText.isValidJSONMsg
            }
        }

        return QueryResponse()
    }

    /**
     * API call which
     * - Creates tapes
     *    - [header] mockTape_...
     * - Create Chapter/ Mock
     *    - ff
     */
    private suspend fun ApplicationCall.processPutMock(): QueryResponse<BaseTape> {
        val headers = request.headers
        // result
        // input: mockTape_Name: Something
        // output: tape_name: Something
        var mockParams = headers.entries().asSequence()
            .filter { it.key.startsWith("mock", true) }
            .associateBy(
                { it.key.removePrefix("mock", true).toLowerCase() },
                { it.value[0] }
            )

        // Step 0: Pre-checks
        // Validate the request contains "mock..." data
        if (mockParams.isEmpty()) return QueryResponse {
            status = HttpStatusCode.BadRequest
            responseMsg = "Missing mock params. Ex: mock{variable}: {value}"
        }

        // Step 0.5: prepare variables
        mockParams = mockParams
            .filterNot { it.key.startsWith("filter", true) }
        val attractors = createRequestAttractor("", request.headers)
        val uniqueFilters = createUniqueFilters(request.headers)
        val alwaysLive = if (mockParams["live"].isStrTrue()) true else null

        // Step 1: get existing tape or create a new tape
        val query = getTape(mockParams)

        val tape = query.item ?: return QueryResponse {
            status = HttpStatusCode.InternalServerError
            responseMsg = HttpStatusCode.InternalServerError.description
        }

        when (query.status) {
            HttpStatusCode.Created -> { // set ONLY if this is a new tape
                if (attractors.hasData)
                    tape.attractors = attractors
                if (uniqueFilters != null)
                    tape.byUnique = uniqueFilters
            }
            HttpStatusCode.Found -> { // apply the tape's attractors to this object
                attractors.append(tape.attractors)
            }
        }

        query.item?.alwaysLive = alwaysLive

        if (mockParams["tape_save"].isStrTrue())
            tape.saveFile()

        if (mockParams.containsKey("tape_only")) return query.also {
            it.responseMsg = "Tape (%s): %s"
                .format(
                    if (query.status == HttpStatusCode.Created) "New" else "Old",
                    query.item?.name
                )
        }

        // Step 2: Get existing chapter (to override) or create a new one
        val interactionName = mockParams["name"] ?: mockParams["_name"]

        var creatingNewChapter = false
        val chapter =
            tape.chapters.firstOrNull { it.name == interactionName }
                ?: let {
                    creatingNewChapter = true
                    tape.createNewInteraction()
                }

        // Step 3: Set the MimikMock data
//        if (!chapter.hasRequestData)
//            chapter.requestData = RequestTapedata()

        val hasAwait = mockParams.containsKey("await")
        val awaitResponse = mockParams["await"].isStrTrue()

//        val requestMock = RequestTapedata() { builder ->
//            builder.method = mockParams["method"]
//
//            builder.url = tape.httpRoutingUrl?.newBuilder()
//                ?.apply {
//                    attractors.routingPath?.value?.also {
//                        addPathSegments(it.removePrefix("/"))
//                    }
//                    query(mockParams["route_params"])
//                }?.build().toString()
//
//            builder.headers = mockParams
//                .filter { it.key.startsWith("headerin_") }
//                .mapKeys { it.key.removePrefix("headerin_") }
//                .toHeaders.valueOrNull
//        }

        val bodyText = tryGetBody()

        // Method will have a body and filter isn't allowing bodies
        if (HttpMethod.requiresRequestBody(mockParams["method"].orEmpty()) &&
            (attractors.bodyMatchers.isNullOrEmpty().isTrue ||
                attractors.bodyMatchers?.all { it.hardValue.isBlank() }.isTrue)
        ) // add the default "accept all bodies" to calls requiring a body
            attractors.bodyMatchers = listOf(RequestAttractorBit { it.allowAllInputs = true })

        chapter.also { updateChapter ->
            updateChapter.alwaysLive = alwaysLive
            updateChapter.attractors = attractors
            updateChapter.cachedCalls.clear()
//            updateChapter.requestData = requestMock

            // In case we want to update an existing chapter's name
            updateChapter.chapterName = interactionName ?: updateChapter.name

            updateChapter.responseData = if (alwaysLive.isTrue || (hasAwait && awaitResponse))
                null
            else Responsedata { rData ->
                rData.code = mockParams["response_code"]?.toIntOrNull()

                rData.headers = mockParams
                    .filter { it.key.startsWith("headerout_") }
                    .mapKeys { it.key.removePrefix("headerout_") }
                    .toHeaders.valueOrNull

                // todo 1; Beautify the input if it's a valid json?
                // todo 2; skip body if the method doesn't allow bodies
                rData.body = bodyText
            }

            val useRequest = mockParams["use"]
            updateChapter.mockUses = if (mockParams["readonly"].isStrTrue()) {
                when (useRequest?.toLowerCase()) {
                    "disable" -> MockUseStates.DISABLE
                    else -> MockUseStates.ALWAYS
                }.state
            } else {
                useRequest?.toIntOrNull()
                    ?: when (useRequest?.toLowerCase()) {
                        "disable" -> MockUseStates.DISABLE
                        "always" -> MockUseStates.ALWAYS
                        else -> MockUseStates.asState(updateChapter.mockUses)
                    }.state
            }
        }

        tape.saveIfExists()

        val isJson = bodyText.isValidJSON

        // Step 4: Profit!!!
        return QueryResponse {
            val created = anyTrue(
                query.status == HttpStatusCode.Created,
                creatingNewChapter
            )
            status = if (created) HttpStatusCode.Created else HttpStatusCode.Found
            responseMsg = "Tape (%s): %s, Mock (%s): %s".format(
                if (query.status == HttpStatusCode.Created) "New" else "Old",
                query.item?.name,
                if (creatingNewChapter) "New" else "Old",
                chapter.name
            )
            if (!isJson && bodyText.isNullOrBlank()) {
                responseMsg += "\nNote; input body is not recognized as a valid json.\n" +
                    bodyText.isValidJSONMsg
            }
        }
    }

    /**
     * Creates a Request Attractor from the input [headers] that contain "mock{[prefixReq]}Filter"
     */
    private fun createRequestAttractor(prefixReq: String = "", headers: Headers): RequestAttractors {
        val filterKey = "mock${prefixReq}filter_"
        val filters = headers.entries()
            .filter { it.key.contains(filterKey, true) }
            .associateBy(
                { it.key.toLowerCase().removePrefix(filterKey) },
                { it.value })

        // A. url path, single
        val urlPath = filters["path"]?.firstOrNull()

        // B. queries, multiple + split "&" parts
        val queryAttractors = filters.filterAttractorKeys("query") {
            it.split("&")
        }

        val bodyAttractors = filters.filterAttractorKeys("body")

        var headAttractors = filters.filterAttractorKeys("header")
        if (headAttractors.isEmpty())
            headAttractors = headAttractors.toMutableList().apply {
                add(RequestAttractorBit { it.allowAllInputs = true })
            }

        return RequestAttractors { attr ->
            if (urlPath != null)
                attr.routingPath = RequestAttractorBit(urlPath)
            if (queryAttractors.isNotEmpty())
                attr.queryMatchers = queryAttractors
            if (bodyAttractors.isNotEmpty())
                attr.bodyMatchers = bodyAttractors
            attr.headerMatchers = headAttractors
        }
    }

    // Todo; finish feature
    private fun createUniqueFilters(headers: Headers): List<List<UniqueBit>>? {
        val filterKey = "mockunique_"
        val filters = headers.entries()
            .filter { it.key.contains(filterKey, true) }
            .associateBy(
                { it.key.toLowerCase().removePrefix(filterKey) },
                { vMap ->
                    vMap.value.flatMap { it.split(arrayReg) }
                        .filterNot { it.isBlank() }
                        .map { it.trim() }
                })

        val uQuery = filters["query"]?.map { UniqueBit(it, UniqueTypes.Query) }
        val uHead = filters["head"]?.map { UniqueBit(it, UniqueTypes.Header) }
        val uBody = filters["body"]?.map { UniqueBit(it, UniqueTypes.Body) }

        return listOfNotNull(uQuery, uHead, uBody).let {
            if (it.isEmpty()) null else it
        }
    }

    /**
     * Filters this [key, values] map into a list of Attractor bits.
     * Function also sets optional and required flags
     */
    private fun Map<String, List<String>>.filterAttractorKeys(
        key: String,
        valueSplitter: (String) -> List<String>? = { null }
    ): List<RequestAttractorBit> {
        return asSequence()
            .filter { it.key.contains(key) }
            .filterNot { it.value.isEmpty() }
            .flatMap { (header_key, header_values) ->
                header_values.asSequence()
                    .filterNot { it.isEmpty() }
                    .flatMap { it.split(arrayReg).asSequence() }
                    .filterNot { it.isBlank() }
                    .map { it.trim() }
                    .flatMap {
                        (valueSplitter.invoke(it) ?: listOf(it)).asSequence()
                    }
                    .map {
                        val postfix = header_key.takeLast(3)
                        val isOpt = postfix.contains("~")
                        val isExpt = postfix.contains("!")
                        val isBase64 = postfix.contains("#")
                        val strData = if (isBase64)
                            it.fromBase64 else it

                        // override, to allow all inputs
                        val allowAll = allTrue(
                            strData == ".*", // catch any inputs
                            !isOpt // required request
                            // isExpt is ignored, because there is no opposite of (zero to many)
                        )

                        RequestAttractorBit { bit ->
                            if (allowAll) {
                                bit.allowAllInputs = true
                            } else {
                                bit.optional = isOpt
                                bit.except = isExpt
                                bit.value = strData
                            }
                        }
                    }
            }
            .toList()
    }

    /**
     * Attempts to find an existing tape suitable tape (by name) or creates a new one.
     *
     * If a tape is created, it'll also be added to the tape catalog automatically
     *
     * @param canCreateTape Determines if this function can create a tape (during the `mock/tape` call
     * - Returns [null] if the tape can't be found or created
     * @param name What the tape is named
     * - May include path, but not supported yet
     * @param url Routing url
     * @param allowliverecordings If new recordings are allowed to be added to the tape
     *
     * @return Query response containing the requested tape, or a newly created tape (if [canCreateTape] is [true])
     */
    @Suppress("KDocUnresolvedReference", "SpellCheckingInspection")
    private fun getTape(mockParams: Map<String, String>, canCreateTape: Boolean = false): QueryResponse<BaseTape> {
        val paramTapeName = mockParams[HeaderTags.Name]?.split("/")?.last()
        // todo; add an option for sub-directories
        // val paramTapeDir = mockParams["tape_name"]?.replace(paramTapeName ?: "", "")

        val result = QueryResponse<BaseTape> {
            status = HttpStatusCode.NotFound
            item = tapeCatalog.tapes.firstOrNull { it.name.equals(paramTapeName, true) }
                ?.also { status = HttpStatusCode.Found }
        }

        if (canCreateTape && result.status != HttpStatusCode.Found) {
            result.status = HttpStatusCode.Created
            result.item = BaseTape.Builder {
                it.tapeName = paramTapeName
                it.routingURL = mockParams[HeaderTags.Url]
                it.allowNewRecordings = mockParams[HeaderTags.AllowNewRecordings].isStrTrue(true)
            }.build()
                .also { tapeCatalog.tapes.add(it) }
        }

        return result
    }
}
