package mimikMockHelpers

import helpers.isTrue
import io.ktor.http.HttpHeaders
import okhttp3.Headers

abstract class Networkdata {
    var headers: Headers? = null
    val tapeHeaders: Headers
        get() {
            return headers?.let {
                if (it.size() < 1) null else it
            } ?: Headers.of(HttpHeaders.ContentType, "text/plain")
        }

    var body: String? = null

    /**
     * Returns if this object is an image (based on the header Content-Type)
     */
    val isImage: Boolean
        get() = tapeHeaders.get(HttpHeaders.ContentType)?.contains("image").isTrue()
}
