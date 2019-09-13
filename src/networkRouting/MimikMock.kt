package com.fiserv.mimik.networkRouting

import com.fiserv.mimik.TapeCatalog
import com.fiserv.mimik.helpers.toHeaders
import com.fiserv.mimik.helpers.toReplayRequest
import com.fiserv.mimik.mimikMockHelpers.RecordedInteractions
import com.fiserv.mimik.mimikMockHelpers.RequestTapedata
import com.fiserv.mimik.mimikMockHelpers.ResponseTapedata
import com.fiserv.mimik.tapeItems.BlankTape
import com.fiserv.mimik.tapeItems.RequestAttractors
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
import okhttp3.Headers
import okhttp3.Protocol
import kotlin.math.max

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
                    call.respond(status, errorResponse ?: "")
                }
            }
        }

    private class MockRequestedResponse(build: MockRequestedResponse.() -> Unit) {
        var status: HttpStatusCode = HttpStatusCode.OK
        var interaction: RecordedInteractions? = null
        var errorResponse: String? = null

        init {
            build.invoke(this)
        }
    }

    private fun String.removePrefix(prefix: String, ignoreCase: Boolean): String {
        return if (startsWith(prefix, ignoreCase))
            substring(prefix.length, length)
        else this
    }

    private fun ApplicationCall.processPutMock(): MockRequestedResponse {
        val headers = request.headers
        val mockParams = headers.entries()
            .filter { it.key.startsWith("mock", true) }
            .associateBy({ it.key.removePrefix("mock", true) }, { it.value[0] })

        // Step 0: Pre-checks
        if (mockParams.isEmpty()) return MockRequestedResponse {
            status = HttpStatusCode.BadRequest
            errorResponse = "Missing mock params"
        }

        if (!mockParams.containsKey("Url_path")) return MockRequestedResponse {
            status = HttpStatusCode.BadRequest
            errorResponse = "Missing url routing path ('Url_path')"
        }

        val urlPath = mockParams.getValue("Url_path").removePrefix("/")

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
                routingURL = mockParams["Route_Url"]
                tapeName = String.format(
                    "%s_%d",
                    urlPath,
                    urlPath.hashCode()
                )
                attractors = RequestAttractors {
                    routingPath = urlPath
                }
            }.build()

        if (tape.httpRoutingUrl == null) return MockRequestedResponse {
            status = HttpStatusCode.PreconditionFailed
            errorResponse = "Missing routing url ('Route_Url')"
        }

        if (createdTape) tapeCatalog.tapes.add(tape)

        // Step 2: Get existing chapter (to override) or create a new one
        val requestMock = RequestTapedata() {
            it.method = mockParams["Method"]

            it.url = tape.httpRoutingUrl!!.newBuilder()
                .addPathSegments(
                    urlPath
                ).build()

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
                rData.code = mockParams["ResponseCode"]?.toIntOrNull()

                rData.headers = headers.entries()
                    .filter { it.key.startsWith("mockHeader", true) }
                    .associateBy(
                        { it.key.removePrefix("mockHeader", true) },
                        { it.value[0] }
                    )
                    .toHeaders()

                rData.body = runBlocking { receiveText() }
            }
            updateReplayData()

            if (mockParams.containsKey("Use"))
                mockUses = 1 // set as "1-time use"

            if (mockParams.containsKey("Uses")) {
                mockUses = when (val usesRequest = mockParams.getValue("Uses").toIntOrNull()) {
                    null, 0 -> 0 // reset usage count

                    // decrement mock requests, to a limit of 0
                    in Int.MIN_VALUE..0 -> max(0, mockUses - usesRequest)

                    else -> usesRequest
                }
            }
        }

        // Step 4: Profit!!!
        return MockRequestedResponse {
            status = HttpStatusCode.Created
            interaction = chapter
        }
    }

    private val okhttp3.Request.toMockResponse: okreplay.Response
        get() = toReplayRequest.let { request ->
            object : okreplay.Response {
                override fun code(): Int {
                    return (this@toMockResponse.header("mockResponseCode")?.toIntOrNull()?.let {
                        HttpStatusCode.fromValue(it)
                    } ?: HttpStatusCode.OK).value
                }

                override fun getEncoding() = request.encoding

                override fun body() = request.body()

                override fun newBuilder() = null

                override fun getContentType() = request.contentType

                override fun hasBody() = request.hasBody()

                override fun toYaml() = request.toYaml()

                override fun protocol() = Protocol.HTTP_2

                override fun bodyAsText() = request.bodyAsText()

                override fun getCharset() = request.charset

                override fun header(name: String) = request.header(name)

                override fun headers(): Headers {
                    return Headers.of(request.headers().toMultimap()
                        .filter { !it.key.startsWith("mock") }
                        .flatMap {
                            it.value.map { mvalue -> it.key to mvalue }
                        }
                        .toMap()
                    )
                }
            }
        }
}
