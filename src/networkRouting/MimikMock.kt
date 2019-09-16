package networkRouting

import TapeCatalog
import helpers.allTrue
import helpers.anyTrue
import helpers.ensurePrefix
import helpers.isFalse
import helpers.isTrue
import helpers.toHeaders
import mimikMockHelpers.RecordedInteractions
import mimikMockHelpers.RequestTapedata
import mimikMockHelpers.ResponseTapedata
import tapeItems.BlankTape
import tapeItems.RequestAttractors
import helpers.removePrefix
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.Routing
import io.ktor.routing.put
import io.ktor.util.filter
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl

@Suppress("RemoveRedundantQualifierName")
class MimikMock(path: String) : RoutingContract(path) {

    private val tapeCatalog = TapeCatalog.Instance

    companion object {
        internal var selfPath = ""
    }

    init {
        selfPath = path
    }

    private enum class RoutePaths(private val value: String) {
        MOCK("");

        val path: String
            get() = selfPath + if (value.isBlank())
                "" else "/$value"
    }

    override fun init(route: Routing) {
        route.apply {
            mock
        }
    }

    private val Routing.mock: Route
        get() = apply {
            put(RoutePaths.MOCK.path) {
                call.processPutMock().apply {
                    call.respond(status, responseMsg ?: "")
                }
            }
        }

    private class MockRequestedResponse(build: MockRequestedResponse.() -> Unit) {
        var status: HttpStatusCode = HttpStatusCode.OK
        var interaction: RecordedInteractions? = null
        var responseMsg: String? = null

        init {
            build.invoke(this)
        }
    }

    private fun ApplicationCall.processPutMock(): MockRequestedResponse {
        val headers = request.headers
        val mockParams = headers.entries()
            .filter { it.key.startsWith("mock", true) }
            .associateBy(
                { it.key.removePrefix("mock", true).toLowerCase() },
                { it.value[0] }
            )

        // Step 0: Pre-checks
        putmockPreChecks(mockParams)?.let { return it }

        val urlPath = mockParams.getValue("route_path").ensurePrefix("/")

        // Step 1: get existing tape (using attractors) or create a new tape
        val tape = putmockFindTape(mockParams, urlPath)

        if (mockParams.containsKey("tape_only")) return MockRequestedResponse {
            status = HttpStatusCode.Created
        }

        // Step 2: Get existing chapter (to override) or create a new one
        val requestMock = RequestTapedata() {
            it.method = mockParams["method"]

            it.url = tape.httpRoutingUrl!!.newBuilder()
                .addPathSegments(urlPath.removePrefix("/"))
                .query(mockParams["query"])
                .build()

            it.headers = request.headers
                .filter { s, _ -> !s.startsWith("mock", true) }
                .toHeaders()
        }

        val chapter = tape.chapters
            .firstOrNull {
                it.attractors?.matchesRequest(requestMock.replayRequest).isTrue()
            }
            ?: tape.createNewInteraction {
                it.requestData = requestMock
            }

        // Step 3: Set the MimikMock data
        chapter.apply {
            responseData = ResponseTapedata { rData ->
                rData.code = mockParams["response_code"]?.toIntOrNull()

                rData.headers = mockParams
                    .filter { it.key.startsWith("header") }
                    .mapKeys { it.key.removePrefix("header") }
                    .toHeaders()

                rData.body = runBlocking { receiveText() }
            }
            updateReplayData()

            val usesRequest = mockParams["use"]
            mockUses = if (mockParams.containsKey("readonly")) {
                when (usesRequest?.toLowerCase()) {
                    "disable" -> RecordedInteractions.UseStates.DISABLE.state
                    else -> RecordedInteractions.UseStates.ALWAYS.state
                }
            } else {
                usesRequest?.toIntOrNull()
                    ?: when (usesRequest?.toLowerCase()) {
                        "always" -> RecordedInteractions.UseStates.ALWAYS.state
                        "disable" -> RecordedInteractions.UseStates.DISABLE.state
                        else -> mockUses
                    }
            }
        }

        if (tape.file?.exists().isTrue())
            tape.saveFile()

        // Step 4: Profit!!!
        return MockRequestedResponse {
            status = HttpStatusCode.Created
            interaction = chapter
        }
    }

    /**
     * Ensures the mockParams contains the required minimum keys
     */
    private fun putmockPreChecks(mockParams: Map<String, String>): MockRequestedResponse? {
        return when {
            mockParams.isEmpty() -> MockRequestedResponse {
                status = HttpStatusCode.BadRequest
                responseMsg = "Missing mock params. Ex: mock{variable}: {value}"
            }

            !mockParams.containsKey("route_url") -> MockRequestedResponse {
                status = HttpStatusCode.PreconditionFailed
                responseMsg = "Missing routing url. Ex; mockRoute_Url: 'http://{routing url}.com'"
            }

            HttpUrl.parse(mockParams.getValue("route_url")) == null -> MockRequestedResponse {
                status = HttpStatusCode.PreconditionFailed
                responseMsg = "Invalid routing url. Ex; mockRoute_Url: 'http://{routing url}.com'"
            }

            !mockParams.containsKey("route_path") -> MockRequestedResponse {
                status = HttpStatusCode.BadRequest
                responseMsg = "Missing url routing path. Ex: mockRoute_Path: 'sub/path'"
            }

            else -> null
        }
    }

    /**
     * Attempts to find a suitable tape (must be writable), or creates a new one.
     *
     * Find priority:
     * 1. tape name (Action: add/ update chapter)
     * 2. chapter attractors (Action: update chapter)
     * 3. tape attractors (Action: add chapter)
     */
    private fun putmockFindTape(mockParams: Map<String, String>, urlPath: String): BlankTape {
        val tape = tapeCatalog.tapes
            .firstOrNull { tapeScan ->
                anyTrue(
                    // check #1
                    allTrue(
                        tapeScan.readOnly.isFalse(),
                        tapeScan.tapeName == mockParams["tape_name"]
                    ),

                    // check #2 (allow only the chapters we could update)
                    tapeScan.chapters.asSequence()
                        .filterNot { it.readOnly }
                        .filter { it.attractors?.matchesPath(mockParams["route_path"]).isTrue() }
                        .filter { it.attractors?.matchesQuery(mockParams["query"]).isTrue() }
                        .any(),

                    // check #3
                    allTrue(
                        tapeScan.readOnly.isFalse(),
                        tapeScan.attractors?.matchesPath(urlPath).isTrue(),
                        tapeScan.attractors?.matchesQuery(mockParams["query"]).isTrue()
                    )
                )
            }
            ?: BlankTape.Builder() {
                subDirectory = "NewTapes"
                routingURL = mockParams["route_url"]
                allowNewRecordings = mockParams["tape_allowrecordings"]?.toBoolean()
                tapeName = mockParams["tape_name"]
                    ?: String.format(
                        "%s_%d",
                        urlPath.removePrefix("/"),
                        urlPath.hashCode()
                    )
                attractors = RequestAttractors {
                    routingPath = urlPath
                }
            }.build()
                .also { tapeCatalog.tapes.add(it) }

        if (mockParams["tape_hard"] == "true")
            tape.saveFile()

        return tape
    }
}
