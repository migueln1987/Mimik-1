package helpers.attractors

import helpers.anyTrue
import helpers.isTrue
import io.ktor.http.HttpStatusCode
import mimikMockHelpers.QueryResponse
import mimikMockHelpers.RequestTapedata
import okhttp3.internal.http.HttpMethod

class RequestAttractors {
    var routingPath: RequestAttractorBit? = null
    var queryParamMatchers: List<RequestAttractorBit>? = null
    var queryHeaderMatchers: List<RequestAttractorBit>? = null
    var queryBodyMatchers: List<RequestAttractorBit>? = null

    constructor(config: (RequestAttractors) -> Unit = {}) {
        config.invoke(this)
    }

    constructor(request: RequestTapedata?) {
        request?.httpUrl?.also { url ->
            routingPath = RequestAttractorBit(url.encodedPath().removePrefix("/"))
            if (routingPath?.value?.isEmpty().isTrue())
                routingPath = null

            queryParamMatchers = url.queryParameterNames().flatMap { key ->
                url.queryParameterValues(key).map { value ->
                    RequestAttractorBit("$key=$value")
                }
            }
            if (queryParamMatchers?.isEmpty().isTrue())
                queryParamMatchers = null
        }

        // If the call always has a body, but a matcher wasn't set
        // then add a compliance matcher
        if (HttpMethod.requiresRequestBody(request?.method))
            queryBodyMatchers = listOf(RequestAttractorBit(".*"))
    }

    companion object {
        /**
         * Returns the best match based on the given criteria from the [source] map
         *
         * @return
         * - HttpStatusCode.Found (302) = item
         * - HttpStatusCode.NotFound (404) = item
         * - HttpStatusCode.Conflict (409) = null item
         */
        fun <T> findBest(
            source: Map<T, RequestAttractors?>,
            path: String?,
            params: String?,
            body: String? = null,
            custom: (T) -> AttractorMatches = { AttractorMatches() }
        ): QueryResponse<T> {
            if (source.isEmpty()) return QueryResponse {
                status = HttpStatusCode.NotFound
            }

            // Filter to early fail checks
            val options = source.mapValues { it.value to AttractorMatches() }
                .asSequence()
                .filter {
                    it.value.second.appendValues(custom.invoke(it.key))
                    it.value.second.matchingRequired
                }
                .filter {
                    it.value.second.appendValues(it.value.first?.matchesPath(path))
                    it.value.second.matchingRequired
                }
                .filter {
                    it.value.second.appendValues(it.value.first?.getParamMatches(params))
                    it.value.second.matchingRequired
                }
//                .filter {
//                    it.value.second.appendValues(it.value.first?.getHeaderMatches(params))
//                    it.value.second.matchingRequired
//                }
                .filter {
                    it.value.second.appendValues(it.value.first?.getBodyMatches(body))
                    it.value.second.matchingRequired
                }
                .filter { it.value.second.satisfiesRequired }
                .associate { it.key to it.value.second }

            val response = QueryResponse<T> {
                status = HttpStatusCode.NotFound
            }

            if (options.isEmpty()) return response

            val bestMatch = MatchFilter.findBestMatch(options)
            if (bestMatch == null)
                response.status = HttpStatusCode.Conflict
            else {
                response.item = options
                    .filter { it.value.toString() == bestMatch.toString() }.keys.first()
                response.status = HttpStatusCode.Found
            }

            return response
        }
    }

    val hasData: Boolean
        get() {
            return anyTrue(
                routingPath?.hardValue?.isNotBlank().isTrue(),
                queryParamMatchers?.isNotEmpty().isTrue(),
                queryHeaderMatchers?.isNotEmpty().isTrue(),
                queryBodyMatchers?.isNotEmpty().isTrue()
            )
        }

    fun append(data: RequestAttractors?) {
        if (data == null) return

        if (routingPath == null && data.routingPath != null)
            routingPath = data.routingPath?.clone()

        queryParamMatchers = matchAppender {
            from = data.queryParamMatchers
            to = queryParamMatchers
        }

        queryHeaderMatchers = matchAppender {
            from = data.queryHeaderMatchers
            to = queryHeaderMatchers
        }

        queryBodyMatchers = matchAppender {
            from = data.queryBodyMatchers
            to = queryBodyMatchers
        }
    }

    data class matcherPair(
        var from: List<RequestAttractorBit>? = null,
        var to: List<RequestAttractorBit>? = null
    )

    private fun matchAppender(matchers: matcherPair.() -> Unit): List<RequestAttractorBit>? {
        matcherPair().apply {
            matchers.invoke(this)

            from?.also { newData ->
                to = (to ?: listOf())
                    .toMutableList()
                    .apply {
                        addAll(newData.filterNot { contains(it) })
                    }
            }

            return to
        }
    }

    /**
     * Using the provided [matchScanner], the source list will be scanned for required and optional matches
     *
     * If there is no matchers, and there is data, assume the user doesn't want this to match
     */
    fun getMatchCount(
        matchScanner: List<RequestAttractorBit>?,
        source: String?
    ): AttractorMatches {
        if (matchScanner == null || matchScanner.isEmpty())
            return AttractorMatches().also {
                if (!source.isNullOrEmpty()) it.Required = 1
            }

        // nothing can possibly match, so give up here
        if (matchScanner.all { it.hardValue.isBlank() })
            return AttractorMatches()

        val reqCount = matchScanner.count { it.required }
        if (source == null) // hard fail if source is null
            return AttractorMatches(reqCount, -1, -1)

        val (required, reqRatio) = matchScanner.asSequence()
            .filter { it.required }
            .fold(0 to 0.0) { acc, x ->
                val match = x.regex.find(source)
                var matchVal = 0
                var matchRto = 0.0

                if (match.hasMatch) {
                    if (x.except.isTrue().not()) {
                        matchVal = 1
                        matchRto =
                            (if (match.hasMatch) x.regex.pattern.length else 0) /
                                    source.length.toDouble()
                    }
                } else {
                    if (x.except.isTrue()) {
                        matchVal = 1
                        matchRto = 1.0
                    }
                }

                (acc.first + matchVal) to (acc.second + matchRto)
            }

        val (optional, optRatio) = matchScanner.asSequence()
            .filter { it.optional.isTrue() }
            .fold(0 to 0.0) { acc, x ->
                val match = x.regex.find(source)
                val matchRto = (if (match.hasMatch) x.regex.pattern.length else 0) /
                        source.length.toDouble()

                (acc.first + (if (match.hasMatch) 1 else 0)) to
                        (acc.second + matchRto)
            }

        return AttractorMatches(reqCount, required, optional).also {
            it.reqRatio = reqRatio
            it.optRatio = optRatio
        }
    }

    /**
     * Returns true if this [MatchResult] contains any matching groups
     */
    private val MatchResult?.hasMatch: Boolean
        get() = this?.groups?.isNotEmpty().isTrue()

    private fun matchesPath(source: String?) =
        getMatchCount(routingPath?.let {
            it.required = true
            listOf(it)
        }, source)

    private fun getParamMatches(source: String?) =
        getMatchCount(queryParamMatchers, source)

    private fun getHeaderMatches(source: String?) =
        getMatchCount(queryHeaderMatchers, source)

    private fun getBodyMatches(source: String?) =
        getMatchCount(queryBodyMatchers, source)
}
