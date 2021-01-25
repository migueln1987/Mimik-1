package io.ktor.response

import io.ktor.http.*

/**
 * Append HTTP response header
 * @param safeOnly `true` by default, prevents from setting unsafe headers
 */
fun ResponseHeaders.append(name: String, value: ContentType, safeOnly: Boolean = true) =
    this.append(name, value.toString(), safeOnly)
