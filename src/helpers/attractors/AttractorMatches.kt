package helpers.attractors

data class AttractorMatches(
    var Required: Int = -1,
    var MatchesReq: Int = -1,
    var MatchesOpt: Int = -1
) {
    val satisfiesRequirement: Boolean
        get() = Required > 0 && (Required == MatchesReq)

    fun appendValues(data: AttractorMatches): AttractorMatches {
        if (data.Required > 0) Required += data.Required
        if (data.MatchesReq > 0) MatchesReq += data.MatchesReq
        if (data.MatchesOpt > 0) MatchesOpt += data.MatchesOpt
        return this
    }
}
