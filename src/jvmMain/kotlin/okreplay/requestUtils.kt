package okreplay

import kotlinx.isBase64
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestData
import okhttp3.internal.http.HttpMethod
import javax.xml.bind.DatatypeConverter

/**
 * Returns the body, if any (and required), or [default] when null.
 *
 * Images are converted to Base64
 */
fun Request.content(default: String = ""): String? {
    return when {
        !HttpMethod.requiresRequestBody(method()) -> null
        !hasBody() -> default
        else -> {
            val data = body()
            val dataStr = String(data)
            val isBase64 = dataStr.isBase64

            if (!isBase64 && (contentType.orEmpty()).startsWith("image"))
                DatatypeConverter.printBase64Binary(data)
            else
                dataStr
        }
    }
}

/**
 * Attempts to parse the [okreplay.Request] as a [RequestBody], otherwise [default] is returned.
 * Note: The process is skipped if the [okreplay.Request] method does not require a body
 */
fun Request.asRequestBody(default: String = ""): RequestBody? {
    return if (HttpMethod.requiresRequestBody(method())) {
        (content() ?: default).toRequestBody(contentType.toMediaTypeOrNull())
    } else null
}

/**
 * Converts the [okreplay.Request] to [okhttp3.Request]
 */
val Request.toOkRequest: okhttp3.Request
    get() {
        return okhttp3.Request.Builder().also { builder ->
            builder.url(url())
            builder.headers(headers())
            builder.method(method(), asRequestBody())
        }.build()
    }

/**
 * Converts the [okreplay.Request] to [RequestData]
 */
val Request.toTapeData: RequestData
    get() = RequestData(this)
