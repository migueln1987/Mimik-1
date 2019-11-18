package mimikMockHelpers

import helpers.isValidJSON
import helpers.tryGetBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import okhttp3.Protocol
import java.nio.charset.Charset
import java.util.Date

class Responsedata : Networkdata {
    constructor(response: okreplay.Response) {
        code = response.code()
        protocol = response.protocol()
        headers = response.headers()
        body = response.tryGetBody()
    }

    constructor(builder: (Responsedata) -> Unit = {}) {
        builder.invoke(this)
    }

    fun clone(postClone: (Responsedata) -> Unit = {}) = Responsedata {
        it.code = code
        it.protocol = protocol
        it.headers = tapeHeaders.newBuilder().build()
        it.body = body
    }.also { postClone.invoke(it) }

    override fun toString(): String {
        return "%s".format(code)
    }

    @Suppress("unused")
    @Transient
    val recordedDate: Date? = Date()

    var code: Int? = null
        get() = field ?: HttpStatusCode.OK.value
    var protocol: Protocol? = null
        get() = field ?: Protocol.HTTP_1_1

    val replayResponse: okreplay.Response
        get() {
            val isJson = body.isValidJSON
            headers = tapeHeaders.newBuilder().set(
                HttpHeaders.ContentType,
                if (isJson) "application/json" else (tapeHeaders[HttpHeaders.ContentType] ?: "text/plain")
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
                override fun bodyAsText() = body.orEmpty()

                override fun newBuilder() = TODO()
                override fun toYaml() = TODO()
            }
        }
}
