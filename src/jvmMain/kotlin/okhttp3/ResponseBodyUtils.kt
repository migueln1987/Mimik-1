package okhttp3

import kotlinx.isBase64
import kotlinx.tryOrNull
import okhttp3.ResponseBody.Companion.asResponseBody
import javax.xml.bind.DatatypeConverter

/**
 * Returns the [ResponseBody]'s body as a string.
 *
 * Images are converted to Base64 (if not already)
 *
 * !! This action clears the original data's body !!
 */
fun ResponseBody?.contents(default: String = ""): String {
    return if (this == null) default
    else tryOrNull {
        val data = tryOrNull { use { bytes() } } ?: return ""
        val dataStr = String(data)
        val isBase64 = dataStr.isBase64

        if (!isBase64 && contentType()?.type == "image")
            DatatypeConverter.printBase64Binary(data)
        else
            dataStr
    } ?: default
}

fun ResponseBody?.clone(): ResponseBody? {
    return tryOrNull {
        this?.source()?.use { source ->
            source.request(Long.MAX_VALUE)
            source.buffer.clone().asResponseBody(contentType(), contentLength())
        }
    }
}
