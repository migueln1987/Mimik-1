package com.fiserv.mimik.mimikMockHelpers

import com.fiserv.mimik.toJson
import io.ktor.http.HttpStatusCode
import okhttp3.Protocol
import okreplay.Response
import java.nio.charset.Charset

class ResponseTapedata : Tapedata {

    constructor()
    constructor(response: Response) {
        code = response.code()
        protocol = response.protocol()
        headers = response.headers()
        body = response.toJson()
    }

    constructor(builder: (ResponseTapedata) -> Unit) {
        builder.invoke(this)
    }

    var code: Int? = null
    var protocol: Protocol? = null

    val replayResponse: Response
        get() {
            return object : Response {
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
