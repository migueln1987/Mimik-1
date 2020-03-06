package helpers.matchers

import helpers.size

/**
 * Similar to [MatchResult], but with index/ name vars + lambda constructor
 */
open class MatcherResult {
    companion object {
        val EMPTY = MatcherResult(-1)
    }

    val groupName: String
    val groupIndex: Int
    val value: String?
    val range: IntRange
    val isLiteral: Boolean
    /**
     * How many chars literally match from the filter to input
     */
    val litMatchCnt: Int

    /**
     * True: the range of this match is greater (or equal) to zero
     */
    val hasMatch: Boolean
        get() = range.size >= 0

    constructor(index: Int) {
        groupName = ""
        groupIndex = index
        value = null
        range = IntRange.EMPTY
        isLiteral = false
        litMatchCnt = -1
    }

    constructor(
        matchGroup: MatchGroup? = null,
        pre_configs: ResultBuilder? = null,
        config: (ResultBuilder) -> Unit = {}
    ) {
        val configBuilder = pre_configs ?: ResultBuilder()
        config.invoke(configBuilder)
        groupName = configBuilder.groupName
        groupIndex = configBuilder.groupIndex
        value = configBuilder.value ?: matchGroup?.value ?: ""
        range = configBuilder.range ?: matchGroup?.range ?: IntRange.EMPTY
        isLiteral = configBuilder.isLiteral
        litMatchCnt = configBuilder.litMatchCnt
    }

    constructor(config: (ResultBuilder) -> Unit = {}) {
        val configBuilder = ResultBuilder()
        config.invoke(configBuilder)
        groupName = configBuilder.groupName
        groupIndex = configBuilder.groupIndex
        value = configBuilder.value ?: ""
        range = configBuilder.range ?: IntRange.EMPTY
        isLiteral = configBuilder.isLiteral
        litMatchCnt = configBuilder.litMatchCnt
    }

    fun clone(postClone: (ResultBuilder) -> Unit = {}): MatcherResult {
        val configBuilder = ResultBuilder().also {
            it.groupName = groupName
            it.groupIndex = groupIndex
            it.value = value
            it.range = range
            it.isLiteral = isLiteral
            it.litMatchCnt = litMatchCnt
        }
        postClone.invoke(configBuilder)
        return MatcherResult(pre_configs = configBuilder)
    }

    class ResultBuilder {
        var groupName: String = ""
        var groupIndex: Int = -1
        var value: String? = null
        var range: IntRange? = null
        var isLiteral = false
        var litMatchCnt = 0
    }

    override fun toString(): String {
        return if (hasMatch)
            "Index: %d, %sRange: [%s]%s%s".format(
                groupIndex,
                if (groupName.isNotEmpty())
                    "Name: $groupName, " else "",
                range.toString(),
                if (isLiteral) ", Literal: $isLiteral" else "",
                if (litMatchCnt > 0) ", L.Matches: $litMatchCnt" else ""
            )
        else
            "Index: $groupIndex; No match"
    }
}
