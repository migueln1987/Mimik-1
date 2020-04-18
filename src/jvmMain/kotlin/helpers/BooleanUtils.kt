package helpers

/**
 * Returns true only if [value] equals true. [null] returns false.
 */
fun Boolean?.isTrue() = this == true

/**
 * Returns [true] if [value] does not equal [true].
 *
 * (null || false) = true
 */
fun Boolean?.isNotTrue() = (this == true).not()

/**
 * Returns true only if [value] equals false. [null] returns false.
 */
fun Boolean?.isFalse() = this == false

/**
 * Attempts to return [this] value, or [false] (when [this] is null)
 */
val Boolean?.orFalse
    get() = this ?: false

/**
 * Returns true if all the inputs are true
 */
fun allTrue(vararg states: Boolean) = states.all { it }

/**
 * Returns true if any of the input [states] are true.
 */
fun anyTrue(vararg states: Boolean) = states.any { it }

/**
 * Tests if the following [action] would throw an [Exception].
 * Also safely handles throws.
 */
inline fun isThrow(action: () -> Unit = {}): Boolean {
    return try {
        action.invoke()
        false
    } catch (e: Exception) {
        true
    }
}

/*
 * Attempts to get data from [action], or returns a null.
 * Throws return null.
 */
inline fun <T> tryOrNull(action: () -> T?): T? {
    return try {
        action.invoke()
    } catch (e: Exception) {
        null
    }
}

/**
 * Returns true if this [MatchResult] contains any matching groups
 */
val MatchResult?.hasMatch: Boolean
    get() = this?.groups?.isNotEmpty().isTrue()

private val logMatchChars = false

fun matchCount(inputFind: HashSet<Char>, compareMatch: HashSet<Char>): Int {
    // todo; figure out what to do with matchChars
    val matchChars = mutableListOf<Char>()
    return inputFind.intersect(compareMatch).map { x ->
        kotlin.math.min(inputFind.count { it == x }, compareMatch.count { it == x })
            .also {
                if (logMatchChars)
                    repeat((0 until it).count()) { matchChars.add(x) }
            }
    }.sum()
}

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
