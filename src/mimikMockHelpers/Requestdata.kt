package mimikMockHelpers

import helpers.asHttpUrl
import helpers.attractors.RequestAttractors
import helpers.tryGetBody
import io.ktor.http.HttpHeaders
import okhttp3.HttpUrl
import okhttp3.internal.http.HttpMethod
import java.nio.charset.Charset

class Requestdata : Networkdata {

    constructor(request: okreplay.Request) {
        method = request.method()
        url = request.url().toString()
        headers = request.headers()
        body = request.tryGetBody()
    }

    constructor(builder: (Requestdata) -> Unit = {}) {
        builder.invoke(this)

        if (method != null && HttpMethod.requiresRequestBody(method) && body == null)
            body = ""
    }

    fun clone(postClone: (Requestdata) -> Unit = {}) = Requestdata {
        it.method = method
        it.url = url.toString()
        it.headers = tapeHeaders.newBuilder().build()
        it.body = body
    }.also { postClone.invoke(it) }

    override fun toString() =
        "%s: %s".format(method, url)

    var method: String? = null
        get() = field?.toUpperCase()

    /**
     * Full URL
     *
     * Example: http://url.ext/sub/path?Key1=Value1&Key2=Value2
     */
    var url: String? = ""
    val httpUrl: HttpUrl?
        get() = url.asHttpUrl

    val replayRequest: okreplay.Request
        get() = object : okreplay.Request {
            override fun url() = httpUrl ?: HttpUrl.get("http://invalid.com")
            override fun method() = method ?: "GET"

            override fun getEncoding() = ""
            override fun getCharset() = Charset.forName("UTF-8")

            override fun headers() = tapeHeaders
            override fun header(name: String) = tapeHeaders[name]
            override fun getContentType() = tapeHeaders[HttpHeaders.ContentType]

            override fun hasBody() = !body.isNullOrBlank()
            override fun body() = bodyAsText().toByteArray()
            override fun bodyAsText() = body.orEmpty()

            override fun newBuilder() = TODO()
            override fun toYaml() = TODO()
        }

    /**
     * Creates [RequestAttractors] from this [Requestdata]
     */
    val toAttractors: RequestAttractors
        get() = RequestAttractors(this)
}
