package com.fiserv.ktmimic.tapeTypes.helpers

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

    var code: Int = 0
    lateinit var protocol: Protocol

    val replayResponse: Response
        get() {
            return object : Response {
                override fun code() = code
                override fun protocol() = protocol

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
