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
import io.ktor.response.respond
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

    val arrayReg = "(?<!\\\\),".toRegex()

    private enum class RoutePaths(val path: String) {
        MOCK("");

        companion object {
            const val rootPath = "mock"
        }
    }

    override fun init(route: Route) {
        route.route(path) {
            mock
        }
    }

    private val Route.mock: Route
        get() = route(RoutePaths.MOCK.path) {
            put {
                call.processPutMock().apply {
                    println(responseMsg)
                    call.respond(status, responseMsg.orEmpty())
                }
            }
        }

    private suspend fun ApplicationCall.processPutMock(): QueryResponse<BaseTape> {
        val headers = request.headers
        var mockParams = headers.entries().asSequence()
            .filter { it.key.startsWith("mock", true) }
            .associateBy(
                { it.key.removePrefix("mock", true).toLowerCase() },
                { it.value[0] }
            )

        // Step 0: Pre-checks
        if (mockParams.isEmpty()) return QueryResponse {
            status = HttpStatusCode.BadRequest
            responseMsg = "Missing mock params. Ex: mock{variable}: {value}"
        }

        // Step 0.5: prepare variables
        mockParams = mockParams
            .filterNot { it.key.startsWith("filter", true) }
        val attractors = createRequestAttractor(request.headers)
        val uniqueFilters = createUniqueFilters(request.headers)
        val alwaysLive = if (mockParams["live"].isTrue()) true else null

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

        if (mockParams["tape_save"].isTrue())
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
        val awaitResponse = mockParams["await"].isTrue()

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
            (attractors.bodyMatchers.isNullOrEmpty().isTrue() ||
                    attractors.bodyMatchers?.all { it.hardValue.isBlank() }.isTrue())
        ) // add the default "accept all bodies" to calls requiring a body
            attractors.bodyMatchers = listOf(RequestAttractorBit { it.allowAllInputs = true })

        chapter.also { updateChapter ->
            updateChapter.alwaysLive = alwaysLive
            updateChapter.attractors = attractors
            updateChapter.cachedCalls.clear()
//            updateChapter.requestData = requestMock

            // In case we want to update an existing chapter's name
            updateChapter.chapterName = interactionName ?: updateChapter.name

            updateChapter.responseData = if (alwaysLive.isTrue() || (hasAwait && awaitResponse))
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
            updateChapter.mockUses = if (mockParams["readonly"].isTrue()) {
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
     * Creates a Request Attractor from the input [headers] that contain "mockFilter"
     */
    private fun createRequestAttractor(headers: Headers): RequestAttractors {
        val filterKey = "mockfilter_"
        val filters = headers.entries()
            .filter { it.key.contains(filterKey, true) }
            .associateBy(
                { it.key.toLowerCase().removePrefix(filterKey) },
                { it.value })

        val urlPath = filters["path"]?.firstOrNull()

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
            .flatMap { kvvm ->
                kvvm.value.asSequence()
                    .filterNot { it.isEmpty() }
                    .flatMap { it.split(arrayReg).asSequence() }
                    .filterNot { it.isBlank() }
                    .map { it.trim() }
                    .flatMap {
                        (valueSplitter.invoke(it) ?: listOf(it)).asSequence()
                    }
                    .map {
                        val postfix = kvvm.key.takeLast(2)
                        val isOpt = postfix.contains("~")
                        val isAvd = postfix.contains("!")

                        val allowAll = allTrue(
                            it == ".*",
                            !isOpt,
                            !isAvd
                        )

                        RequestAttractorBit { bit ->
                            if (allowAll) {
                                bit.allowAllInputs = true
                            } else {
                                bit.optional = isOpt
                                bit.except = isAvd
                                bit.value = it
                            }
                        }
                    }
            }
            .toList()
    }

    /**
     * Attempts to find an existing tape suitable tape (by name) or creates a new one.
     */
    private fun getTape(mockParams: Map<String, String>): QueryResponse<BaseTape> {
        val paramTapeName = mockParams["tape_name"]?.split("/")?.last()
        // todo; add an option for sub-directories
        // val paramTapeDir = mockParams["tape_name"]?.replace(paramTapeName ?: "", "")

        val result = QueryResponse<BaseTape> {
            status = HttpStatusCode.NotFound
            item = tapeCatalog.tapes.firstOrNull { it.name.equals(paramTapeName, true) }
                ?.also { status = HttpStatusCode.Found }
        }

        if (result.status != HttpStatusCode.Found) {
            result.status = HttpStatusCode.Created
            result.item = BaseTape.Builder {
                it.tapeName = paramTapeName
                it.routingURL = mockParams["tape_url"]
                it.allowNewRecordings = mockParams["tape_allowliverecordings"].isTrue(true)
            }.build()
                .also { tapeCatalog.tapes.add(it) }
        }

        return result
    }
}
