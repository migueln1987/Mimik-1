package okhttp3

/**
 * Removes all the current query parameters from [HttpUrl], and applies the items from [newQueries].
 *
 * [append]: When true, items are appended instead of clearing before [newQueries] is added.
 */
fun HttpUrl?.reQuery(newQueries: Sequence<Pair<String, Any?>>?, append: Boolean = false): HttpUrl? {
    if (this == null) return null
    return newBuilder().also { builder ->
        if (!append)
            queryParameterNames.forEach {
                builder.removeAllQueryParameters(it)
            }
        newQueries?.forEach {
            builder.addQueryParameter(it.first, it.second.toString())
        }
    }.build()
}
