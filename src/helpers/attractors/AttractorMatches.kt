package helpers.attractors

data class AttractorMatches(
    var Required: Int = -1,
    var MatchesReq: Int = -1,
    var MatchesOpt: Int = -1
) {
    var reqRatio = 0.0
    var optRatio = 0.0

    /**
     * Returns true if [Required] equals [MatchesReq]
     */
    val matchingRequired: Boolean
        get() = Required == MatchesReq

    /**
     * Returns true if [Required] is set, and [MatchesReq] equals [Required]
     */
    val satisfiesRequired: Boolean
        get() = Required > 0 && (Required == MatchesReq)

    override fun toString(): String {
        return "Required: %d ->{%d @ %.2f%%, %d @ %.2f%%} - %s".format(
            Required,
            MatchesReq, reqRatio,
            MatchesOpt, optRatio,
            if (satisfiesRequired) "Pass" else "Fail"
        )
    }

    fun appendValues(data: AttractorMatches?): AttractorMatches {
        if (data == null) return this
        if (data.Required > 0) {
            if (Required == -1) Required = 0
            Required += data.Required
        }

        if (data.MatchesReq > 0) {
            if (MatchesReq == -1) MatchesReq = 0
            MatchesReq += data.MatchesReq
        }
        reqRatio += data.reqRatio

        if (data.MatchesOpt > 0) {
            if (MatchesOpt == -1) MatchesOpt = 0
            MatchesOpt += data.MatchesOpt
        }
        optRatio += data.optRatio
        return this
    }
}
