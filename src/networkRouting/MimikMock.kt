package com.fiserv.mimik.networkRouting

import com.fiserv.mimik.TapeCatalog
import com.fiserv.mimik.helpers.toReplayRequest
import com.fiserv.mimik.mimikMockHelpers.RecordedInteractions
import com.fiserv.mimik.tapeTypes.helpers.mockChapterName
import com.fiserv.mimik.tapeTypes.helpers.toChain
import com.google.gson.internal.LinkedTreeMap
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.Routing
import io.ktor.routing.put
import okhttp3.Headers
import okhttp3.Protocol
import kotlin.math.max

class MimikMock(path: String) : RoutingContract(path) {

    private val tapeCatalog = TapeCatalog.Instance

    private enum class RoutePaths(val value: String) {
        MOCK("");

        val path: String
            get() = "$selfPath/$value"
    }

    override fun init(route: Routing) {
        route.apply {
            mock
        }
    }

    var additionalInfo: Any? = null

    val additionalInfoAsMap: LinkedTreeMap<String, Any?>
        get() {
            val additionalHolding = additionalInfo
            @Suppress("UNCHECKED_CAST")
            if (additionalHolding is LinkedTreeMap<*, *> &&
                additionalHolding.keys.all { it is String }
            )
                return additionalHolding as LinkedTreeMap<String, Any?>
            return LinkedTreeMap()
        }

    private val Routing.mock: Route
        get() = apply {
            put(RoutePaths.MOCK.value) {
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
            }
        }

    // todo; update to use BlankTapes??
    private fun Routing.importResponse(path: String) {
        apply {
            put(path) {
                val callChain = call.toChain()
                val opId = call.request.queryParameters["opId"] ?: ""
                val tape = tapeCatalog.tapeCalls[opId]
                    ?: return@put call.respond(HttpStatusCode.FailedDependency, "")

                val callChainRequest = callChain.request()
                val mockResponse = RecordedInteractions(
                    callChainRequest.toReplayRequest,
                    callChainRequest.toMockResponse
                )

                val mockMethod = call.request.header("mockMethod")
                if (mockMethod != null) {
                    mockResponse.chapterName = mockResponse.request
                        .mockChapterName(mockMethod, mockResponse.bodyKey)
                }

                val savedResponse = tape.requestMockResponses
                    .firstOrNull {
                        // try using the existing previous requested response
                        it.chapterName == mockResponse.chapterName
                    }
                    ?: let {
                        // or save the new response, and return this instance
                        tape.requestMockResponses.add(mockResponse)
                        mockResponse
                    }

                if (!call.request.header("mockUse").isNullOrBlank())
                    savedResponse.mockUses = 1 // set as "1-time use"

                if (call.request.header("mockUses")?.isNotEmpty() == true)
                    when (val mockUseRequests = call.request.header("mockUses")?.toIntOrNull()) {
                        null, 0 -> { // reset usage count
                            savedResponse.mockUses = 0
                        }
                        in 1..Int.MAX_VALUE -> { // increment by request
                            savedResponse.mockUses += mockUseRequests
                        }
                        in Int.MIN_VALUE..0 -> { // decrement mock requests, to a limit of 0
                            savedResponse.mockUses =
                                max(0, savedResponse.mockUses - mockUseRequests)
                        }
                    }

                call.respondText(ContentType.parse("text/plain"), HttpStatusCode.OK) {
                    savedResponse.bodyKey
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
