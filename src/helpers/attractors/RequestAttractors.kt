package helpers.attractors

import helpers.anyTrue
import helpers.isFalse
import helpers.isTrue
import io.ktor.http.HttpStatusCode
import mimikMockHelpers.QueryResponse
import mimikMockHelpers.RequestTapedata

class RequestAttractors {
    // "url/sub/path"
    var routingPath: RequestAttractorBit? = null

    var queryParamMatchers: List<RequestAttractorBit>? = null
    // regex match of body items
    var queryBodyMatchers: List<RequestAttractorBit>? = null

    constructor(config: (RequestAttractors) -> Unit = {}) {
        config.invoke(this)
    }

    constructor(request: RequestTapedata) {
        request.url?.also { url ->
            routingPath = RequestAttractorBit(url.encodedPath())

            queryParamMatchers = url.queryParameterNames().flatMap { key ->
                url.queryParameterValues(key).map { value ->
                    RequestAttractorBit("$key=$value")
                }
            }
        }
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

            val options = source.mapValues {
                val response = AttractorMatches(0, 0, 0)
                var matchPath = AttractorMatches()
                var matchesParam = AttractorMatches()
                var matchesBody = AttractorMatches()
                val matchesCustom = custom.invoke(it.key)

                it.value?.also { attr ->
                    matchPath = attr.matchesPath(path)
                    matchesParam = attr.getParamMatches(params)
                    matchesBody = attr.getBodyMatches(body)
                }

                response.appendValues(matchPath)
                    .appendValues(matchesParam)
                    .appendValues(matchesBody)
                    .appendValues(matchesCustom)
            }.filter {
                it.value.satisfiesRequirement
            }

            val response = QueryResponse<T>() {
                status = HttpStatusCode.NotFound
            }

            if (options.isEmpty()) return response

            val groupByReq = options.values
                .groupBy({ it.MatchesReq }, { it })

            // highestReq; group containing the highest "Required" matchers
            val highestReq = groupByReq.keys.max() ?: -1
            if (highestReq > -1) { // there is some by "Required"
                when (groupByReq[highestReq]?.size ?: -1) { // matches in the group
                    // 0 -> invalid, even 0 "Required" would return a count size
                    1 -> { // we found a match!
                        val bestKey = groupByReq[highestReq]?.first()
                        response.item = options.filter { it.value == bestKey }.keys.first()
                        response.status = HttpStatusCode.Found
                    }

                    in (2..Int.MAX_VALUE) -> { // many "Required" matches, filter them by "Optional"
                        val reqOptionals = groupByReq[highestReq] ?: listOf()
                        val groupByOpt = options.values
                            .filter { reqOptionals.contains(it) }
                            .groupBy({ it.MatchesOpt }, { it })

                        // highestOpt; group containing the highest "Optional" matchers
                        val highestOpt = groupByOpt.keys.max() ?: -1
                        when (groupByOpt[highestOpt]?.size ?: -1) {
                            1 -> { // we found a match!
                                val bestKey = groupByOpt[highestOpt]?.first()
                                response.item =
                                    options.filter { it.value == bestKey }.keys.first()
                                response.status = HttpStatusCode.Found
                            }
                            in (0..Int.MAX_VALUE) -> {
                                // too many "Required" and "Optional", throw a Conflict error
                                response.status = HttpStatusCode.Conflict
                            }
                        }
                    }
                }
            }

            return response
        }
    }

    val hasData: Boolean
        get() {
            return anyTrue(
                routingPath?.value.isNullOrBlank().isFalse(),
                queryParamMatchers?.isNotEmpty().isTrue(),
                queryBodyMatchers?.isNotEmpty().isTrue()
            )
        }

    fun append(data: RequestAttractors?) {
        data?.queryParamMatchers?.also { newData ->
            queryParamMatchers = (queryParamMatchers ?: listOf())
                .toMutableList()
                .apply {
                    val filteredData = newData.filterNot { contains(it) }
                    addAll(filteredData)
                }
        }

        data?.queryBodyMatchers?.also { newData ->
            queryBodyMatchers = (queryBodyMatchers ?: listOf())
                .toMutableList()
                .apply {
                    val filteredData = newData.filterNot { contains(it) }
                    addAll(filteredData)
                }
        }
    }

    fun matchesPath(path: String?): AttractorMatches {
        return routingPath?.regex?.let { regex ->
            AttractorMatches(
                1,
                if (regex.containsMatchIn(path ?: "")) 1 else 0,
                0
            )
        } ?: AttractorMatches()
    }

    /**
     * Using the provided [matchScanner], the source list will be scanned for required and optional matches
     */
    private fun getMatchCount(
        matchScanner: List<RequestAttractorBit>?,
        source: String?
    ): AttractorMatches {
        return matchScanner?.let { matchers ->
            //if (matchers.isEmpty() && source != null)
            //    return AttractorMatches(1, 0, 0)
            val reqCount = matchers.count { it.required }
            if (source.isNullOrBlank())
                return AttractorMatches(reqCount, 0, 0)

            val required = matchers.asSequence()
                .filter { it.required }
                .count { it.regex.containsMatchIn(source) }

            val optional = matchers.asSequence()
                .filter { it.optional.isTrue() }
                .count { it.regex.containsMatchIn(source) }

            AttractorMatches(reqCount, required, optional)
        } ?: AttractorMatches().also {
            if (source != null)
                it.Required = 1
        }
    }

    /**
     * Returns the (required, optional) matches
     *
     * No matchers will return (-1, -1)
     *
     * Null source will return (0, 0)
     */
    fun getParamMatches(source: String?) = getMatchCount(queryParamMatchers, source)

    /**
     * Returns the (required, optional) matches
     *
     * No matchers will return (-1, -1)
     *
     * Null source will return (0, 0)
     */
    fun getBodyMatches(source: String?) = getMatchCount(queryBodyMatchers, source)
}
