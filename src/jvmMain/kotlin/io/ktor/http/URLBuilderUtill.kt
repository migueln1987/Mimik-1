package io.ktor.http

/**
 * Appends a path to the current root path
 */
fun URLBuilder.subPath(path: String) {
    if (!encodedPath.endsWith("/") && path.startsWith("/"))
        encodedPath += "/$path"
    encodedPath += path
}

fun URLBuilder.pathFromRoot(path: String) {
    if (!encodedPath.endsWith("/") && path.startsWith("/"))
        encodedPath += "/$path"
    encodedPath += path
}

val URLBuilder.currentPath: String
    get() = buildString {
        append(protocol.name).append("://")
            .append(authority)

        if (encodedPath.isNotBlank() && !encodedPath.startsWith("/"))
            append('/')

        append(encodedPath)

        if (!parameters.isEmpty() || trailingQuery)
            append("?")

        parameters.entries().flatMap { (key, value) ->
            if (value.isEmpty()) listOf(key to null) else value.map { key to it }
        }.formUrlEncodeTo(this)
    }
