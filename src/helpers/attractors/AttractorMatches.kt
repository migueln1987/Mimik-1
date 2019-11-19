package helpers.attractors

import helpers.allTrue

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
        get() = MatchesReq >= Required

    /**
     * Returns true if [Required] is set, and [MatchesReq] equals [Required]
     */
    val satisfiesRequired: Boolean
        get() = Required > 0 && matchingRequired

    val isBlank: Boolean
        get() {
            return allTrue(
                Required == -1,
                MatchesReq == -1
            )
        }

    override fun toString(): String {
        return if (isBlank)
            "No Data"
        else "Required: %d ->{%d @ %.2f%%, %d @ %.2f%%} - %s".format(
            Required,
            MatchesReq, reqRatio,
            MatchesOpt, optRatio,
            if (satisfiesRequired) "Pass" else "Fail"
        )
    }

    /**
     * Appends the current data, then returns a reference of this object
     */
    fun appendValues(data: AttractorMatches?): AttractorMatches {
        if (data == null || data.isBlank) return this
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
