package okhttp3

import io.ktor.http.*
import kotlinUtils.isValidJSON
import okreplay.tryGetBody
import java.nio.charset.Charset
import java.util.Date

class ResponseData : NetworkData {
    constructor(response: okreplay.Response) {
        code = response.code()
        protocol = response.protocol()
        headers = response.headers()
        body = response.tryGetBody()
    }

    constructor(builder: (ResponseData) -> Unit = {}) {
        builder.invoke(this)
    }

    fun clone(postClone: (ResponseData) -> Unit = {}) = ResponseData {
        it.code = code
        it.protocol = protocol
        it.headers = headers.orDefault.newBuilder().build()
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

    @Transient
    var isLocalhostCall: Boolean? = false

    val replayResponse: okreplay.Response
        get() {
            val isJson = body.isValidJSON
            val inputContentType = headers?.contentType
            headers = headers.orDefault.newBuilder().set(
                HttpHeaders.ContentType,
                when {
                    isJson -> ContentType.Application.Json.toString()
                    inputContentType != null -> inputContentType.toString()
                    else -> ContentType.Text.Plain.toString()
                }
            ).build()

            return object : okreplay.Response {
                override fun code() = code ?: HttpStatusCode.OK.value
                override fun protocol() = protocol ?: Protocol.HTTP_1_1

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
}
