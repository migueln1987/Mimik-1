package com.fiserv.mimik.tapeItems

import okreplay.Request

class RequestAttractors(
    // "url/sub/path"
    var routingPath: String? = null,
    // "Key1=Val1&Key2=Val2" -> "[ (Key1, Val1), (Key2, Val2) ]
    var queryParams: Array<Pair<String, String>>? = null,
    var queryBody: String? = null
) {
    constructor(config: RequestAttractors.() -> Unit) : this() {
        config.invoke(this)
    }

    fun matchesRequest(request: Request): Boolean {
        val test_1 = request.url().url().path == routingPath
        val test_2 = request.url().encodedPath() == routingPath

        val url = request.url().url()

        val matchPath = routingPath?.run { url.path == this } ?: true

        val matchQuery = queryParams?.run {
            val reqParams = url.query
                .split("&").sorted()
                .map { KVMap ->
                    KVMap.split("=").run { get(0) to get(1) }
                }

            all { reqParams.contains(it) }
        } ?: true

        val matchBody = queryBody?.run {
            toRegex().containsMatchIn(request.body().toString())
        } ?: true

        return matchPath && matchQuery && matchBody
    }
}
