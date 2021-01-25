package mimikMockHelpers

import helpers.isNotNull
import helpers.isValidJSON
import helpers.tryGetBody
import io.ktor.http.*
import okhttp3.Protocol
import java.nio.charset.Charset
import java.util.Date

class Responsedata : NetworkData {
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

    @Transient
    var isLocalhostCall: Boolean? = false

    val replayResponse: okreplay.Response
        get() {
            val isJson = body.isValidJSON
            val inputContentType = tapeHeaders[HttpHeaders.ContentType]
            headers = tapeHeaders.newBuilder().set(
                HttpHeaders.ContentType,
                when {
                    isJson -> ContentType.Application.Json.toString()
                    inputContentType != null -> inputContentType
                    else -> ContentType.Text.Plain.toString()
                }
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
