package okhttp3

import io.ktor.http.*
import kotlinx.isTrue
import okhttp3.Headers.Companion.headersOf

abstract class NetworkData {
    var headers: Headers? = null

    @Deprecated(message = "use Headers", level = DeprecationLevel.ERROR)
    val tapeHeaders: Headers
        get() {
            return headers?.let {
                if (it.size < 1) null else it
            } ?: headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
        }

    var body: String? = null

    /**
     * Returns if this object is an image (based on the header Content-Type)
     */
    val isImage: Boolean
        get() = headers.orDefault[HttpHeaders.ContentType]?.contains("image").isTrue
}
