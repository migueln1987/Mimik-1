package helpers.attractors

import helpers.match
import helpers.queryItems
import helpers.toStringPairs
import mimikMockHelpers.RecordedInteractions

enum class UniqueTypes {
    Query, Header, Body, Unknown;

    val type: String
        get() = name.toLowerCase()
}

class UniqueBit(var searchStr: String? = null, var uniqueType: UniqueTypes? = UniqueTypes.Unknown) {

    override fun toString(): String {
        return "%s: (%s)".format(
            (uniqueType ?: UniqueTypes.Unknown).name,
            searchStr ?: "{null}"
        )
    }

    /**
     * Returns the matching content from [chap] based on [uniqueType]
     */
    fun uniqueMatching(chap: RecordedInteractions): String? {
        val chapReq = chap.requestData ?: return null

        var matchStr: String? = null
        when (uniqueType) {
            UniqueTypes.Query -> {
                chapReq.httpUrl.queryItems().any {
                    val (m, _) = searchStr.match(it)
                    matchStr = m?.value
                    m != null
                }
            }
            UniqueTypes.Header -> {
                chapReq.headers?.toStringPairs()?.any {
                    val (m, _) = searchStr.match(it)
                    matchStr = m?.value
                    m != null
                }
            }
            UniqueTypes.Body -> {
                chapReq.body?.also {
                    matchStr = searchStr.match(it).first?.value
                }
            }
            else -> Unit
        }

        return matchStr
    }
}

/**
 * Scans the contents of [chap]'s requestData (using this list's regex/ literal values).
 *
 * Returns:
 * - list of String/UniqueType if ALL the input's values match [chap]
 * - null if [chap]'s requestData is null
 * - null if not ALL of the uniqueBits match
 */
fun List<UniqueBit>?.uniqueAllOrNull(chap: RecordedInteractions): List<UniqueBit>? {
    if (!chap.hasRequestData) return null
    return this?.mapNotNull { uBit ->
        uBit.uniqueMatching(chap)?.let { UniqueBit(it, uBit.uniqueType!!) }
    }?.let {
        if (it.size == size)
            it else null
    }
}
