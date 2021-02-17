package okreplay

import okhttp3.Headers
import okhttp3.Protocol
import java.nio.charset.Charset

class ResponseEditor(input: Response) {
    var code: Int = input.code()
    var protocol: Protocol = input.protocol()
    var encoding: String = input.encoding
    var charset: Charset = input.charset
    var headers: Headers = input.headers()
    var contentType: String = input.contentType
    var body: ByteArray = input.body()

    fun toResponse(): Response {
        return object : Response {
            override fun code() = code
            override fun protocol() = protocol

            override fun getEncoding() = this@ResponseEditor.encoding
            override fun getCharset() = this@ResponseEditor.charset

            override fun headers() = this@ResponseEditor.headers
            override fun header(name: String) = this@ResponseEditor.headers[name]
            override fun getContentType() = this@ResponseEditor.contentType

            override fun hasBody() = this@ResponseEditor.body.isNotEmpty()
            override fun body() = this@ResponseEditor.body
            override fun bodyAsText() = this@ResponseEditor.body.toString()

            override fun newBuilder() = TODO()
            override fun toYaml() = TODO()
        }
    }
}
