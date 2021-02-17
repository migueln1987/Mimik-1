package okhttp3

import io.ktor.http.*
import kotlinUtils.ensureHttpPrefix
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Returns this [HttpUrl] as the host and path in url form.
 *
 * Example: http://url.ext
 */
val HttpUrl.hostPath: String
    get() = "%s://%s%s".format(
        scheme,
        host,
        encodedPath
    )

/**
 * Replaces the scheme and host in the input [HttpUrl] with the new value in newHost.
 *
 * newHost can be in the format of "url.ext" or "http://url.ext"
 */
fun HttpUrl?.reHost(newHost: String): HttpUrl? {
    val newHttpHost = newHost.ensureHttpPrefix.toHttpUrlOrNull()

    if (this == null || newHttpHost == null) return newHttpHost
    return newBuilder().also {
        it.scheme(newHttpHost.scheme)
        it.host(newHttpHost.host)
    }.build()
}

/**
 * Replaces/ appends [newPort] to this [HttpUrl]
 */
fun HttpUrl?.rePort(newPort: Int): HttpUrl? {
    return this?.let {
        newBuilder().also {
            it.port(newPort)
        }.build()
    }
}

/**
 * Returns a list of this [HttpUrl]'s query items in a "item=value" format
 */
fun HttpUrl?.queryItems(): List<String> {
    return if (this == null) listOf()
    else queryParameterNames.flatMap { name ->
        queryParameterValues(name).map { value -> "$name=$value" }
    }
}

/**
 * Returns the [HttpUrl] as a [Parameters] object.
 *
 * Keys with no values are returned as keys with empty strings
 */
val HttpUrl?.toParameters: Parameters?
    get() {
        if (this == null) return null
        val pairs = this.queryParameterNames.asSequence()
            .filterNotNull().flatMap { name ->
                if (name.isBlank()) return@flatMap emptySequence<Pair<String, String>>()
                this.queryParameterValues(name).asSequence()
                    .map { name to it.orEmpty() }
            }

        return Parameters.build {
            pairs.forEach { append(it.first, it.second) }
        }
    }

fun HttpUrl.containsPath(vararg path: String) =
    pathSegments.containsAll(path.toList())
