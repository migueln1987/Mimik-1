package helpers.parser

import helpers.RandomHost
import helpers.isNotNull
import helpers.isNull
import helpers.matchers.MatcherResult
import helpers.toBase64
import kotlin.random.Random

@Suppress("PropertyName", "MemberVisibilityCanBePrivate")
class P4Command {

    var isValid = false

    /**
     * Returns [true] if this [P4Command] does an action or affects another [P4Command]
     */
    val hasResults: Boolean
        get() = isCond || hasAction

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
        get() = source_name.isNotNull() or source_match.isNotNull()
    var source_name: String? = null
    var source_match: String? = null

    /**
     * This seqStep has an action
     */
    val hasAction
        get() = act_name.isNotNull() or act_match.isNotNull()

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

    private fun List<MatcherResult>.contains(token: ParserTokens): Boolean = contains(token.flag)

    private fun List<MatcherResult>.contains(field: String): Boolean =
        firstOrNull { it.groupName == field }.isNotNull()

    private fun List<MatcherResult>.find(token: ParserTokens): MatcherResult? = find(token.flag)

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
        if (items.isEmpty() || items.find(ParserTokens.source).isNull())
            return
        isValid = true

        if (items.contains(ParserTokens.cond)) {
            if (items.contains(ParserTokens.cond_opt))
                cStOpt = true

            cStSrc = when {
                items.contains(ParserTokens.cond_true) -> 1
                items.contains(ParserTokens.cond_false) -> 2
                cStOpt -> 0
                else -> -1
            }
        }

        when {
            items.contains(ParserTokens.type_RX) -> {
                when {
                    items.contains(ParserTokens.rx_In) -> {
                        isRequest = true
                        isHead = items.contains(ParserTokens.req_Header)
                        if (isHead)
                            source_name = items.find(ParserTokens.req_HeadName)?.value
                        else
                            isBody = items.contains(ParserTokens.req_Body)
                    }

                    items.contains(ParserTokens.rx_Out) -> {
                        isResponse = true
                        isHead = items.contains(ParserTokens.res_Header)
                        if (isHead)
                            source_name = items.find(ParserTokens.res_HeadName)?.value
                        else
                            isBody = items.contains(ParserTokens.res_Body)
                    }

                    else -> {
                        isValid = false
                        return
                    }
                }

                source_match = items.find(ParserTokens.rx_Match)?.value
            }

            items.contains(ParserTokens.type_Var) -> {
                varLevel = when {
                    items.contains(ParserTokens.var_Chap) -> 1 // Chapter
                    items.contains(ParserTokens.var_Bounds) -> 2 // test Bounds
                    else -> 0 // sequence
                }
                varSearchUp = items.contains(ParserTokens.var_UpSrc)
                source_name = items.find(ParserTokens.var_Name)?.value
                source_match = items.find(ParserTokens.var_Match)?.value
            }

            items.contains(ParserTokens.type_Use) -> {
                isType_U = true
                if (isType_U) {
                    source_name = items.find(ParserTokens.use_Name)?.value
                    source_match = items.find(ParserTokens.use_Match)?.value
                }
            }

            else -> {
                isValid = false
                return
            }
        }

        if (items.contains(ParserTokens.action)) {
            if (items.contains(ParserTokens.act_var)) {
                act_scopeLevel = when {
                    items.contains(ParserTokens.act_lvChap) -> 1
                    items.contains(ParserTokens.act_lvBound) -> 2
                    else -> 0
                }
                act_name = items.find(ParserTokens.act_Name)?.value
                if (items.contains(ParserTokens.act_VarTypes)) {
                    act_nExists = items.contains(ParserTokens.act_Exist)
                    act_nCount = items.contains(ParserTokens.act_Count)
                    act_nResult = items.contains(ParserTokens.act_CondRst)
                    if (items.contains(ParserTokens.act_IdxSprd)) {
                        act_nSpread = true
                        act_nSpreadType = when {
                            // keep specific index (or last)
                            items.contains(ParserTokens.act_IdxSI) ->
                                items.find(ParserTokens.act_IdxSI)?.value?.toIntOrNull()
                                    ?: -2
                            // spread all
                            items.contains(ParserTokens.act_IdxSAll) -> -1
                            // keep last index only
                            items.contains(ParserTokens.act_IdxSL) -> -2
                            // do nothing
                            else -> -8
                        }
                    }
                }
            } else {
                act_match = items.find(ParserTokens.act_Match)?.value
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
            if (source_name.isNotNull())
                sb.append("[").append(source_name).append("]")
            if (source_match.isNotNull())
                sb.append(":{").append(source_match).append("}")
        }

        if (hasAction) {
            sb.append("->")
            if (act_name.isNotNull()) {
                when (act_scopeLevel) {
                    0 -> Unit
                    1 -> sb.append("&")
                    2 -> sb.append("%")
                }
                sb.append(act_name)
                if (act_nExists) sb.append("?")
                if (act_nCount) sb.append("#")
                if (act_nResult) sb.append("@")
                if (act_nSpreadType != -8) {
                    when (act_nSpreadType) {
                        -1 -> sb.append("_#")
                        -2 -> sb.append("_?")
                    }
                    if (act_nSpreadType >= 0)
                        sb.append("_#$act_nSpreadType")
                }
            } else if (act_match.isNotNull()) {
                sb.append("{").append(act_match).append("}")
            }
        }

        return sb.toString()
    }

    /**
     * Wraps this string in a quotes, or returns an empty if [this] is null
     */
    private val String?.jStr: String
        get() = if (this.isNullOrEmpty()) "\"\"" else "\"$this\""

    val asJSObject: String
        get() {
            val srcRTypeInt = when {
                isHead -> 1
                isBody -> 2
                else -> 0
            }
            return """
            {
                "isValid": $isValid,
                
                "isCond": $isCond,
                "isOpt": $cStOpt,
                "condSrc": $cStSrc,
                
                "srcType": $srcType,
                "srcRType": $srcRTypeInt,
                "isHead": $isHead,
                "isBody": $isBody,
                "varLevel": $varLevel,
                "varSearchUp": $varSearchUp,
            
                "source_name": ${source_name.toBase64.jStr},
                "source_match": ${source_match.toBase64.jStr},
            
                "hasAction": $hasAction,
                "act_scopeLevel": $act_scopeLevel,
                "act_name": ${act_name.toBase64.jStr},
                "act_nExists": $act_nExists,
                "act_nCount": $act_nCount,
                "act_nResult": $act_nResult,
                "act_nSpread": $act_nSpread,
                "act_nSpreadType": $act_nSpreadType,
                "act_match": ${act_match.toBase64.jStr}
            }
            """.trimIndent()
        }

    fun jumble(): P4Command {
        isValid = true
        cStOpt = Random.nextBoolean()
        cStSrc = Random.nextInt(-1, 3)

        isRequest = false
        isResponse = false
        when (Random.nextInt(1, 5)) {
            0 -> Unit // Unset or none
            1 -> isRequest = true
            2 -> isResponse = true
            3 -> varLevel = Random.nextInt(0, 3)
            4 -> isType_U = true
        }

        if (isRequest || isResponse) {
            when (Random.nextInt(1, 3)) {
                0 -> Unit // unset
                1 -> isHead = true
                2 -> isBody = true
            }
        }

        if (varLevel > -1)
            varSearchUp = Random.nextBoolean()

        fun randomVarName(UseSymbols: Boolean = false): String {
            return RandomHost().valueToValid {
                if (UseSymbols)
                    it.add(RandomHost.pool_LetterNumSymbols to Random.nextInt(1, 5))
                else {
                    it.add(RandomHost.pool_Letters to 1)
                    it.add(RandomHost.pool_LetterNum to Random.nextInt(0, 5))
                    it.add(RandomHost.pool_LetterNum to Random.nextInt(1, 5))
                }
            }
        }

        if (isRequest || isResponse || varLevel > -1 || isType_U) {
            var subSource_IsName = Random.nextBoolean()
            if (isBody) subSource_IsName = false

            if (subSource_IsName)
                source_name = randomVarName()
            else
                source_match = if (isType_U)
                    randomVarName(true) else RandomHost().valueAsChars()
        }

        when (Random.nextInt(0, 3)) {
            0 -> Unit
            1 -> {
                act_scopeLevel = Random.nextInt(-1, 3)
                act_name = randomVarName()
                act_nExists = Random.nextBoolean()
                act_nCount = Random.nextBoolean()
                act_nResult = Random.nextBoolean()
                act_nSpread = Random.nextBoolean()
                act_nSpreadType = if (Random.nextBoolean())
                    Random.nextInt(-2, 21) else -8
            }
            2 -> act_match = if (isType_U)
                randomVarName(true) else RandomHost().valueAsChars()
        }

        return this
    }
}
