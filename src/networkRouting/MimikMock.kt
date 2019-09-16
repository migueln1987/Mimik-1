package networkRouting

import TapeCatalog
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
        when {
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
        }?.let { return it }

        val urlPath = mockParams.getValue("route_path").removePrefix("/")

        // Step 1: get existing tape (using attractors) or create a new tape
        var createdTape = false
        val tape = tapeCatalog.tapes
            .firstOrNull {
                // attempt to use an existing tape
                it.attractors?.routingPath == urlPath
            }
            ?: BlankTape.Builder() {
                createdTape = true
                subDirectory = "NewTapes"
                routingURL = mockParams["route_url"]
                tapeName = mockParams["tape_name"]
                    ?: String.format(
                        "%s_%d",
                        urlPath,
                        urlPath.hashCode()
                    )
                attractors = RequestAttractors {
                    routingPath = urlPath
                }
            }.build()

        if (createdTape) tapeCatalog.tapes.add(tape)

        if (mockParams["tape_hard"] == "true")
            tape.saveFile()

        if (mockParams.containsKey("tape_only")) return MockRequestedResponse {
            status = HttpStatusCode.Created
        }

        // Step 2: Get existing chapter (to override) or create a new one
        val requestMock = RequestTapedata() {
            it.method = mockParams["method"]

            it.url = tape.httpRoutingUrl!!.newBuilder()
                .addPathSegments(urlPath)
                .query(mockParams["query"])
                .build()

            it.headers = request.headers
                .filter { s, _ -> !s.startsWith("mock", true) }
                .toHeaders()
        }

        val chapter = tape.tapeChapters
            .firstOrNull {
                it.attractors?.matchesRequest(requestMock.replayRequest) == true
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

            if (mockParams.containsKey("uses")) {
                val usesRequest = mockParams.getValue("uses")
                val asNumber = usesRequest.toIntOrNull()

                mockUses = asNumber
                    ?: when (usesRequest.toLowerCase()) {
                        "always" -> RecordedInteractions.UseStates.ALWAYS.state
                        "disable" -> RecordedInteractions.UseStates.DISABLE.state
                        else -> mockUses
                    }
            }
        }

        if (tape.file?.exists() == true)
            tape.saveFile()

        // Step 4: Profit!!!
        return MockRequestedResponse {
            status = HttpStatusCode.Created
            interaction = chapter
        }
    }
}
