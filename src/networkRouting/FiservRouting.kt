package com.fiserv.mimik.networkRouting

import com.fiserv.mimik.TapeCatalog
import com.fiserv.mimik.tapeTypes.helpers.RecordedInteractions
import com.fiserv.mimik.tapeTypes.helpers.mockChapterName
import com.fiserv.mimik.tapeTypes.helpers.toChain
import com.fiserv.mimik.toJson
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.post
import io.ktor.routing.put
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.Protocol
import okio.Buffer
import java.nio.charset.Charset
import kotlin.math.max

class FiservRouting(private val routing: Routing) {

    private val tapeCatalog = TapeCatalog.Instance

    /**
     * Networking call for all POSTs to "/fiserver/cbes/perform.do"
     */
    fun perform() {

        routing.post("/fiserver/cbes/perform.do") {
            val response = tapeCatalog.processCall(call) {
                call.request.queryParameters["opId"] ?: ""
            }

            val contentType = response.header("content-type") ?: "text/plain"
            val code = HttpStatusCode.fromValue(response.code())

            call.respondText(ContentType.parse(contentType), code) {
                withContext(Dispatchers.IO) {
                    response.toJson()
                }
            }
        }
    }

    fun importResponse(path: String) {
        routing.apply {
            put(path) {
                val callChain = call.toChain()
                val opId = call.request.queryParameters["opId"] ?: ""
                val tape = tapeCatalog.tapeCalls[opId]
                    ?: return@put call.respond(HttpStatusCode.FailedDependency, "")

                val mockResponse = RecordedInteractions(
                    callChain.request().toRequest,
                    callChain.request().toMockResponse
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
                            savedResponse.mockUses = max(0, savedResponse.mockUses - mockUseRequests)
                        }
                    }

                call.respondText(ContentType.parse("text/plain"), HttpStatusCode.OK) {
                    savedResponse.bodyKey
                }
            }
        }
    }

    private val okhttp3.Request.toRequest: okreplay.Request
        get() = object : okreplay.Request {
            override fun url() = this@toRequest.url()

            override fun method() = this@toRequest.method()

            override fun body(): ByteArray {
                return this@toRequest.body()?.let {
                    val buffer = Buffer()
                    it.writeTo(buffer)
                    buffer.readByteArray()
                } ?: byteArrayOf()
            }

            override fun hasBody() = body().isNotEmpty()

            override fun bodyAsText() = this@toRequest.body().toString()

            override fun getContentType(): String =
                this@toRequest.body()?.contentType().toString() ?: ""

            override fun headers() = this@toRequest.headers()

            override fun getEncoding(): String = TODO("not implemented")

            override fun getCharset() = Charset.defaultCharset()

            override fun header(name: String) = headers().get(name)

            override fun newBuilder() = TODO()
            override fun toYaml() = TODO()
        }

    private val okhttp3.Request.toMockResponse: okreplay.Response
        get() = toRequest.let { request ->
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
