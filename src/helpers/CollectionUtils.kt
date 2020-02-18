package helpers

import java.util.regex.Matcher

/**
 * Loops through the [Iterable] doing [action] on each item.
 *
 * If there's an item after the current action, then [hasNext] is also called.
 */
inline fun <T> Iterable<T>.eachHasNext(action: (T) -> Unit, hasNext: () -> Unit = {}) {
    val iterator = iterator()
    while (iterator.hasNext()) {
        action.invoke(iterator.next())
        if (iterator.hasNext())
            hasNext.invoke()
    }
}

/**
 * Returns a map containing only the non-null results of applying the given [transform] function
 * to each entry in the original map.
 */
inline fun <T, V, R> Map<T, V>.mapNotNullToMap(transform: (Map.Entry<T, V>) -> R?): Map<T, R> {
    val result = mutableMapOf<T, R>()
    forEach { element ->
        transform(element)?.also { result[element.key] = it }
    }
    return result
}

/**
 * Subtracts [other] from this, then remaps the values based on [transform]
 */
inline fun <T, V, oV, R> Map<T, V>.subtractMap(
    other: Map<T, oV>,
    transform: (Map.Entry<T, V>) -> R?
) = filterNot { other.keys.contains(it.key) }.mapNotNullToMap(transform)

/**
 * Returns the elements yielding the largest value of the given function.
 */
inline fun <T, R : Comparable<R>> Sequence<T>.filterByMax(crossinline selector: (T) -> R): Sequence<T> {
    var maxValue: R? = null

    return map {
        val v = selector(it)
        if (maxValue == null || v >= maxValue!!)
            maxValue = v
        it to v
    }.toList()
        .asSequence()
        .filter { maxValue != null && it.second >= maxValue!! }
        .map { it.first }
}

val ClosedRange<Int>.size: Int
    get() = endInclusive - start

val ClosedRange<Long>.size: Long
    get() = endInclusive - start

val <T> List<T>?.nonNull: List<T>
    get() = this ?: listOf()

/**
 * Returns the first element matching the given [predicate], or `null` if element was not found.
 */
inline fun <T, R : Any> Iterable<T>.firstNotNullResult(predicate: (T) -> R?): R? {
    for (element in this) {
        val result = predicate(element)
        if (result != null) return result
    }
    return null
}

/**
 * Returns the first item in [this] which matches a [predicates]
 */
fun <T> Iterable<T>.firstMatchNotNull(vararg predicates: (T) -> Boolean): T? {
    for (p in predicates) {
        val element = firstOrNull { p.invoke(it) }
        if (element != null) return element
    }
    return null
}

/**
 * Appends each list in [lists] that isn't null
 */
fun <T> Iterable<T>.appendNotNull(vararg lists: List<T>?): List<T> {
    return toMutableList().apply {
        lists.forEach {
            if (it != null) addAll(it)
        }
    }
}

/**
 * Similar to [MatchNamedGroupCollection], but with public matcher & groups variables
 */
class MatcherCollection(filterText: String? = null) : Iterable<MatcherResult> {
    var matcher: Matcher? = null
    var matchBundles: MutableList<List<MatcherResult?>> = mutableListOf()
    var filterText = ""
        private set
    private val filterTextChars
        get() = filterText.toHashSet()

    /**
     * How many matches this collection contains
     */
    val matchCount: Int
        get() = matchBundles.sumBy { it.size }

    /**
     * True if this collection has any matches
     */
    val hasMatches: Boolean
        get() = matchBundles.any { it.any { it?.hasMatch ?: false } }

    init {
        this.filterText = filterText.orEmpty()
    }

    /**
     * Get all the results by group index
     */
    operator fun get(byIndex: Int) = get { it.groupIndex == byIndex }

    /**
     * Get all the results by group name
     */
    operator fun get(byGroup: String) = get { it.groupName == byGroup }

    /**
     * Get all the results which match [findBy]
     */
    fun get(matchesOnly: Boolean = false, findBy: (MatcherResult) -> Boolean): List<MatcherResult> {
        return matchBundles.asSequence()
            .flatMap { it.asSequence() } // all are equal
            .filterNotNull() // except the nulls
            .filter {
                if (matchesOnly)
                    it.hasMatch
                else true
            }
            .filter { findBy.invoke(it) }
            .toList()
    }

    /**
     * Attempts to get the root match of [match]
     */
    fun rootIndexOf(match: MatcherResult): MatcherResult? {
        return matchBundles.asSequence()
            .filter {
                // first list which contains this match
                it.any { a -> a.toString() == match.toString() }
            }
            .flatMap { it.asSequence() }
            .firstOrNull { it?.groupIndex == 0 }
    }

    /**
     * Check if this collection contains an index of [byIndex]
     */
    fun contains(byIndex: Int) = contains { it.groupIndex == byIndex }

    /**
     * Check if this collection contains a group named [byGroup]
     */
    fun contains(byGroup: String) = contains { it.groupName == byGroup }

    /**
     * Check if this collection contains a match equal to [compare]
     * - Note; null matches are excluded
     */
    fun contains(compare: (MatcherResult) -> Boolean): Boolean {
        return matchBundles.asSequence()
            .flatMap { it.asSequence() }
            .filterNotNull()
            .any { compare.invoke(it) }
    }

    /**
     * Loads a literal match into this collection.
     * @param result String which was matched
     * @param countMatches Length of the literal matched chars<pre>
     * - True; count matching Chars to [filterText].
     * - False; use [result]'s length
     * </pre>
     */
    fun loadResult(result: String, countMatches: Boolean = false) {
        var countResults: Int? = null
        if (countMatches)
            countResults = matchCount(filterTextChars, result.toHashSet())

        matchBundles.add(
            listOf(
                MatcherResult {
                    it.groupIndex = 0
                    it.value = result
                    it.range = (0..result.length)
                    it.isLiteral = true
                    it.litMatchCnt = countResults ?: result.length
                })
        )
    }

    /**
     * Loads a [matchResult] into this collection.
     * @param literalComp If this [matchResult] should be processed like a String or [MatchResult]
     */
    fun loadResult(matchResult: MatchResult, literalComp: Boolean = false) {
        if (literalComp)
        // input should be a string of literal-like chars
        // so no sub-group processing is needed
            matchBundles.add(
                listOf(
                    MatcherResult {
                        it.groupIndex = 0
                        it.value = matchResult.value
                        val newRange = matchResult.range.run { first..(last + 1) }
                        it.range = newRange
                        it.isLiteral = true
                        it.litMatchCnt = newRange.size
                    })
            )
        else
            loadResults(matchResult)
    }

    /**
     * Loads a [matchResult] into this collection.
     * If [matchResult] has multiple match groups, then multiple lists of groups will bee added accordingly
     */
    fun loadResults(matchResult: MatchResult?) {
        if (matchResult == null) return
        matcher = matchResult.accessField("matcher") as? Matcher

        if (matcher?.groupCount() == 0 && matcher?.group(0) == "") {
            // regex match which allows an empty body
            matchBundles.add(listOf(MatcherResult {
                it.groupIndex = 0
                it.value = ""
                it.range = (0..0)
                it.litMatchCnt = 0
            }))
            return
        }

        matcher?.also { mm ->
            @Suppress("UNCHECKED_CAST")
            val namedGroups = (mm.pattern().accessField("namedGroups") as? Map<String, Int>)
                .orEmpty()

            val toAddList = (0..mm.groupCount()).mapNotNull {
                mm.group(it)
                    .run { if (isNullOrEmpty()) null else this }
                    ?.let { subSequence ->
                        var name = ""
                        if (namedGroups.containsValue(it))
                            name = namedGroups.entries.firstOrNull { f -> f.value == it }?.key ?: ""
                        val matchCnt = matchCount(filterTextChars, subSequence.toHashSet())
                        MatcherResult { b ->
                            b.groupName = name
                            b.groupIndex = it
                            b.value = subSequence
                            b.range = (mm.start(it)..mm.end(it))
                            b.litMatchCnt = matchCnt
                        }
                    }
            }.toMutableList()
            matchBundles.add(toAddList)
        }
    }

    override fun iterator(): Iterator<MatcherResult> {
        if (matchBundles.isEmpty())
            return listOf(MatcherResult.EMPTY).iterator()

        return matchBundles.asSequence()
            .flatMap { it.asSequence() }
            .filterNotNull()
            .iterator()
    }

    // todo; override toString
}

/**
 * Similar to [MatchResult], but with index/ name vars + lambda constructor
 */
open class MatcherResult {
    companion object {
        val EMPTY = MatcherResult(-1)
    }

    val groupName: String
    val groupIndex: Int
    val value: String?
    val range: IntRange
    val isLiteral: Boolean
    /**
     * How many chars literally match from the filter to input
     */
    val litMatchCnt: Int

    val hasMatch: Boolean
        get() = range.size >= 0

    constructor(index: Int) {
        groupName = ""
        groupIndex = index
        value = null
        range = IntRange.EMPTY
        isLiteral = false
        litMatchCnt = -1
    }

    constructor(
        matchGroup: MatchGroup? = null,
        pre_configs: ResultBuilder? = null,
        config: (ResultBuilder) -> Unit = {}
    ) {
        val configBuilder = pre_configs ?: ResultBuilder()
        config.invoke(configBuilder)
        groupName = configBuilder.groupName
        groupIndex = configBuilder.groupIndex
        value = configBuilder.value ?: matchGroup?.value ?: ""
        range = configBuilder.range ?: matchGroup?.range ?: IntRange.EMPTY
        isLiteral = configBuilder.isLiteral
        litMatchCnt = configBuilder.litMatchCnt
    }

    constructor(config: (ResultBuilder) -> Unit = {}) {
        val configBuilder = ResultBuilder()
        config.invoke(configBuilder)
        groupName = configBuilder.groupName
        groupIndex = configBuilder.groupIndex
        value = configBuilder.value ?: ""
        range = configBuilder.range ?: IntRange.EMPTY
        isLiteral = configBuilder.isLiteral
        litMatchCnt = configBuilder.litMatchCnt
    }

    fun clone(postClone: (ResultBuilder) -> Unit = {}): MatcherResult {
        val configBuilder = ResultBuilder().also {
            it.groupName = groupName
            it.groupIndex = groupIndex
            it.value = value
            it.range = range
            it.isLiteral = isLiteral
            it.litMatchCnt = litMatchCnt
        }
        postClone.invoke(configBuilder)
        return MatcherResult(pre_configs = configBuilder)
    }

    class ResultBuilder {
        var groupName: String = ""
        var groupIndex: Int = -1
        var value: String? = null
        var range: IntRange? = null
        var isLiteral = false
        var litMatchCnt = 0
    }

    override fun toString(): String {
        return if (hasMatch)
            "Index: %d, %sRange: [%s], Literal: %b, L.Matches: %d".format(
                groupIndex,
                if (groupName.isNotEmpty())
                    "Name: $groupName, " else "",
                range.toString(),
                isLiteral,
                litMatchCnt
            )
        else
            "Index: $groupIndex; No match"
    }
}
