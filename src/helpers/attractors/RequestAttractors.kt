package helpers.attractors

import helpers.anyTrue
import helpers.isFalse
import helpers.isTrue
import io.ktor.http.HttpStatusCode
import mimikMockHelpers.QueryResponse

class RequestAttractors() {
    // "url/sub/path"
    var routingPath: String? = null

    var queryParamMatchers: List<RequestAttractorBit>? = null
    // regex match of body items
    var queryBodyMatchers: List<RequestAttractorBit>? = null

    constructor(config: RequestAttractors.() -> Unit) : this() {
        config.invoke(this)
    }

    companion object {
        /**
         * Returns the best match based on the given criteria from the [source] map
         *
         * @return
         * - HttpStatusCode.NotFound (404) = item
         * - HttpStatusCode.Found (302) = null item
         * - HttpStatusCode.Conflict (409) = null item
         */
        fun <T> findBest(
            source: Map<T, RequestAttractors?>,
            path: String?,
            params: String?,
            body: String? = null,
            custom: (T) -> Pair<Int, Int> = { (-1 to -1) }
        ): QueryResponse<T> {
            if (source.isEmpty()) return QueryResponse {
                status = HttpStatusCode.NotFound
            }

            val options = source.mapValues {
                var matchPath = 0
                var matchesParam = (-1 to -1)
                var matchesBody = (-1 to -1)
                val matchesCustom = custom.invoke(it.key)

                it.value?.also { attr ->
                    matchPath = attr.matchesPath(path)
                    matchesParam = attr.getParamMatches(params)
                    matchesBody = attr.getBodyMatches(body)
                }

                var first = 0
                if (matchPath > 0) first += matchPath
                if (matchesParam.first > 0) first += matchesParam.first
                if (matchesBody.first > 0) first += matchesBody.first
                if (matchesCustom.first > 0) first += matchesCustom.first

                var second = 0
                if (matchesParam.second > 0) second += matchesParam.second
                if (matchesBody.second > 0) second += matchesBody.second
                if (matchesCustom.second > 0) second += matchesCustom.second

                if (first == 0 && second == 0)
                    (-1 to -1) else (first to second)
            }

            val response = QueryResponse<T>() {
                status = HttpStatusCode.NotFound
            }

            val groupByReq = options.values
                .groupBy({ it.first }, { it })

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
                            .groupBy({ it.second }, { it })

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
                routingPath?.isBlank().isFalse(),
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

    fun matchesPath(path: String?): Int {
        return routingPath?.run {
            if (path == this) 1 else 0
        } ?: -1
    }

    /**
     * Using the provided [matchScanner], the source list will be scanned for required and optional matches
     */
    private fun getMatchCount(
        matchScanner: List<RequestAttractorBit>?,
        source: String?
    ): Pair<Int, Int> {
        return matchScanner?.let { matchers ->
            if (source.isNullOrBlank())
                return (0 to 0)

            val required = matchers.asSequence()
                .filter { it.required }
                .map { it.value.toRegex() }
                .count { it.containsMatchIn(source) }

            val optional = matchers.asSequence()
                .filter { it.optional.isTrue() }
                .map { it.value.toRegex() }
                .count { it.containsMatchIn(source) }
            (required to optional)
        } ?: (-1 to -1)
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
