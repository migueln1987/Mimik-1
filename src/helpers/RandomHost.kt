package helpers

import java.util.Random
import kotlin.experimental.and
import kotlin.math.abs
import kotlin.math.max

/**
 * A class which generates a random number, and persist the last generated number
 */
class RandomHost {
    private val random = Random()
    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    var value = nextRandom()
        private set

    fun nextRandom(bound: Int? = null): Int {
        value = if (bound == 0)
            random.nextInt()
        else
            bound?.let { random.nextInt(abs(bound)) } ?: random.nextInt()

        if (value < 0) value = abs(value)
        return value
    }

    val valueAsChars: String
        get() {
            val useRandom = Random(value.toLong())

            // positive value between 5-10 chars
            val length = max(5, abs(useRandom.nextInt(10)))

            val byteData = ByteArray(length)
            useRandom.nextBytes(byteData)

            return byteData.asSequence()
                .map { it and Byte.MAX_VALUE } // abs value
                .map { it / Byte.MAX_VALUE.toFloat() } // 0-127 -> 0-100%
                .map { (it * (charPool.size - 1)).toInt() } // % -> 0-charPool
                .map { charPool[it] } // get char value
                .joinToString("")
        }
}
