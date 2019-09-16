package tapeItems

import helpers.isTrue
import okreplay.Request

class RequestAttractors(
    // "url/sub/path"
    var routingPath: String? = null,
    // "Key1=Val1&Key2=Val2" -> "[ (Key1, Val1), (Key2, Val2) ]
    var queryParams: List<Pair<String, String>>? = null,
    // regex match
    var queryBody: String? = null
) {
    constructor(config: RequestAttractors.() -> Unit) : this() {
        config.invoke(this)
    }

    fun matchesRequest(request: Request): Boolean {
        val url = request.url().url()

        val matchPath = matchesPath(url.path)

        val matchQuery = matchesQuery(url.query)

        val matchBody = queryBody?.run {
            toRegex().containsMatchIn(request.body().toString())
        } ?: true

        return matchPath && matchQuery && matchBody
    }

    fun matchesPath(path: String?) =
        routingPath?.run { path == this } ?: true

    fun matchesQuery(query: String?): Boolean {
        return queryParams?.run {
            val reqParams = query
                ?.split("&")?.sorted()
                ?.map { it.split("=").run { get(0) to get(1) } }

            all { reqParams?.contains(it).isTrue() }
        } ?: true
    }
}
