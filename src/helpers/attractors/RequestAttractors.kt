package helpers.attractors

import helpers.anyTrue
import helpers.isNotTrue
import helpers.isTrue
import helpers.toStringPairs
import io.ktor.http.HttpStatusCode
import mimikMockHelpers.QueryResponse
import mimikMockHelpers.Requestdata
import okhttp3.internal.http.HttpMethod

class RequestAttractors {
    var routingPath: RequestAttractorBit? = null
    var queryMatchers: List<RequestAttractorBit>? = null
    var headerMatchers: List<RequestAttractorBit>? = null
    var bodyMatchers: List<RequestAttractorBit>? = null

    constructor(config: (RequestAttractors) -> Unit = {}) {
        config.invoke(this)
    }

    constructor(request: Requestdata?) {
        request?.httpUrl?.also { url ->
            routingPath = RequestAttractorBit(url.encodedPath().removePrefix("/"))
            if (routingPath?.value?.isBlank().isTrue())
                routingPath = null

            queryMatchers = url.queryParameterNames().flatMap { key ->
                url.queryParameterValues(key).map { value ->
                    RequestAttractorBit("$key=$value")
                }
            }
            if (queryMatchers?.isEmpty().isTrue())
                queryMatchers = null
        }

        request?.headers?.toStringPairs()?.also {
            headerMatchers = it.map { v ->
                RequestAttractorBit(v)
            }
        }

        // If the call always has a body, but a matcher wasn't set
        // then add a compliance matcher
        if (HttpMethod.requiresRequestBody(request?.method))
            bodyMatchers = listOf(RequestAttractorBit { it.allowAllInputs = true })
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
            queries: String?,
            headers: List<String>?,
            body: String? = null,
            custom: (T) -> AttractorMatches = { AttractorMatches() }
        ): QueryResponse<T> {
            if (source.isEmpty()) return QueryResponse {
                status = HttpStatusCode.NotFound
            }

            Fails.clear()
            // Filter to early fail checks
            val options = source.mapValues { it.value to AttractorMatches() }
                .asSequence()
                .filter { it.parseMatch { _ -> custom.invoke(it.key) } }
                .filter { it.parseMatch { it?.matchesPath(path) } }
                .filter { it.parseMatch { it?.getQueryMatches(queries) } }
                .filter { it.parseMatch { it?.getHeaderMatches(headers) } }
                .filter { it.parseMatch { it?.getBodyMatches(body) } }
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

        val Fails = mutableListOf<Pair<RequestAttractors?, AttractorMatches>>()

        private fun Map.Entry<*, Pair<RequestAttractors?, AttractorMatches>>.parseMatch(
            matcher: (RequestAttractors?) -> AttractorMatches?
        ): Boolean {
            val mResult = matcher.invoke(value.first)
            value.second.appendValues(mResult)
            if (!value.second.matchingRequired) {
                Fails.add(value)
            }
            return value.second.matchingRequired
        }
    }

    /**
     * Returns [true] if any of the data in this object has data
     */
    val hasData: Boolean
        get() {
            return anyTrue(
                routingPath?.hardValue?.isNotBlank().isTrue(),
                queryMatchers?.isNotEmpty().isTrue(),
                headerMatchers?.isNotEmpty().isTrue(),
                bodyMatchers?.isNotEmpty().isTrue()
            )
        }

    fun clone() = RequestAttractors {
        it.routingPath = routingPath?.clone()
        it.queryMatchers = queryMatchers?.map { it.clone() }
        it.headerMatchers = queryMatchers?.map { it.clone() }
        it.bodyMatchers = queryMatchers?.map { it.clone() }
    }

    /**
     * Appends [data] to this object's data. null [data] are ignored
     */
    fun append(data: RequestAttractors?) {
        if (data == null) return

        if (routingPath == null && data.routingPath != null)
            routingPath = data.routingPath?.clone()

        queryMatchers = matchAppender {
            from = data.queryMatchers
            to = queryMatchers
        }

        headerMatchers = matchAppender {
            from = data.headerMatchers
            to = headerMatchers
        }

        bodyMatchers = matchAppender {
            from = data.bodyMatchers
            to = bodyMatchers
        }
    }

    private data class MatcherPair(
        var from: List<RequestAttractorBit>? = null,
        var to: List<RequestAttractorBit>? = null
    )

    /**
     * Appends data from [matchers]'s [MatcherPair.from] to the contents of [MatcherPair.to],
     * then returns [MatcherPair.to].
     */
    private fun matchAppender(matchers: MatcherPair.() -> Unit): List<RequestAttractorBit>? {
        return MatcherPair().apply {
            matchers.invoke(this) // initialize matcherPair's data

            from?.also { newData ->
                to = (to ?: listOf())
                    .toMutableList()
                    .apply {
                        addAll(newData.filterNot { contains(it) })
                    }
            }
        }.to
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

        val reqCount = matchScanner.count { it.required }
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

    private fun RequestAttractorBit.matchResult(source: String): Pair<Int, Double> {
        val match = regex.find(source)
        val literalMatch = hardValue.isNotBlank() && (source == hardValue)
        var matchVal = 0
        var matchRto = 0.0

        if (literalMatch || match.hasMatch) {
            if (except.isNotTrue()) {
                matchVal = 1
                val matchLen = when {
                    literalMatch -> hardValue.length
                    match.hasMatch -> regex.pattern.length
                    else -> 0
                }
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

    private fun getQueryMatches(source: String?) =
        getMatchCount(queryMatchers, source)

    private fun getHeaderMatches(source: List<String>?): AttractorMatches {
        if (headerMatchers?.isNullOrEmpty().isTrue())
            return AttractorMatches().also {
                if (!source.isNullOrEmpty()) it.Required = 1
            }

        if (headerMatchers?.any { it.allowAllInputs.isTrue() }.isTrue())
            return AttractorMatches(1, 1, 0)

        if (headerMatchers?.all { it.hardValue.isBlank() }.isTrue())
            return AttractorMatches()

        return AttractorMatches().apply {
            source?.forEach {
                if (it == "host : local.host")
                    appendValues(AttractorMatches(1, 1, 0))
                else
                    appendValues(getMatchCount(headerMatchers, it))
            }
            Required = headerMatchers?.count { it.required } ?: 0
        }
    }

    private fun getBodyMatches(source: String?) =
        getMatchCount(bodyMatchers, source)
}
