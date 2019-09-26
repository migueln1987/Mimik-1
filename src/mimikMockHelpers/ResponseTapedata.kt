package mimikMockHelpers

import helpers.isJSONValid
import helpers.toJson
import helpers.tryGetBody
import io.ktor.http.HttpStatusCode
import okhttp3.Headers
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
        // todo; fact check to make sure the header is a multiple of 2

        if (!hasHeaders)
            headers = Headers.of("Content-Type", "text/plain")
    }

    var code: Int? = null
    var protocol: Protocol? = null

    val replayResponse: okreplay.Response
        get() {
            val isJson = body.isJSONValid
            if (headers.values("Content-Type").isEmpty()) {
                headers = headers.newBuilder().add(
                    "Content-Type",
                    if (isJson) "application/json" else "text/plain"
                ).build()
            }

            return object : okreplay.Response {
                override fun code() = code ?: HttpStatusCode.OK.value
                override fun protocol() = protocol ?: Protocol.HTTP_1_1

                override fun getEncoding() = ""
                override fun getCharset() = Charset.forName("UTF-8")

                override fun headers() = headers
                override fun header(name: String) = headers[name]
                override fun getContentType() = headers["Content-Type"]

                override fun hasBody() = !body.isNullOrBlank()
                override fun body() = bodyAsText().toByteArray()
                override fun bodyAsText() = body ?: ""

                override fun newBuilder() = TODO()
                override fun toYaml() = TODO()
            }
        }
}
