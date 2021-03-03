package okhttp3

import kotlinUtils.asHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.internal.http.HttpMethod
import okreplay.content
import java.nio.charset.Charset

/**
 * Open version of [Request] which has writable fields for:
 * - method
 * - headers
 * - body
 */
class RequestData : NetworkData {

    constructor(request: okreplay.Request) {
        method = request.method()
        url = request.url().toString()
        headers = request.headers()
        body = request.content()
    }

    constructor(builder: (RequestData) -> Unit = {}) {
        builder(this)
        method?.also { method ->
            if (HttpMethod.requiresRequestBody(method) && body == null)
                body = ""
        }
    }

    fun clone(postClone: (RequestData) -> Unit = {}) = RequestData {
        it.method = method
        it.url = url.toString()
        it.headers = headers.orDefault.newBuilder().build()
        it.body = body
    }.also { postClone(it) }

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
            override fun url() = httpUrl ?: "http://invalid.com".toHttpUrl()
            override fun method() = method ?: io.ktor.http.HttpMethod.Get.value

            override fun getEncoding() = ""
            override fun getCharset() = Charset.forName("UTF-8")

            override fun headers() = headers.orDefault
            override fun header(name: String) = headers()[name]
            override fun getContentType() = headers().contentType.orDefault.toString()

            override fun hasBody() = !body.isNullOrBlank()
            override fun body() = bodyAsText().toByteArray()
            override fun bodyAsText() = body.orEmpty()

            override fun newBuilder() = TODO()
            override fun toYaml() = TODO()
        }
}
