package mimik.helpers

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
    val random = Random.Default

    /**
     * Current [value] of this random host.
     */
    var value = nextInt()
        private set

    /**
     * [value] as an absolute number
     */
    val value_abs get() = value.absoluteValue

    companion object {
        /** 'a'..'z' */
        val pool_LetterLower = ('a'..'z').toList()

        /** 'A'..'Z' */
        val pool_LetterUpper = ('A'..'Z').toList()

        /** lower + uppercase letters' */
        val pool_Letters = pool_LetterLower + pool_LetterUpper

        /** numbers */
        val pool_Nums = ('0'..'9').toList()

        /** Upper/ Lower letters and numbers **/
        val pool_LetterNum = pool_Letters + pool_Nums

        val pool_LetterNumSymbols = pool_Letters + pool_Nums + listOf('=', ',', '.', '<', '>')

        /**
         * ASCII 32 (space) to 126 (tilde)
         */
        var pool_Chars = (' '..'~').toList()
    }

    init {
        value = init ?: nextInt()
    }

    /**
     * Returns a random series of chars between [min] and [max] length.
     *
     * - Results are predictable based on the current [value].
     * @param Pool Default: [pool_LetterNum]
     */
    fun valueAsChars(
        min: Int = 5,
        max: Int = 10,
        Pool: List<Char> = pool_LetterNum
    ): String {
        val useRandom = Random(value_abs.toLong())

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
            .map { (it * (Pool.size - 1)).toInt() } // % -> 0-charPool
            .map { Pool[it] } // get char value
            .joinToString("")
    }

    /**
     * Generates a string from a list of
     * - Char ranges
     * - how many items to generate
     */
    fun valueToValid(vCheck: (MutableList<Pair<List<Char>, Int>>) -> Unit): String {
        val useRandom = Random(value_abs.toLong())
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
     * Generates a new random value from 0 (inclusive) until (exclusive) [Max]
     *
     * - 0: Int.MIN_VALUE .. Int.MAX_VALUE
     * - null: Int.MIN_VALUE .. Int.MAX_VALUE
     * - else: ([Min]|0) .. ([Max] - 1)
     */
    fun nextInt(Max: Int? = null, Min: Int = 0): Int {
        value = when (Max) {
            null, 0 -> random.nextInt()
            else -> {
                val (sMin, sMax) = arrayOf(Min, Max).sortedArray()
                random.nextInt(sMin, sMax)
            }
        }

        return value
    }

    /** Gets the next random [Boolean] value. **/
    fun nextBool() = random.nextBoolean()

    /**
     * Randomly returns the result of [action] or [default]
     */
    inline fun <T> actionOrDefault(default: T, action: () -> T): T =
        if (nextBool()) action() else default

    fun nextRegex(config: RegBuilder.RegexBuilder.() -> Unit = {}): String =
        RegBuilder(this).nextRegex(config)
}
