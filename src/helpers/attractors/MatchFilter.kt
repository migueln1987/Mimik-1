package helpers.attractors

import helpers.isTrue

object MatchFilter {
    /**
     * Returns the best fit match from [source], based on [keyGroupBy]
     *
     * @return (in length)
     * - null = [source] is null/ empty
     * - 1 = found a match
     * - many = too many conflicting choices
     */
    private fun findBestByGroup(
        source: Collection<AttractorMatches>?,
        keyGroupBy: (AttractorMatches) -> Number
    ): List<AttractorMatches>? {
        if (source == null || source.isEmpty()) return null

        val groupBy = source
            .groupBy({ keyGroupBy.invoke(it) }, { it })

        val highestKey = groupBy.keys.maxBy { it.toDouble() }
        return when (groupBy[highestKey]?.size ?: -1) {
            1 -> { // we found a match!
                val bestKey = groupBy[highestKey]?.first() ?: return null
                return listOf(bestKey)
            }

            in (2..Int.MAX_VALUE) -> return groupBy[highestKey]

            else -> null
        }
    }

    /**
     * Returns the best match based on the [AttractorMatches] Required/Optional contents
     */
    fun <T> findBestMatch(source: Map<T, AttractorMatches>): AttractorMatches? {
        // filter by "Required" matchers
        val byReq = findBestByGroup(source.values) { it.MatchesReq }
        if (byReq?.isEmpty().isTrue()) return null
        if (byReq?.size == 1) return byReq.first()

        // filter by how well the "Required" matchers match
        val byReqR = findBestByGroup(byReq) { it.reqRatio }
        if (byReqR?.isEmpty().isTrue()) return null
        if (byReqR?.size == 1) return byReqR.first()

        // filter by "Optional" matchers
        val byOpt = findBestByGroup(byReqR) { it.MatchesOpt }
        if (byOpt?.isEmpty().isTrue()) return null
        if (byOpt?.size == 1) return byOpt.first()

        // filter by how well the "Optional" matchers match
        val byOptR = findBestByGroup(byOpt) { it.optRatio }
        if (byOptR?.isEmpty().isTrue()) return null
        if (byOptR?.size == 1) return byOptR.first()

        // give up, too much conflict
        return null
    }
}
