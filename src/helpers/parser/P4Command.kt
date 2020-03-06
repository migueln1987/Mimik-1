package helpers.parser

import helpers.matchers.MatcherResult

class P4Command {
    val isCond
        get() = cStSrc >= 0
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
     * State Search flag; What kind of state to look for.
     *
     * If (state != stSrc), action is not called
     * - -1: unset
     * - 0: optional condition only
     * - 1: True (?)
     * - 2: false (!)
     */
    var cStSrc = -1

    /**
     * Source is a Request or Response
     */
    val isType_R
        get() = isRequest or isResponse

    /**
     * Source is a variable
     */
    var isType_V = false

    /**
     * Source is the `use` field
     */
    var isType_U = false
    /**
     * True: wanting to set `use` on another chapter
     */
    private var use_ext = false // todo; implement

    var isRequest = false
    var isResponse = false
    var isHead = false
    var isBody = false

    /**
     * True: looking for a item or match in the source
     */
    val source_HasSubItem
        get() = (source_name != null) or (source_match != null)
    var source_name: String? = null
    var source_match: String? = null
    /**
     * - 0: any (scoped then bound)
     * - 1: scoped (limited to current steps)
     * - 2: bounds (shared across active test bound)
     */
    var varType = 0

    /**
     * This seqStep has an action
     */
    val hasAction
        get() = (act_name != null) or (act_match != null)

    var act_nameScoped = false
    var act_name: String? = null
    var act_match: String? = null

    private fun List<MatcherResult>.find(field: String): MatcherResult? =
        firstOrNull { it.groupName == field }

    /**
     * Constructs a SeqStep with all options turned off
     */
    constructor()

    /**
     * Constructs a SeqStep using [items].
     *
     * - [items].isEmpty = all options turned off
     * - no `source` = all options turned off
     */
    constructor(items: List<MatcherResult>) {
        if (items.isEmpty() || items.find("source") == null)
            return

        if (items.find("cond") != null) {
            if (items.find("cP") != null)
                cStOpt = true

            cStSrc = when {
                items.find("cT") != null -> 1
                items.find("cF") != null -> 2
                cStOpt -> 0
                else -> -1
            }
        }

        if (items.find("rType") != null) {
            isRequest = items.find("rIn") != null
            if (isRequest) {
                isHead = items.find("rInH") != null
                if (isHead)
                    source_name = items.find("rInHN")?.value
                else
                    isBody = items.find("rInB") != null
            } else {
                isResponse = items.find("rOut") != null
                if (isResponse) {
                    isHead = items.find("rOutH") != null
                    if (isHead)
                        source_name = items.find("rOutHN")?.value
                    else
                        isBody = items.find("rOutB") != null
                }
            }
            source_match = items.find("rM")?.value
        } else {
            isType_V = items.find("vType") != null

            if (isType_V) {
                varType = when {
                    items.find("vS") != null -> 1
                    items.find("vB") != null -> 2
                    else -> 0
                }
                source_name = items.find("vN")?.value
                source_match = items.find("vM")?.value
            } else {
                isType_U = items.find("uType") != null
                if (isType_U) {
                    source_name = items.find("uN")?.value
                    source_match = items.find("uM")?.value
                }
            }
        }

        if (items.find("act") != null) {
            if (items.find("aV") != null) {
                act_nameScoped = items.find("aVS") != null
                act_name = items.find("aVN")?.value
            } else {
                act_match = items.find("aM")?.value
            }
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        if (isCond) {
            if (cStOpt)
                sb.append("~")
            when (cStSrc) {
                1 -> sb.append("?")
                2 -> sb.append("!")
            }
        }

        if (isType_R) {
            if (isRequest)
                sb.append("request:")
            else if (isResponse)
                sb.append("response:")
            if (isHead)
                sb.append("head")
            else if (isBody)
                sb.append("body")
        } else if (isType_V) {
            when (varType) {
                0 -> sb.append("var")
                1 -> sb.append("&var")
                2 -> sb.append("bvar")
            }
        } else if (isType_U) {
            sb.append("use")
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
                if (act_nameScoped)
                    sb.append("&")
                sb.append(act_name)
            } else if (act_match != null) {
                sb.append("{").append(act_match).append("}")
            }
        }

        return sb.toString()
    }
}
