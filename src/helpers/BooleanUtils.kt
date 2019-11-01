package helpers

/**
 * Returns true only if [value] equals true. [null] returns false.
 */
fun Boolean?.isTrue() = this == true

/**
 * Returns true only if [value] equals false. [null] returns false.
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

/**
 * Tests if the following [action] would throw an [Exception]
 */
fun isThrow(action: () -> Unit = {}): Boolean {
    return try {
        action.invoke()
        false
    } catch (e: Exception) {
        true
    }
}
