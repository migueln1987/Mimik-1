package helpers.attractors

import helpers.anyTrue
import helpers.isNotTrue
import helpers.isTrue
import io.ktor.http.HttpStatusCode
import mimikMockHelpers.QueryResponse
import mimikMockHelpers.Requestdata
import okhttp3.internal.http.HttpMethod

class RequestAttractors {
    var routingPath: RequestAttractorBit? = null
    var queryParamMatchers: List<RequestAttractorBit>? = null
    var queryHeaderMatchers: List<RequestAttractorBit>? = null
    var queryBodyMatchers: List<RequestAttractorBit>? = null

    constructor(config: (RequestAttractors) -> Unit = {}) {
        config.invoke(this)
    }

    constructor(request: Requestdata?) {
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

    /**
     * Returns [true] if any of the data in this object has data
     */
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
     * - No [matchScanner]s and [source] is not null: returns failed match
     * - Any [matchScanner]'s allowAllInputs = true: ANY input (even none) passes the match
     * - all of [matchScanner]'s values are blank: skips this test
     * - [source] is null: fail all the non-optional (and non-except) tests
     */
    fun getMatchCount(
        matchScanner: List<RequestAttractorBit>?,
        source: String?
    ): AttractorMatches {
        if (matchScanner.isNullOrEmpty())
            return AttractorMatches().also {
                if (!source.isNullOrEmpty()) it.Required = 1
            }

        // match ANY input passed in, even an empty input
        if (matchScanner.any { it.allowAllInputs.isTrue() })
            return AttractorMatches(1, 1, 0)

        // nothing can possibly match, so give up here
        if (matchScanner.all { it.hardValue.isBlank() })
            return AttractorMatches()

        val reqCount = matchScanner.count { it.required && it.except.isNotTrue() }
        if (source == null) // hard fail if source is null
            return AttractorMatches(reqCount, -1, -1)

        val (required, reqRatio) = matchScanner.asSequence()
            .filter { it.required }
            .fold(0 to 0.0) { acc, x ->
                val result = x.matchResult(source)
                (acc.first + result.first) to (acc.second + result.second)
            }

        val (optional, optRatio) = matchScanner.asSequence()
            .filter { it.optional.isTrue() }
            .fold(0 to 0.0) { acc, x ->
                val result = x.matchResult(source)
                (acc.first + result.first) to (acc.second + result.second)
            }

        return AttractorMatches(reqCount, required, optional).also {
            it.reqRatio = reqRatio
            it.optRatio = optRatio
        }
    }

    fun RequestAttractorBit.matchResult(source: String): Pair<Int, Double> {
        val match = regex.find(source)
        var matchVal = 0
        var matchRto = 0.0

        if (match.hasMatch) {
            if (except.isNotTrue()) {
                matchVal = 1
                val matchLen = if (match.hasMatch) regex.pattern.length else 0
                matchRto = matchLen / source.length.toDouble()
            }
        } else {
            if (except.isTrue()) {
                matchVal = 1
                matchRto = 1.0
            }
        }

        return (matchVal to matchRto)
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
