package com.fiserv.ktmimic.tapeTypes.helpers

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

    lateinit var method: String
    lateinit var url: HttpUrl

    val replayRequest: Request
        get() {
            return object : Request {
                override fun url() = url
                override fun method() = method

                override fun getEncoding() = ""
                override fun getCharset() = Charset.forName("UTF-8")

                override fun headers() = headers
                override fun header(name: String) = headers[name]
                override fun getContentType() = headers["Content-Type"]

                override fun hasBody() = body.isNotEmpty()
                override fun body() = body.toByteArray()
                override fun bodyAsText() = body

                override fun newBuilder() = TODO()
                override fun toYaml() = TODO()
            }
        }
}
