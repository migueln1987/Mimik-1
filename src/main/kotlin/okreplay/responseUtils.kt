package okreplay

import kotlinUtils.isBase64
import okhttp3.ResponseData
import javax.xml.bind.DatatypeConverter

/**
 * Returns the body, if any, or [default] when null
 */
fun Response.tryGetBody(default: String = ""): String {
    return if (hasBody()) {
        val data = body()
        val dataStr = String(data)
        val isBase64 = dataStr.isBase64

        if (!isBase64 && (contentType.orEmpty()).startsWith("image"))
            DatatypeConverter.printBase64Binary(data)
        else
            dataStr
    } else default
}

/**
 * Converts the [okreplay.Response] to [ResponseData]
 */
val Response.toTapeData: ResponseData
    get() = ResponseData(this)

fun Response.edit(action: (ResponseEditor) -> Unit): ResponseEditor =
    ResponseEditor(this).also(action)
