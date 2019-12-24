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
 * Regex matches will have a range equal to the query length.
 * Second value of pair is "was the match a literal match".
 *
 * Searches (in order):
 * - Literal (which may include regex items)
 * - Regex
 *
 * If [this] is empty or blank, [null] is returned
 */
fun String?.match(input: String): Pair<String?, Boolean> {
    if (this.isNullOrBlank()) return null to true

    val asReg = this.ensurePrefix(".*?(").ensureSuffix(").*")
        .toRegex().find(input)?.groups?.get(1)
    val asLiteral = isNotBlank() && (input == this)

    return when {
        asLiteral -> input to true
        asReg != null -> asReg.value to false
        else -> null to true
    }
}
