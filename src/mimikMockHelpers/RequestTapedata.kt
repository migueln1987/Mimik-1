package mimikMockHelpers

import helpers.toJson
import helpers.tryGetBody
import okhttp3.HttpUrl
import java.nio.charset.Charset

class RequestTapedata : Tapedata {

    constructor(request: okreplay.Request) {
        method = request.method()
        url = request.url()
        headers = request.headers()
        body = request.tryGetBody()
    }

    constructor(builder: (RequestTapedata) -> Unit = {}) {
        builder.invoke(this)
    }

    fun clone(postClone: (RequestTapedata) -> Unit) = RequestTapedata {
        it.method = method
        it.url = HttpUrl.get(url.toString())
        it.headers = headers.newBuilder().build()
        it.body = body
    }.also { postClone.invoke(it) }

    var method: String? = null
    var url: HttpUrl? = null

    val replayRequest: okreplay.Request
        get() {
            return object : okreplay.Request {
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
