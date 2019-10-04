package mimikMockHelpers

import helpers.isJSONValid
import helpers.tryGetBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import okhttp3.Protocol
import java.nio.charset.Charset

class ResponseTapedata : Tapedata {
    constructor(response: okreplay.Response) {
        code = response.code()
        protocol = response.protocol()
        headers = response.headers()
        body = response.tryGetBody()
    }

    constructor(builder: (ResponseTapedata) -> Unit = {}) {
        builder.invoke(this)
    }

    override fun toString(): String {
        return "%s".format(code)
    }

    var code: Int? = null
        get() = field ?: HttpStatusCode.OK.value
    var protocol: Protocol? = null
        get() = field ?: Protocol.HTTP_1_1

    val replayResponse: okreplay.Response
        get() {
            val isJson = body.isJSONValid
            headers = tapeHeaders.newBuilder().set(
                HttpHeaders.ContentType,
                if (isJson) "application/json" else "text/plain"
            ).build()

            return object : okreplay.Response {
                override fun code() = code ?: HttpStatusCode.OK.value
                override fun protocol() = protocol ?: Protocol.HTTP_1_1

                override fun getEncoding() = ""
                override fun getCharset() = Charset.forName("UTF-8")

                override fun headers() = tapeHeaders
                override fun header(name: String) = tapeHeaders[name]
                override fun getContentType() = tapeHeaders[HttpHeaders.ContentType]

                override fun hasBody() = !body.isNullOrBlank()
                override fun body() = bodyAsText().toByteArray()
                override fun bodyAsText() = body ?: ""

                override fun newBuilder() = TODO()
                override fun toYaml() = TODO()
            }
        }
}
