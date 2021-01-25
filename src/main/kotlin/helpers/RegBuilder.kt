package helpers

import kotlin.math.abs
import kotlin.random.Random

// EBNF ref: https://github.com/kean/Regex/blob/master/grammar.ebnf
class RegBuilder(val randomHost: RandomHost = RandomHost()) {

    class RegexBuilder(config: RegexBuilder.() -> Unit) {
        // How many "groups" of searching items to add
        var chunks = 10

        /**
         * Flag to use count modifiers such as:
         * - "*": None or Many
         * - "+": One or Many
         * - "?": One or None
         * - "{count}"/ "{min, max}"
         *
         * Note: setting to "null" will random this value each time
         */
        var includeOrQuantifiers: Boolean? = null
            get() = if (field == null) Random.nextBoolean() else field

        /**
         * If the range "{min, max}" items should be used.
         *
         * - Note 1: [includeOrQuantifiers] must return true for this to be used
         * - Note 2: setting to "null" will random this value each time
         */
        var includeRange: Boolean? = null
            get() = if (field == null) Random.nextBoolean() else field

        /**
         * Min/ Max for "{#,#}"
         * - Max = null: {min}
         * - Max = -1: {min,}
         * - Max > 0: {min, max}
         */
        var rangeMinMax: Pair<Int, Int?> = Pair(9, 10)

        /**
         * If the lazy flags should be included.
         *
         * - Note 1: [includeOrQuantifiers] must return true for this to be used
         * - Note 2: setting to "null" will random this value each time
         */
        var includeLazy: Boolean? = null
            get() = if (field == null) Random.nextBoolean() else field

        /**
         * Enables/ disables the use of sets
         * - \[abc]
         * - \[123def]
         */
        var includeSets: Boolean? = null
            get() = if (field == null) Random.nextBoolean() else field
        var setSize: Int? = null
            get() = if (field == null) Random.nextInt() else field

        /**
         * Pools of characters to use in a set (to match)
         */
        var setPool: List<Char> = RandomHost.pool_LetterNumSymbols

        /**
         * If ranges should be compressed
         * - false: ABCD
         * - true: A-D
         */
        var setUseRanges: Boolean? = null
            get() = if (field == null) Random.nextBoolean() else field

        /**
         * Enables/ disabled the use of excepts in sets
         * - \[^abc]
         * - \[abc]
         */
        var includeSetExcepts: Boolean? = null
            get() = if (field == null) Random.nextBoolean() else field

        /**
         * If the following characters classes should be used
         * - Spaces: \S \s
         * - Digits: \d \d
         * - Chars: \W \W
         */
        var includeCharacterClass: Boolean? = null
            get() = if (field == null) Random.nextBoolean() else field

        /**
         * If Character groups (sets @ [includeSets]) or character classes can be used
         */
        var includeMatchCharacterClass: Boolean? = null
            get() = if (field == null) Random.nextBoolean() else field

        /**
         * Any ASCII char from 32 (space) to 126 (tilde)
         */
        var includeRandomPoolChar: Boolean? = null
            get() = if (field == null) Random.nextBoolean() else field

        init {
            config(this)
        }
    }

    val orQuantifier = listOf("*", "+", "?", null)

    val CharacterClass = "SsdDDwW".map { "\$it" }

    val escapeChars = "[\\^\$.|?*+()".toList()

    fun RangeQuantifier(builder: RegexBuilder): String {
        return "{%s%s%s}".let { rStr ->
            val min = randomHost.random.nextInt(builder.rangeMinMax.first)
            val max = randomHost.random.nextInt(min, builder.rangeMinMax.second ?: Int.MAX_VALUE)
            var (range_from, range_to) = listOf(min, max).map { it.toString() }

            val useRange = (min != max) && builder.rangeMinMax.second != null
            val useTo = useRange && (builder.rangeMinMax.second ?: 0) > 0
            // "from" will be set to max's value, with "to" being the highest number
            // so switch, to use the higher number
            if (!useTo)
                range_from = range_to
            rStr.format(
                range_from,
                if (useRange) "," else "",
                if (useTo) range_to else ""
            )
        }
    }

    fun Quantifier(builder: RegexBuilder): String {
        if (!builder.includeOrQuantifiers!!) return ""
        var type = orQuantifier.random()
        if (type == null && !builder.includeRange!!) return ""
        type = type ?: RangeQuantifier(builder)

        return type + randomHost.actionOrDefault("") { "?" }.let {
            if (builder.includeLazy!!) it else ""
        }
    }

    /**
     * Converts a list of chars to a string with ranges
     */
    fun rangeConverter(in_list: List<Char>): String {
        val rejoins = mutableListOf<Pair<Int, Int>>()
        val inInt_list = in_list.map { it.toInt() }
        val reqSpacing = 2 // spacing between ranges; (1)"A-CD" vs (2)"A-D"
        var pre_v = -1
        var steps = 0
        var mark = -1

        inInt_list.forEachIndexed { index, v ->
            if ((v - pre_v) == 1) {
                if (mark == -1) {
                    mark = index - 1
                    steps = 0
                }
                steps++
            } else {
                if (mark > -1) {
                    if (steps > reqSpacing)
                        rejoins.add(mark to (mark + steps))
                    mark = -1
                    steps = 0
                }
            }
            pre_v = v
        }
        if (mark > -1 && steps > 1)
            rejoins.add(mark to (mark + steps))

        if (rejoins.isEmpty())
            return in_list.joinToString("")

        var prev_to = 0
        return rejoins.foldIndexed("") { index, acc, (from_c, to_c) ->
            var carry = acc
            if (index > 0) prev_to++
            carry += (prev_to until from_c).map { inInt_list[it].toChar() }.joinToString("")
            carry += inInt_list[from_c].toChar() + "-" + inInt_list[to_c].toChar()
            if (index == (rejoins.size - 1) && to_c < inInt_list.size)
                carry += inInt_list.drop(to_c + 1).map { it.toChar() }.joinToString("")
            prev_to = to_c
            carry
        }
    }

    fun CharacterGroup(builder: RegexBuilder): String {
        val pool = builder.setPool
        if (pool.isEmpty()) return ""

        // If using a random number, trim to pool size
        var reqSize = if (abs(builder.setSize!!) >= pool.size)
            randomHost.nextInt(pool.size) else abs(builder.setSize!!)
        if (reqSize == 0) reqSize++
        val capStr = pool.shuffled().take(reqSize).sorted()

        val setStr = if (builder.setUseRanges!!)
            rangeConverter(capStr) else capStr.joinToString("")

        return "[%s%s]".format(
            randomHost.actionOrDefault("") { "^" }.let {
                if (builder.includeSetExcepts!!) it else ""
            },
            setStr
        )
    }

    /**
    MatchCharacterClass
    ::= CharacterGroup
    | CharacterClass
     */
    fun MatchCharacterClass(builder: RegexBuilder): String {
        val range = mutableListOf(-1)
        if (builder.includeSets!!) range.add(0)
        if (builder.includeCharacterClass!!) range.add(1)
        return when (range.random()) {
            0 -> CharacterGroup(builder)
            1 -> CharacterClass.random()
            else -> ""
        }
    }

    /**
    MatchItem
    ::= MatchAnyCharacter
    | MatchCharacterClass
    | MatchCharacter
     */
    fun Match(builder: RegexBuilder): String {
        val range = mutableListOf(-1, 0)
        if (builder.includeMatchCharacterClass!!) range.add(1)
        if (builder.includeRandomPoolChar!!) range.add(2)

        val matchItem = when (range.random()) {
            0 -> "." // MatchAnyCharacter
            1 -> MatchCharacterClass(builder)
            2 -> RandomHost.pool_Chars.random().let {
                if (escapeChars.contains(it)) "\\$it" else it
            }.toString()
            else -> ""
        }

        return if (matchItem.isEmpty()) ""
        else matchItem + Quantifier(builder)
    }

    fun nextRegex(config: RegexBuilder.() -> Unit = {}): String {
        val builder = RegexBuilder(config)
        return (0..builder.chunks).joinToString("") { Match(builder) }
    }
}
