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

/**
 * Returns the search of [input] in [this] string.
 * 1. Regex matches will have a range equal to the query length.
 * 2. Second value of pair is "was the match a literal match".
 * 3. Union count between [this] and [input], preserving duplicates
 *
 * Searches (in order):
 * - Literal (which may include regex items)
 * - Regex
 * - How much of the result (regex capture) matches the filter input
 *
 * If [this] is empty or blank, [null] is returned
 */
fun String?.match(input: String): Triple<String?, Boolean, Int> {
    if (this.isNullOrBlank()) return Triple(null, true, 0)

    val asReg = ".*($this)".toRegex().find(input)?.groups?.get(1)
    val asLiteral = isNotBlank() && (input == this)

    val filtArr = toHashSet()
    val regMatch = asReg?.value.orEmpty().toHashSet()

    val logMatchChars = false
    val matchChars = mutableListOf<Char>()
    val filterCnt = filtArr.intersect(regMatch).map { x ->
        val m = kotlin.math.min(filtArr.count { it == x }, regMatch.count { it == x })
        if (logMatchChars)
            repeat((0 until m).count()) { matchChars.add(x) }
        m
    }.sum()

    // matched by not using regex
    val literalComp = filtArr.subtract(regMatch)

    return when {
        asLiteral -> Triple(input, true, filterCnt)
        literalComp.isEmpty() -> Triple(asReg?.value, true, filterCnt)
        asReg != null -> Triple(asReg.value, false, filterCnt)
        else -> Triple(null, true, 0)
    }
}
