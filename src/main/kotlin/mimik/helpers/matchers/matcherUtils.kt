package mimik.helpers.matchers

import kotlinUtils.isTrue
import kotlin.math.min

/**
 * Returns true if this [MatchResult] contains any matching groups
 */
val MatchResult?.hasMatch: Boolean
    get() = this?.groups?.isNotEmpty().isTrue

private val logMatchChars = false

fun matchCount(inputFind: HashSet<Char>, compareMatch: HashSet<Char>): Int {
    // todo; figure out what to do with matchChars
    val matchChars = mutableListOf<Char>()
    return inputFind.intersect(compareMatch).map { x ->
        min(inputFind.count { it == x }, compareMatch.count { it == x }).also {
            if (logMatchChars)
                repeat(it) { matchChars.add(x) }
        }
    }.sum()
}

/**
 * Using [this] as the search criteria, find all the matches in the [input].
 * - Null/ Empty [this] or [input] = empty results
 */
fun String?.matchResults(input: String?): MatcherCollection {
    if (this.isNullOrEmpty() || input == null) return MatcherCollection()

    val resultCollection = MatcherCollection(this)

    if (input == this) { // finish early if it's a literal match
        resultCollection.loadResult(input)
        return resultCollection
    }

    val filterArr = toHashSet()
    toRegex().findAll(input).forEach {
        resultCollection.processResult(it, filterArr)
    }
    return resultCollection
}

/**
 * Processes the incoming [result] into the destination collection ([this])
 */
private fun MatcherCollection.processResult(result: MatchResult, inputReg: HashSet<Char>) {
    val literalComp = inputReg.isLiteralCompatible(result)
    loadResult(result, literalComp)
}

/**
 * Determines if the [match]'s matched value was found based on non-regex means
 */
fun HashSet<Char>.isLiteralCompatible(match: MatchResult): Boolean {
    val firstMatch = match.value
    val firstMatchChars = firstMatch.toHashSet()
    val literalComp = subtract(firstMatchChars)
    return literalComp.isEmpty()
}
