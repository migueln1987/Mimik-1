package okhttp3

import io.ktor.http.*

/**
 * Returns the [Headers] or default headers containing {ContentType: "* / *"}
 */
val Headers?.orDefault: Headers
    get() = this ?: Headers.headersOf(HttpHeaders.ContentType, ContentType.Any.toString())
