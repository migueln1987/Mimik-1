package helpers

import java.util.Random

/**
 * A class which generates a random number, and persist the last generated number
 */
class RandomHost {
    private val random = Random()
    var value = nextRandom()
        private set

    fun nextRandom(bound: Int? = null): Int {
        value = bound?.let { random.nextInt(bound) } ?: random.nextInt()
        return value
    }
}
