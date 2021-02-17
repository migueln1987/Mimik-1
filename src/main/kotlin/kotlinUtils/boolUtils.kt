package kotlinUtils

/**
 * Returns true only if [value] equals true. [null] returns false.
 */
val Boolean?.isTrue get() = this == true

/**
 * Returns [true] if [value] does not equal [true].
 *
 * (null || false) = true
 */
val Boolean?.isNotTrue get() = (this == true).not()

/**
 * Returns true only if [value] equals false. [null] returns false.
 */
val Boolean?.isFalse get() = this == false

/**
 * Attempts to return [this] value, or [false] (when [this] is null)
 */
val Boolean?.orFalse get() = this ?: false

/**
 * Returns true if all the inputs are true
 */
fun allTrue(vararg states: Boolean) = states.all { it }

/**
 * Returns true if any of the input [states] are true.
 */
fun anyTrue(vararg states: Boolean) = states.any { it }
