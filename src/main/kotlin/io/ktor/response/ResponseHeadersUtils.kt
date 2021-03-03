package io.ktor.response

import io.ktor.http.*
import kotlinUtils.isThrow

/**
 * Append HTTP response header
 * @param safeOnly `true` by default, prevents from setting unsafe headers
 */
fun ResponseHeaders.append(name: String, value: ContentType, safeOnly: Boolean = true) =
    this.append(name, value.toString(), safeOnly)

/**
 * Appends the data from [headers] to this [ResponseHeaders]
 */
fun ResponseHeaders.append(headers: okhttp3.Headers) =
    append(headers.toMultimap())

/**
 * Appends the data from [headers] to this [ResponseHeaders]
 */
fun ResponseHeaders.append(headers: Map<String, List<String>>) {
    headers.forEach { (t, u) ->
        u.forEach { isThrow { append(t, it) } }
    }
}

fun ResponseHeaders.append(vararg values: Pair<String, String>) =
    values.forEach { isThrow { append(it.first, it.second) } }

fun ResponseHeaders.append(key: String, vararg values: String) =
    values.forEach { isThrow { append(key, it) } }

fun ResponseHeaders.append(key: String, values: List<String>) =
    isThrow { values.forEach { isThrow { append(key, it) } } }
