package helpers.attractors

import helpers.filterByMax

object MatchFilter {
    /**
     * Returns the best fit match from [source], based on [keyGroupBy]
     *
     * @return (in length)
     * - null = [source] is null/ empty
     * - 1 = found a match
     * - many = too many conflicting choices
     */
    fun <T> findBestByGroup(
        source: Map<T, AttractorMatches>,
        keyGroupBy: (AttractorMatches) -> Number
    ): Map<T, AttractorMatches> {
        return source.asSequence()
            .map { keyGroupBy.invoke(it.value).toDouble() to (it.key to it.value) }
            .filterByMax { it.first }
            .associate { it.second }
    }

    /**
     * Returns the best match based on the [AttractorMatches] Required/Optional contents
     */
    fun <T> findBestMatch(source: Map<T, AttractorMatches>): Map<T, AttractorMatches>? {
        // filter by "Required" matchers
        val byReq = findBestByGroup(source) { it.MatchesReq }
        if (byReq.isEmpty()) return null
        if (byReq.size == 1) return byReq

        // filter by how well the "Required" matchers match
        val byReqR = findBestByGroup(byReq) { it.reqRatio }
        if (byReqR.isEmpty()) return null
        if (byReqR.size == 1) return byReqR

        // filter by "Optional" matchers
        val byOpt = findBestByGroup(byReqR) { it.MatchesOpt }
        if (byOpt.isEmpty()) return null
        if (byOpt.size == 1) return byOpt

        // filter by how well the "Optional" matchers match
        val byOptR = findBestByGroup(byOpt) { it.optRatio }
        if (byOptR.isEmpty()) return null
        if (byOptR.size == 1) return byOptR

        // give up, too much conflict
        return null
    }
}
