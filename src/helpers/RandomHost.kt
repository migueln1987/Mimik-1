package helpers

import java.util.UUID
import java.util.UUID.nameUUIDFromBytes
import kotlin.experimental.and
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

/**
 * A class which generates a random number, and persist the last generated number
 */
class RandomHost(init: Int? = null) {
    private val random = Random.Default
    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    var value = nextRandom()
        private set

    init {
        value = init?.let { abs(it) } ?: nextRandom()
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

    val valueToUUID: UUID
        get() = nameUUIDFromBytes(valueAsChars.toByteArray())

    val valueAsUUID
        get() = valueToUUID.toString()

    fun nextRandom(bound: Int? = null): Int {
        value = when (bound) {
            null, 0 -> random.nextInt()
            else -> random.nextInt(abs(bound))
        }

        if (value < 0) value = abs(value)
        return value
    }
}
