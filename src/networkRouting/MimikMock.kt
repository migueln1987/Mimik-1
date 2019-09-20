package networkRouting

import TapeCatalog
import helpers.anyTrue
import helpers.toHeaders
import mimikMockHelpers.RecordedInteractions
import mimikMockHelpers.RequestTapedata
import mimikMockHelpers.ResponseTapedata
import tapeItems.BlankTape
import helpers.attractors.RequestAttractors
import helpers.removePrefix
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.Routing
import io.ktor.routing.put
import mimikMockHelpers.QueryResponse
import helpers.attractors.RequestAttractorBit
import helpers.ensurePrefix
import helpers.isJSONValid
import helpers.isTrue
import io.ktor.routing.route
import java.lang.Exception

@Suppress("RemoveRedundantQualifierName")
class MimikMock(path: String) : RoutingContract(path) {

    private val tapeCatalog = TapeCatalog.Instance

    companion object {
        internal var selfPath = ""
    }

    init {
        selfPath = path
    }

    private enum class RoutePaths(val path: String) {
        MOCK("");
    }

    override fun init(route: Routing) {
        route.route(path) {
            mock
        }
    }

    private val Route.mock: Route
        get() = route(RoutePaths.MOCK.path) {
            put {
                call.processPutMock().apply {
                    call.respond(status, responseMsg ?: "")
                }
            }
        }

    private suspend fun ApplicationCall.processPutMock(): QueryResponse<BlankTape> {
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

        // Step 1: get existing tape or create a new tape
        val query = putmockGetTape(mockParams)

        val tape = query.item ?: return QueryResponse {
            status = HttpStatusCode.InternalServerError
        }

        when (query.status) {
            HttpStatusCode.Created -> { // set ONLY if this is a new tape
                if (!tape.usingCustomName)
                    attractors.routingPath?.also { tape.updateNameByURL(it) }
                if (attractors.hasData)
                    tape.attractors = attractors
            }
            HttpStatusCode.Found -> { // apply the tape's attractors to this object
                attractors.append(tape.attractors)
            }
        }

        if (mockParams["tape_save"].isTrue())
            tape.saveFile()

        if (mockParams.containsKey("tape_only")) return query

        // Step 2: Get existing chapter (to override) or create a new one
        val interactionName = mockParams["name"]

        var creatingNewChapter = false
        val chapter =
            tape.chapters.firstOrNull { it.chapterName == interactionName }
                ?: let {
                    creatingNewChapter = true
                    tape.createNewInteraction()
                }

        // Step 3: Set the MimikMock data
        val requestMock = RequestTapedata() { builder ->
            builder.method = mockParams["method"]

            val urlPath = (mockParams["route_path"] // try using the input data
                ?: attractors.routingPath ?: "") // or the tape data, or just nothing
                .removePrefix("/")

            if (attractors.routingPath == null && urlPath.isNotBlank())
                attractors.routingPath = urlPath.ensurePrefix("/")
            builder.url = tape.httpRoutingUrl?.run {
                newBuilder()
                    .addPathSegments(urlPath)
                    .query(mockParams["route_params"])
                    .build()
            }

            builder.headers = mockParams
                .filter { it.key.startsWith("headerin_") }
                .mapKeys { it.key.removePrefix("headerin_") }
                .toHeaders()
        }

        val bodyText = try {
            receiveText()
        } catch (e: Exception) {
            null
        }

        chapter.also {
            it.attractors = attractors
            it.requestData = requestMock

            // In case we want to update an existing chapter's name
            it.chapterName = interactionName ?: it.chapterName

            it.responseData = ResponseTapedata { rData ->
                rData.code = mockParams["response_code"]?.toIntOrNull()

                rData.headers = mockParams
                    .filter { it.key.startsWith("headerout_") }
                    .mapKeys { it.key.removePrefix("headerout_") }
                    .toHeaders()

                rData.body = bodyText
            }

            it.updateReplayData()

            val usesRequest = mockParams["use"]
            it.mockUses = if (mockParams["readonly"].isTrue()) {
                when (usesRequest?.toLowerCase()) {
                    "disable" -> RecordedInteractions.UseStates.DISABLE.state
                    else -> RecordedInteractions.UseStates.ALWAYS.state
                }
            } else {
                usesRequest?.toIntOrNull()
                    ?: when (usesRequest?.toLowerCase()) {
                        "always" -> RecordedInteractions.UseStates.ALWAYS.state
                        "disable" -> RecordedInteractions.UseStates.DISABLE.state
                        else -> it.mockUses
                    }
            }
        }

        if (tape.file?.exists().isTrue())
            tape.saveFile()

        val isJson = bodyText.isJSONValid

        // Step 4: Profit!!!
        return QueryResponse {
            val created = anyTrue(
                query.status == HttpStatusCode.Created,
                creatingNewChapter
            )
            status = if (created)
                HttpStatusCode.Created else
                HttpStatusCode.Found
            if (!isJson && bodyText.isNullOrBlank()) {
                responseMsg = "Note; input body is not recognized as a valid json."
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

        val urlPath = filters["path"]?.firstOrNull()?.ensurePrefix("/")

        val paramAttractors = filters.filterAttractorKeys("param") {
            it.split("&")
        }

        val bodyAttractors = filters.filterAttractorKeys("body")

        return RequestAttractors {
            routingPath = urlPath
            queryParamMatchers = paramAttractors
            queryBodyMatchers = bodyAttractors
        }
    }

    /**
     * Filters this [key, values] map into a list of Attractor bits
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
                    .flatMap {
                        (valueSplitter.invoke(it) ?: listOf(it)).asSequence()
                    }
                    .map {
                        RequestAttractorBit {
                            optional = kvvm.key.contains("~")
                            value = it
                        }
                    }
            }
            .toList()
    }

    /**
     * Attempts to find an existing tape suitable tape, or creates a new one.
     *
     * Find priority:
     * 1. tape name
     * 2. route values attractors
     */
    private fun putmockGetTape(mockParams: Map<String, String>): QueryResponse<BlankTape> {
        val paramTapeName = mockParams["tape_name"]?.split("/")?.last()
        // todo; add an option for sub-directories
        // val paramTapeDir = mockParams["tape_name"]?.replace(paramTapeName ?: "", "")

        var result = QueryResponse<BlankTape> {
            status = HttpStatusCode.NotFound
            item = tapeCatalog.tapes.firstOrNull { it.tapeName.equals(paramTapeName, true) }
                ?.also { status = HttpStatusCode.Found }
        }

        if (result.status != HttpStatusCode.Found) {
            // No matching tape by name
            // so try finding a tape which accepts the query path/ params
            val queryPath = mockParams["route_path"]
            val queryParams = mockParams["route_params"]

            val tapeMap = tapeCatalog.tapes
                .associateBy({ it }, { it.attractors })

            result = RequestAttractors.findBest(tapeMap, queryPath, queryParams)

            if (result.status != HttpStatusCode.Found) {
                // no viable tape found, so return a new tape
                result.status = HttpStatusCode.Created
                result.item = BlankTape.Builder() {
                    routingURL = mockParams["tape_url"]
                    allowLiveRecordings = mockParams["tape_allowliverecordings"].isTrue(true)
                    tapeName = paramTapeName
                }.build()
                    .also { tapeCatalog.tapes.add(it) }
            }
        }

        return result
    }
}
