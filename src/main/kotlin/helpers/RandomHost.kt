package helpers

import java.util.UUID
import java.util.UUID.nameUUIDFromBytes
import kotlin.experimental.and
import kotlin.math.absoluteValue
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

    companion object {
        /** 'a'..'z' */
        val char_Lower = ('a'..'z').toList()

        /** 'A'..'Z' */
        val char_Upper = ('A'..'Z').toList()

        /** lower + upprecase letters' */
        val chars = char_Lower + char_Upper

        /** numbers */
        val nums = ('0'..'9').toList()

        /** letters and numbers */
        val word = chars + nums

        val useSymbols = nums + listOf('=', ',', '.', '<', '>')
    }

    init {
        value = init?.absoluteValue ?: nextRandom()
    }

    /**
     * Returns a random series of chars between [min] and [max] length.
     *
     * Results are predictable based on the current [value].
     */
    fun valueAsChars(min: Int = 5, max: Int = 10): String {
        val useRandom = Random(value.toLong())

        var useMin = min
        var useMax = max
        if (useMin > useMax) {
            useMax = useMin
            useMin = useMax
        }

        // positive value between 5-10 chars
        val length = max(useMin, useRandom.nextInt(useMax).absoluteValue)

        val byteData = ByteArray(length)
        useRandom.nextBytes(byteData)
        return byteData.asSequence()
            .map { it and Byte.MAX_VALUE } // (-127 - 127) -> 0-127
            .map { it / Byte.MAX_VALUE.toFloat() } // 0-127 -> 0-100%
            .map { (it * (charPool.size - 1)).toInt() } // % -> 0-charPool
            .map { charPool[it] } // get char value
            .joinToString("")
    }

    /**
     * Generates a string from a list of
     * - Char ranges
     * - how many items to generate
     */
    fun valueToValid(vCheck: (MutableList<Pair<List<Char>, Int>>) -> Unit): String {
        val useRandom = Random(value.toLong())
        val sb = StringBuilder()

        val vCheckList = mutableListOf<Pair<List<Char>, Int>>()
        vCheck.invoke(vCheckList)

        return vCheckList.fold(StringBuilder()) { acc, (range, count) ->
            sb.clear()
            repeat(count) {
                sb.append(range[useRandom.nextInt(0, range.size)])
            }
            acc.append(sb.toString())
        }.toString()
    }

    val valueToUUID: UUID
        get() = nameUUIDFromBytes(valueAsChars().toByteArray())

    /**
     * Returns a [UUID] in string form (8-4-4-4-12, each letter being hexadecimal)
     */
    val valueAsUUID
        get() = valueToUUID.toString()

    /**
     * Causes [value] to randomize to a new value (within 0 and [bound])
     */
    fun nextRandom(bound: Int? = null): Int {
        value = when (bound) {
            null, 0 -> random.nextInt()
            else -> random.nextInt(bound.absoluteValue)
        }

        if (value < 0) value = value.absoluteValue
        return value
    }
}
