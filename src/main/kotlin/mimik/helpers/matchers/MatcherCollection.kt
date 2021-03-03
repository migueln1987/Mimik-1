@file:Suppress("unused")

package mimik.helpers.matchers

import kotlinUtils.ranges.size
import kotlinUtils.tryCast
import mimik.helpers.accessField
import java.util.regex.Matcher

/**
 * Similar to [MatchNamedGroupCollection], but with public matcher & groups variables
 */
class MatcherCollection(filterText: String? = null) : Iterable<MatcherResult> {
    var matcher: Matcher? = null
    var matchBundles: MutableList<List<MatcherResult?>> = mutableListOf()
    var filterText = ""
        private set
    var inputStr = ""
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

    /**
     * Get the highest matching index number.
     *
     * When no matches are found/ available, then `-1` is returned
     */
    val lastIndex: Int
        get() {
            var maxIndex = -1
            get(true) {
                if (it.groupIndex >= maxIndex)
                    maxIndex = it.groupIndex
                false
            }

            return maxIndex
        }

    init {
        this.filterText = filterText.orEmpty()
    }

    operator fun invoke(config: MatcherCollection.() -> Unit = {}): MatcherCollection {
        config(this)
        return this
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
     *
     * @param hasMatchesOnly Include a filter where the items need to have matches
     */
    fun get(hasMatchesOnly: Boolean = false, findBy: (MatcherResult) -> Boolean): List<MatcherResult> {
        return matchBundles.asSequence()
            .flatMap { it.asSequence() } // all are equal
            .filterNotNull() // except the nulls
            .filter {
                if (hasMatchesOnly)
                    it.hasMatch
                else true
            }
            .filter { findBy(it) }
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
            .any { compare(it) }
    }

    /**
     * Loads a literal match into this collection.
     * @param result String which was matched
     * @param countMatches Length of the literal matched chars<pre>
     * - True; count matching Chars to [filterText].
     * - False; use [result]'s length
     * </pre>
     */
    fun loadResult(result: String, countMatches: Boolean = false): MatcherCollection {
        var countResults: Int? = null
        if (countMatches)
            countResults = matchCount(filterTextChars, result.toHashSet())

        matchBundles.add(
            listOf(MatcherResult {
                it.groupIndex = 0
                it.value = result
                it.range = (0..result.length)
                it.isLiteral = true
                it.litMatchCnt = countResults ?: result.length
            })
        )
        return this
    }

    fun loadItems(vararg items: String): MatcherCollection {
        items.forEach { item ->
            matchBundles.add(
                listOf(MatcherResult {
                    it.groupIndex = 0
                    it.value = item
                    it.range = (0..item.length)
                    it.isLiteral = true
                    it.litMatchCnt = item.length
                })
            )
        }
        return this
    }

    /**
     * Loads a [matchResult] into this collection.
     * @param literalComp If this [matchResult] should be processed like a String or [MatchResult]
     */
    fun loadResult(matchResult: MatchResult, literalComp: Boolean = false): MatcherCollection {
        if (literalComp) {
            // input should be a string of literal-like chars
            // so no sub-group processing is needed
            inputStr = (matchResult.accessField("input") as? String).orEmpty()
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
        } else
            loadResults(matchResult)
        return this
    }

    /**
     * Loads a [matchResult] into this collection.
     * If [matchResult] has multiple match groups, then multiple lists of groups will bee added accordingly
     */
    fun loadResults(matchResult: MatchResult?): MatcherCollection {
        if (matchResult == null) return this
        matcher = matchResult.accessField("matcher") as? Matcher

        if (matcher?.groupCount() == 0 && matcher?.group(0) == "") {
            // regex match which allows an empty body
            matchBundles.add(listOf(MatcherResult {
                it.groupIndex = 0
                it.value = ""
                it.range = (0..0)
                it.litMatchCnt = 0
            }))
            return this
        }

        matcher?.also { mm ->
            inputStr = (mm.accessField("text") as? String).orEmpty()

            val namedGroups = mm.pattern().accessField("namedGroups")
                .tryCast<Map<String, Int>>().orEmpty()

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
        return this
    }

    companion object Caster {
        /**
         * Converts [MatchResult] into an easier to process [MatcherResult] (list of lists)
         */
        fun castResult(
            matchResult: MatchResult?,
            filterTextChars: HashSet<Char> = hashSetOf()
        ): MutableList<List<MatcherResult?>> {
            if (matchResult == null) return mutableListOf()
            val resultData: MutableList<List<MatcherResult?>> = mutableListOf()
            val matcher = matchResult.accessField("matcher") as? Matcher

            if (matcher?.groupCount() == 0 && matcher.group(0) == "") {
                // regex match which allows an empty body
                resultData.add(listOf(MatcherResult {
                    it.groupIndex = 0
                    it.value = ""
                    it.range = (0..0)
                    it.litMatchCnt = 0
                }))
                return resultData
            }

            matcher?.also { mm ->
                val namedGroups = mm.pattern().accessField("namedGroups")
                    .tryCast<Map<String, Int>>().orEmpty()

                val toAddList = (0..mm.groupCount()).mapNotNull {
                    mm.group(it)
//                        .run { if (isNullOrEmpty()) null else this }
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
                resultData.add(toAddList)
            }

            return resultData
        }

        /**
         * Looks through [bundles] to any items which match [findBy]
         */
        fun get(
            matchesOnly: Boolean = false,
            bundles: MutableList<List<MatcherResult?>>,
            findBy: (MatcherResult) -> Boolean
        ): List<MatcherResult> {
            return bundles.asSequence()
                .flatMap { it.asSequence() } // all are equal
                .filterNotNull() // except the nulls
                .filter {
                    if (matchesOnly)
                        it.hasMatch
                    else true
                }
                .filter { findBy(it) }
                .toList()
        }

        fun finder(
            items: MutableList<List<MatcherResult?>>,
            group: String
        ): MatcherResult? {
            return get(bundles = items) { it.groupName == group }.firstOrNull()
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

    /**
     * Finds the highest matching index, and returns the last instance of this index.
     */
    fun lastIndexMatch(): MatcherResult {
        if (matchBundles.isEmpty())
            return MatcherResult.EMPTY

        var maxIndex = -1
        var maxValue: MatcherResult? = null
        get(true) {
            if (it.groupIndex >= maxIndex) {
                maxIndex = it.groupIndex
                maxValue = it
            }
            false
        }

        return maxValue ?: MatcherResult.EMPTY
    }

    // todo; override toString
}
