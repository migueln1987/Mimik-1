package mimik.helpers.attractors

import kotlinUtils.collections.appendNotNull
import kotlinUtils.collections.firstNotNullResult
import mimik.helpers.matchers.matchResults
import mimik.mockHelpers.RecordedInteractions
import okhttp3.queryItems
import okhttp3.toStringPairs

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
        val chapReq = chap.requestData
        val chapAttrs = chap.attractors

        return when (uniqueType) {
            UniqueTypes.Query -> setOf<String>().appendNotNull(
                chapReq?.httpUrl.queryItems(),
                chapAttrs?.queryMatchers?.mapNotNull { it.value }
            )

            UniqueTypes.Header -> setOf<String>().appendNotNull(
                chapReq?.headers?.toStringPairs(),
                chapAttrs?.headerMatchers?.mapNotNull { it.value }
            )

            UniqueTypes.Body -> setOf<String>().appendNotNull(
                listOf(chapReq?.body.orEmpty()),
                chapAttrs?.bodyMatchers?.mapNotNull { it.value }
            )

            else -> null
        }?.firstNotNullResult {
            searchStr.matchResults(it).get { r ->
                r.groupIndex == 0 && r.hasMatch
            }.firstOrNull()?.value
        }
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
    return this?.mapNotNull { uBit ->
        uBit.uniqueMatching(chap)?.let { UniqueBit(it, uBit.uniqueType!!) }
    }?.let {
        if (it.size == size)
            it else null
    }
}
