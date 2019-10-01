package helpers

/**
 * Returns true only if [value] equals true
 */
fun Boolean?.isTrue() = this == true

/**
 * Returns true only if [value] equals false
 */
fun Boolean?.isFalse() = this == false

/**
 * Returns true if all the inputs are true
 */
fun allTrue(vararg states: Boolean) = states.all { it }

/**
 * Returns true if any of the input [states] are true.
 */
fun anyTrue(vararg states: Boolean) = states.any { it }
