package helpers.attractors

import helpers.allTrue

data class AttractorMatches(
    var Required: Int = -1,
    var MatchesReq: Int = -1,
    var MatchesOpt: Int = -1
) {
    var reqSub = 0.0
    var optSub = 0.0

    /**
     * Returns if this has a [Required] and any [MatchesReq] matches
     */
    val hasMatches
        get() = isBlank || MatchesReq > 0

    /**
     * Returns true if [Required] is set and [Required] is in range of [MatchesReq]
     */
    val satisfiesRequired: Boolean
        get() = Required in 1..MatchesReq
    // Required > 0 && MatchesReq >= Required

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
        else "Required: %d ->{%d @ %.2f, %d @ %.2f}".format(
            Required,
            MatchesReq, reqSub,
            MatchesOpt, optSub
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
        reqSub += data.reqSub

        if (data.MatchesOpt > 0) {
            if (MatchesOpt == -1) MatchesOpt = 0
            MatchesOpt += data.MatchesOpt
        }
        optSub += data.optSub
        return this
    }
}
