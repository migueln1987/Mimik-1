package okhttp3

import io.ktor.http.*

/**
 * Attempts to retrieve the Content-Type from the [Headers], or results `null`
 */
val Headers.contentType: ContentType?
    get() {
        val contentString = this[HttpHeaders.ContentType]
        if (contentString.isNullOrBlank())
            return null
        return ContentType.parse(contentString)
    }

/**
 * Returns [ContentType] or [ContentType.Any] when null
 */
val ContentType?.orDefault get() = this ?: ContentType.Any
