package helpers.parser

import helpers.RandomHost
import helpers.matchers.MatcherResult
import kotlin.random.Random

@Suppress("PropertyName", "MemberVisibilityCanBePrivate")
class P4Command {

    var isValid = false

    /**
     * True: wanting to set `use` on another chapter
     */
    private var use_ext = false // todo; implement

    // == Conditionals ==
    /**
     * Optional condition flag
     *
     * == Using on State Search flags
     * - True: StSrc -> fails, reject act but continue the sequence
     * - False: StSrc -> successful, or reject act + following commands
     *
     * == By itself
     * - Note: Must be after a type of state search flag
     * - True: StSrc -> successful, this will also run
     * - False: StSrc -> fails, this will be skipped
     */
    var cStOpt = false

    /**
     * True: command has a conditional set
     */
    val isCond
        get() = cStSrc >= 0

    /**
     * State Search flag; What kind of state to look for.
     *
     * If (state != stSrc), action is not called
     * - -1: unset
     * - 0: optional condition only
     * - 1: True (?)
     * - 2: false (!)
     */
    var cStSrc = -1

    // == Sources ==
    // === Source root types ===
    /**
     * Type of source:<pre>
     *   - 0: Unset or none
     *   - 1: Request
     *   - 2: Response
     *   - 3: Variable
     *   - 4: Uses
     * </pre>
     */
    val srcType: Int
        get() {
            return when {
                isRequest -> 1
                isResponse -> 2
                isType_V -> 3
                isType_U -> 4
                else -> 0
            }
        }

    /**
     * Source is a Request or Response
     */
    val isType_R
        get() = isRequest or isResponse
    var isRequest = false
    var isResponse = false

    /**
     * Source is a variable
     */
    val isType_V: Boolean
        get() = varLevel > -1

    /**
     * Source is the `use` field
     */
    var isType_U = false

    // === Source root sub-types ===
    var isHead = false
    var isBody = false

    /**
     * Variable scope type
     * - (-1): unset
     * - 0: Sequence
     * - 1: Chapter
     * - 2: Test Bounds
     */
    var varLevel = -1

    /**
     * True: Will search the current scope, then hierarchy up
     */
    var varSearchUp = false

    // === Source content ===
    /**
     * True: looking for a item or match in the source
     */
    val source_HasSubItem
        get() = (source_name != null) or (source_match != null)
    var source_name: String? = null
    var source_match: String? = null

    /**
     * This seqStep has an action
     */
    val hasAction
        get() = (act_name != null) or (act_match != null)

    /**
     * Variable scope type
     * - (-1): unset
     * - 0: Sequence
     * - 1: Chapter
     * - 2: Test Bounds
     */
    var act_scopeLevel = -1
    var act_name: String? = null

    var act_nExists = false
    var act_nCount = false
    var act_nResult = false
    var act_nSpread = false

    /**
     * How the data will be spread
     * - (-1): all indexes
     * - (-2): last index
     * - (-8): unset
     * - (0...xxx): by specific index
     */
    var act_nSpreadType = -8
    var act_match: String? = null

    private fun List<MatcherResult>.contains(field: String): Boolean =
        firstOrNull { it.groupName == field } != null

    private fun List<MatcherResult>.find(field: String): MatcherResult? =
        firstOrNull { it.groupName == field }

    /**
     * Constructs a SeqStep with all options turned off.
     *
     * @param setup Optional lambda config of this object
     */
    constructor(setup: (P4Command) -> Unit = {}) {
        setup.invoke(this)
    }

    /**
     * Constructs a SeqStep using [items].
     *
     * - [items].isEmpty = all options turned off
     * - no `source` = all options turned off
     */
    constructor(items: List<MatcherResult>) {
        if (items.isEmpty() || items.find("source") == null)
            return
        isValid = true

        if (items.contains("cond")) {
            if (items.contains("cP"))
                cStOpt = true

            cStSrc = when {
                items.contains("cT") -> 1
                items.contains("cF") -> 2
                cStOpt -> 0
                else -> -1
            }
        }

        when {
            items.contains("rType") -> {
                when {
                    items.contains("rIn") -> {
                        isRequest = true
                        isHead = items.contains("rInH")
                        if (isHead)
                            source_name = items.find("rInHN")?.value
                        else
                            isBody = items.contains("rInB")
                    }

                    items.contains("rOut") -> {
                        isResponse = true
                        isHead = items.contains("rOutH")
                        if (isHead)
                            source_name = items.find("rOutHN")?.value
                        else
                            isBody = items.contains("rOutB")
                    }

                    else -> {
                        isValid = false
                        return
                    }
                }

                source_match = items.find("rM")?.value
            }

            items.contains("vType") -> {
                varLevel = when {
                    items.contains("vC") -> 1 // Chapter
                    items.contains("vB") -> 2 // test Bounds
                    else -> 0 // sequence
                }
                varSearchUp = items.contains("vU")
                source_name = items.find("vN")?.value
                source_match = items.find("vM")?.value
            }

            items.contains("uType") -> {
                isType_U = true
                if (isType_U) {
                    source_name = items.find("uN")?.value
                    source_match = items.find("uM")?.value
                }
            }

            else -> {
                isValid = false
                return
            }
        }

        if (items.contains("act")) {
            if (items.contains("aV")) {
                act_scopeLevel = when {
                    items.contains("aSC") -> 1
                    items.contains("aSB") -> 2
                    else -> 0
                }
                act_name = items.find("aVN")?.value
                if (items.contains("aVT")) {
                    act_nExists = items.contains("aVE")
                    act_nCount = items.contains("aVC")
                    act_nResult = items.contains("aVR")
                    if (items.contains("aVX")) {
                        act_nSpread = true
                        act_nSpreadType = when {
                            // keep specific index (or last)
                            items.contains("aVI") ->
                                items.find("aVI")?.value?.toIntOrNull()
                                    ?: -2
                            // spread all
                            items.contains("aVS") -> -1
                            // keep last index only
                            items.contains("aVL") -> -2
                            // do nothing
                            else -> -8
                        }
                    }
                }
            } else {
                act_match = items.find("aM")?.value
            }
        }
    }

    override fun toString(): String {
        if (!isValid) return "Invalid"

        val sb = StringBuilder()
        if (isCond) {
            if (cStOpt)
                sb.append("~")
            when (cStSrc) {
                1 -> sb.append("?")
                2 -> sb.append("!")
            }
        }

        when (srcType) {
            // request/ response
            1, 2 -> {
                when {
                    isRequest -> sb.append("request:")
                    isResponse -> sb.append("response:")
                }

                when {
                    isHead -> sb.append("head")
                    isBody -> sb.append("body")
                }
            }

            // Variable
            3 -> {
                when (varLevel) {
                    0 -> Unit
                    1 -> sb.append("&")
                    2 -> sb.append("%")
                }
                if (varSearchUp)
                    sb.append("^")
                sb.append("var")
            }

            // Uses
            4 -> sb.append("use")
        }

        if (source_HasSubItem) {
            if (source_name != null)
                sb.append("[").append(source_name).append("]")
            if (source_match != null)
                sb.append(":{").append(source_match).append("}")
        }

        if (hasAction) {
            sb.append("->")
            if (act_name != null) {
                when (act_scopeLevel) {
                    0 -> Unit
                    1 -> sb.append("&")
                    2 -> sb.append("%")
                }
                sb.append(act_name)
                if (act_nExists) sb.append("?")
                if (act_nCount) sb.append("#")
                if (act_nResult) sb.append("@")
                if (act_nSpread) {
                    when (act_nSpreadType) {
                        -1 -> sb.append("_#")
                        -2 -> sb.append("_?")
                    }
                    if (act_nSpreadType >= 0)
                        sb.append("_#$act_nSpreadType")
                }
            } else if (act_match != null) {
                sb.append("{").append(act_match).append("}")
            }
        }

        return sb.toString()
    }

    /**
     * Wraps this string in a single-quotes, or returns `'null'` if [this] is null
     */
    private val String?.jStr: String
        get() = this?.let { "\'$it\'" } ?: "null"

    val asJSObject: String
        get() {
            return """
            {
                isValid: $isValid,
                
                isCond: $isCond,
                isOpt: $cStOpt,
                condSrc: $cStSrc,
                
                srcType: $srcType,
                isHead: $isHead,
                isBody: $isBody,
                varLevel: $varLevel,
                varSearchUp: $varSearchUp
            
                source_hasItems: $source_HasSubItem,
                source_name: ${source_name.jStr},
                source_match: ${source_match.jStr},
            
                act_hasItem: $hasAction,
                act_name: ${act_name.jStr},
                act_nExists: $act_nExists
                act_nCount: $act_nCount
                act_nResult: $act_nResult
                act_nSpread: $act_nSpread
                act_nSpreadType: $act_nSpreadType
                act_scopeLevel: $act_scopeLevel,
                act_match: ${act_match.jStr}
            }
            """.trimIndent()
        }

    fun jumble(): P4Command {
        isValid = true
        cStOpt = Random.nextBoolean()
        cStSrc = Random.nextInt(-1, 3)
        isRequest = Random.nextBoolean()
        isResponse = Random.nextBoolean()
        varLevel = Random.nextInt(0, 3)
        varSearchUp = Random.nextBoolean()
        isType_U = Random.nextBoolean()
        isHead = Random.nextBoolean()
        isBody = Random.nextBoolean()

        source_name = if (Random.nextBoolean())
            RandomHost().valueAsChars()
        else null

        source_match = if (Random.nextBoolean())
            RandomHost().valueAsChars()
        else null

        act_scopeLevel = Random.nextInt(-1, 3)
        act_name = if (Random.nextBoolean())
            RandomHost().valueAsChars()
        else null
        act_nExists = Random.nextBoolean()
        act_nCount = Random.nextBoolean()
        act_nResult = Random.nextBoolean()
        act_nSpread = Random.nextBoolean()
        act_nSpreadType = if (Random.nextBoolean())
            Random.nextInt(-2, 20) else -8
        act_match = if (Random.nextBoolean())
            RandomHost().valueAsChars()
        else null
        return this
    }
}
