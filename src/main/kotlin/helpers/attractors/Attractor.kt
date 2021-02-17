package helpers.attractors

import io.ktor.http.*
import mimikMockHelpers.QueryResponse

abstract class Attractor {
    var routingPath: RequestAttractorBit? = null
    var queryMatchers: List<RequestAttractorBit>? = null
    var headerMatchers: List<RequestAttractorBit>? = null
    var bodyMatchers: List<RequestAttractorBit>? = null

    companion object {
        val skipHeaders =
            listOf(
                HttpHeaders.ContentLength, HttpHeaders.Host, HttpHeaders.Accept, HttpHeaders.TE,
                HttpHeaders.UserAgent, HttpHeaders.Connection, HttpHeaders.CacheControl,
                "localhost"
            )

        enum class GuessType {
            /**
             * Must have matched at least 1 required value
             */
            Any,

            /**
             * Must match at least the required times
             */
            Exact
        }

        /**
         * Returns the best match based on the given criteria from the [source] map
         *
         * @return
         * - HttpStatusCode.Found (302) = item
         * - HttpStatusCode.NotFound (404) = item
         * - HttpStatusCode.Conflict (409) = null item
         */
        fun <T> findBest(
            source: Map<T, Attractor>,
            path: String?,
            queries: String?,
            headers: List<String>?,
            body: String? = null,
            custom: (T) -> AttractorMatches = { AttractorMatches() }
        ): QueryResponse<T> {
            return findBest_many(source, path, queries, headers, body, custom).let {
                val items = it.item
                when {
                    items.isNullOrEmpty() -> QueryResponse { status = HttpStatusCode.NotFound }

                    items.size == 1 -> QueryResponse {
                        status = it.status
                        item = items.first()
                    }

                    else -> QueryResponse { status = HttpStatusCode.Conflict }
                }
            }
        }

        fun <T> findBest_many(
            source: Map<T, Attractor>,
            path: String?,
            queries: String?,
            headers: List<String>?,
            body: String? = null,
            custom: (T) -> AttractorMatches = { AttractorMatches() }
        ): QueryResponse<List<T>> {
            val options = findMany(
                source, path, queries, headers, body,
                GuessType.Exact,
                custom
            )

            return QueryResponse {
                if (options.isEmpty()) {
                    status = HttpStatusCode.NotFound
                } else {
                    val bestMatch = MatchFilter.findBestMatch(options)
                    if (bestMatch == null)
                        status = HttpStatusCode.NotFound
                    else {
                        item = bestMatch.keys.toList()
                        status = HttpStatusCode.Found
                    }
                }
            }
        }

        /**
         * Returns the matching results from [source] which have at least 1 [AttractorMatches.MatchesReq]
         */
        fun <T> findMany(
            source: Map<T, Attractor>,
            path: String? = null,
            queries: String? = null,
            headers: List<String>? = null,
            body: String? = null,
            guessType: GuessType = GuessType.Exact,
            custom: (T) -> AttractorMatches = { AttractorMatches() }
        ): Map<T, AttractorMatches> {
            if (source.isEmpty()) return linkedMapOf()

            Fails.clear()
            // Filter to early fail checks
            return source.mapValues { it.value to AttractorMatches() }
                .asSequence()
                .parseMatchFilter { custom.invoke(it.key) }
                .parseMatchFilter { matchesPath(path) }
                .parseMatchFilter { getQueryMatches(queries) }
                .parseMatchFilter { getHeaderMatches(headers) }
                .parseMatchFilter { getBodyMatches(body) }
                .filter { m ->
                    when (guessType) {
                        GuessType.Any -> true
                        GuessType.Exact -> {
                            m.value.second.satisfiesRequired
                                .also { if (!it) Fails.add(m.value) }
                        }
                    }
                }
                .associate { it.key to it.value.second }
        }

        val Fails = mutableListOf<Pair<Attractor?, AttractorMatches>>()

        /**
         * [parseMatch] within a [Sequence]'s [filter]
         */
        private inline fun <T : Map.Entry<*, Pair<Attractor, AttractorMatches>>> Sequence<T>.parseMatchFilter(
            crossinline filterAction: Attractor.(T) -> AttractorMatches? = { null }
        ): Sequence<T> = filter {
            it.parseMatch { attr -> filterAction(attr, it) }
        }

        private inline fun Map.Entry<*, Pair<Attractor, AttractorMatches>>.parseMatch(
            matcher: (Attractor) -> AttractorMatches?
        ): Boolean {
            val mResult = matcher(value.first)
            value.second.appendValues(mResult)
            if (!value.second.hasMatches)
                Fails.add(value)
            return value.second.hasMatches
        }
    }

    private fun matchesPath(source: String?): AttractorMatches {
        return routingPath?.let { listOf(it) }.orEmpty()
            .getMatches(source?.removePrefix("/"))
    }

    private fun getQueryMatches(source: String?): AttractorMatches {
        val inputs = source?.split('&')
        return queryMatchers.getMatches(inputs)
    }

    private fun getHeaderMatches(source: List<String>?): AttractorMatches {
        val inputs = source?.filterNot {
            skipHeaders.any { hHeaders -> it.startsWith(hHeaders, true) }
        }
        return headerMatchers.getMatches(inputs)
    }

    private fun getBodyMatches(source: String?): AttractorMatches {
        return bodyMatchers.getMatches(source)
    }
}
