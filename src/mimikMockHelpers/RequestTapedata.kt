package com.fiserv.mimik.mimikMockHelpers

import com.fiserv.mimik.toJson
import okhttp3.HttpUrl
import okreplay.Request
import java.nio.charset.Charset

class RequestTapedata : Tapedata {

    constructor()
    constructor(request: Request) {
        method = request.method()
        url = request.url()
        headers = request.headers()
        body = request.toJson()
    }

    constructor(builder: (RequestTapedata) -> Unit) {
        builder.invoke(this)
    }

    var method: String? = null
    var url: HttpUrl? = null

    val replayRequest: Request
        get() {
            return object : Request {
                override fun url() = url ?: HttpUrl.parse("http://invalid.com")
                override fun method() = method ?: "GET"

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
