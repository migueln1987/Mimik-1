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
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respondText
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
                val mockInteraction = call.processPutMock()
                call.respondText(ContentType.parse("text/plain"), HttpStatusCode.OK) {
                    mockInteraction.bodyKey
                }
            }
        }

    private fun ApplicationCall.processPutMock(): RecordedInteractions {
        /*
           Mock input:
           - query (as regex?)
           - headers
           -- url_path
           -- mockMethod
           -- mockResponseCode
           -- mockUse/ mockUses
           - body
           -- request
           -- response
            */
        val headers = request.headers
        val mockParams = headers.entries()
            .filter { it.key.startsWith("mock") }
            .associateBy({ it.key.removePrefix("mock") }, { it.value[0] })

        // Step 1: get existing tape (using attractors) or create a new tape
        var creatingNewTape = false
        val tape = if (mockParams.containsKey("Url_path")) {
            tapeCatalog.tapes
                .firstOrNull {
                    // attempt to use an existing tape
                    it.attractors?.routingPath == mockParams["Url_path"]
                }
                ?: BlankTape.Builder() {
                    creatingNewTape = true
                    subDirectory = "NewTapes"
                    tapeName = String.format(
                        "%s_%d",
                        mockParams["Url_path"],
                        mockParams["Url_path"].hashCode()
                    )
                    attractors = RequestAttractors {
                        routingPath = mockParams["Url_path"]
                    }
                }.build()
        } else {
            BlankTape.Builder() {
                creatingNewTape = true
                subDirectory = "NewTapes"
                tapeName = mockParams.hashCode().toString()
            }.build()
        }

        if (creatingNewTape) tapeCatalog.tapes.add(tape)

        // Step 2: get existing chapter (to override) or create a new one
        val requestMock = RequestTapedata() {
            it.method = mockParams["Method"]

            if (mockParams.containsKey("Url_path")) {
                it.url = tape.HttpRoutingUrl?.newBuilder()
                    ?.addPathSegments(
                        mockParams["Url_path"]?.removePrefix("/") ?: ""
                    )
                    ?.build()
            }

            it.headers = request.headers
                .filter { s, _ -> !s.startsWith("mock") }
                .toHeaders()
        }

        val chapter = tape.tapeChapters
            .firstOrNull {
                it.attractors?.matchesRequest(requestMock.replayRequest) == true
            }
            ?: tape.createNewInteraction {
                it.requestData = requestMock
            }

        // Step 3: start setting the MimikMock data
        return chapter.apply {
            responseData = ResponseTapedata {
                it.code = mockParams["ResponseCode"]?.toIntOrNull()
                    ?: HttpStatusCode.OK.value

                it.headers = headers.entries()
                    .filter { f -> f.key.startsWith("mockHEADER") }
                    .associateBy(
                        { kv -> kv.key.removePrefix("mockHEADER") },
                        { kv -> kv.value[0] }
                    )
                    .toHeaders()

                it.body = runBlocking { receiveText() }
            }
            updateReplayData()

            if (mockParams.containsKey("Use"))
                mockUses = 1 // set as "1-time use"

            if (mockParams.containsKey("Uses")) {
                mockUses = when (val usesRequest = mockParams["Uses"]?.toIntOrNull()) {
                    null, 0 -> {// reset usage count
                        0
                    }
                    in Int.MIN_VALUE..0 -> {// decrement mock requests, to a limit of 0
                        max(0, mockUses - usesRequest)
                    }
                    else -> usesRequest
                }
            }
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
