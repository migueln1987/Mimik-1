package mimikMockHelpers

import io.ktor.http.HttpHeaders
import okhttp3.Headers

abstract class Tapedata {
    var headers: Headers? = null
    val tapeHeaders: Headers
        get() {
            return headers?.let {
                if (it.size() < 1) null else it
            } ?: Headers.of(HttpHeaders.ContentType, "text/plain")
        }

    var body: String? = null
}
