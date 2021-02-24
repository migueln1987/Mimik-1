package mimik.helpers.parser

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import kolor.red
import kotlinUtils.collections.toArrayList
import kotlinUtils.isNotNull
import kotlinUtils.tryCast
import kotlinUtils.tryOrNull
import mimik.helpers.firstNotNullResult
import mimik.helpers.matchers.*
import mimik.mockHelpers.SeqActionObject
import mimik.networkRouting.testingManager.BoundChapterItem

/**
 * Example
 * ```json
 * {
 *   "aaa":[
 *      [
 *          "?request:body:(some\w+)->toVar",
 *          "response:body:((test:))->(@{1}@{toVar})"
 *      ],
 *      [
 *          "var:regCode->(true)",
 *          "response:body:(code: )-(code: @{regCode})"
 *      ]
 *   ]
 * }
 * ```
 */
object Parser_v4 {
    /**
     * cond: is this a conditional
     * source: where to do actions on
     * - rType: Request/ Response sources
     * -- rIn: Request
     * --- rInH: head
     * ---- rInHN: head value
     * --- rInB: body
     * -- rOut: Response
     * --- rOutH: head
     * ---- rOutHN: head value
     * --- rOutB: body
     * - rM: match request
     * - vType: Variable
     * -- vN: name
     * -- vM: match
     * - uType: Uses
     * -- uM: match
     * act
     * - aM: match
     * - aM: value (var name)
     *
     */

    private object Groups {
        fun makeScript() =
            (cond + source + act)
                .replace("  ", "").replace("\n", "")

        // continue searching till we hit the end of the line or action
        private val toEOM
            get() = """(?=->|\s|$)"""

        private val toEOA
            get() = """(?=\s|$)"""

        private val cond
            get() = """
                (?<cond>
                  (?=[~?!])
                  (?<cP>~)?
                  (?:(?<cT>\?)|(?<cF>!))?
                )?
            """

        private val source
            get() = """
                (?<source>
                  $rType|$vType|$uType
                )
            """

        private val rType
            get() = """
                (?<rType>
                  (?:$rIn|$rOut)
                  $match
                )
            """

        private val rIn
            get() = """
                (?<rIn>request:(?:
                  (?<rInH>head(?:\[(?<rInHN>\w+)\])?)|
                  (?<rInB>body))
                )
            """

        private val rOut
            get() = """
                (?<rOut>response:(?:
                  (?<rOutH>head(?:\[(?<rOutHN>\w+)\])?)|
                  (?<rOutB>body))
                )
            """

        private val match
            get() = """
                (?::\{
                  (?<rM>.*?)
                \}$toEOM)?
            """

        private val vType
            get() = """
                (?<vType>
                  (?:
                    (?=[&%\^])
                    (?<vbound>
                      (?:(?<vC>&)|(?<vB>%))?
                      (?<vU>\^)?
                    )|(?:)
                  )
                  var
                  (?:\[(?<vN>$varName)\])?
                  (?::\{(?<vM>.+?)\}$toEOM)?
                )
            """

        private val uType
            get() = """
                (?<uType>use
                  (?:\[(?<uN>[a-zA-Z][!-+\--~]*)\])?
                  (?::\{(?<uM>[\d=,.<>]+)\})?
                )
            """

        /**
         * Valid variable names (+ suffix flags)
         * 1) content must start with a letter
         * 2) body can contain: letters, numbers, and underscores
         * 3) ending flags can be: ?, #, @, _?, _#, _\d
         */
        val varName
            get() = "[a-zA-Z](?:\\w+|(?:[?#@]*(?:_[\\d#?]*)?))*"

        private val act
            get() = """
                (?<act>->
                  (?:$act_match|$act_var)
                )?
                """

        private val act_match
            get() = """
                (?:\{(?<aM>.*?)\}(?=\s|$))
            """

        private val act_var
            get() = """
                (?<aV>
                (?<aSVL>
                  (?<aSC>&)|(?<aSB>%)
                )?
                (?<aVN>[a-zA-Z]\w*[a-zA-Z0-9])
                (?:
                  (?<aVT>
                    (?<aVE>\?)?
                    (?<aVC>\#)?
                    (?<aVR>@)?
                    (?<aVS>_(?:(?<aVSA>\#(?<aVSI>\d+)?)|(?<aVSL>\?)))?
                  )
                )
              )
        """
    }

    override fun toString() = Groups.makeScript()

    val asRegex by lazy { Groups.makeScript().toRegex() }

    /**
     * Extracts templates from a matcher string
     */
    private const val templateReg = """@\{(.+?)\}"""

    /** Extracts the type of template from a template
     * - content = &?\w+
     * - final = '.+?' or ".+?"
     */
    private val variableMatch = "%s|%s|%s".format(
        "(?<index>\\d+)",
        "(?<content>[&%^]*${Groups.varName})",
        "(?<final>['\"].+?['\"])"
    )

    /**
     * Translates template items '@{...}' into resolved strings.
     *
     * @param finder String to deTemplate
     * @param vars (variable to look for, search Scoped only)
     */
    fun deTemplate(
        finder: String,
        vars: (String, Int, Boolean) -> String?
    ): String {
        // todo; update the Actions to reflect the updated deTemplating actions
        /* Actions:
    1. determine if (finder) is a template
    1.a is a template
    -> result becomes (templateMatches)

    1.a.1 (variableMatch) collects the results of (templateMatches)[1].sortedByDescending
    -> each result becomes (templateMatch)
    -> (temRootRange) = indexOf(item) or (templateMatch)[0]

    1.a.2 loop through the items of (templateMatch) by group name
    - process/ use only the items which are valid
    1.a.2.a empty; skip ths item
    1.a.2.b "content"; use as is
    1.a.2.c "final"; remove padding quotes and use data
    1.a.2.d other; TBD, use as is for now

    1.a.3 Process the valid group items
    1.a.3.a group name is "final"
    ->> (value to range to replace)

    1.a.3.b item is a template to be processed
    ~> below processing (1-3) becomes (result)
    1.a.3.b.1 collect vars and process the item
    --> (temValue) = index/ group name/ bounds variable

    1.a.3.b.1.a (temValue) is an index
    1.a.3.b.1.a.1 (replacer) must contain a index of (content index)
    -> (temRepValue) = (replacer)[temValue as index]

    1.a.3.b.1.b check if (replacer) contains [temValue] - as it's closer relative
    -> (temRepValue) = (replacer)[temValue]

    1.a.3.b.1.c check if (boundVars) contains (content key)
    -> (temRepValue) = boundVars[temValue]

    1.a.3.b.2 (temRepValue) is not null
    ->> (temRepValue to range to replace)

2.a.4.b.3 above result is null, no matches or "finals" were found
->> (empty string to range to replace)




    1.b is not a template
    */

        var repToOut = finder

        val templateMatches = templateReg.matchResults(finder)
        if (templateMatches.hasMatches) {
            templateMatches[1].sortedByDescending { it.range.first }.forEach { temMatch ->
                val template = variableMatch.matchResults(temMatch.value)
                val temRootRange = templateMatches.rootIndexOf(temMatch)!!.range

                val temReplace = template.asSequence()
                    .mapNotNull {
                        when (it.groupName) {
                            "" -> null
                            "index", "content" -> it
                            "final" -> { // remove bounding quotes (single or double)
                                it.clone { b ->
                                    b.value = b.value!!.drop(1).dropLast(1)
                                }
                            }
                            else -> it
                        }
                    }
                    .firstNotNullResult { mResult ->
                        val (temValue, scope, searchUp) = mResult.value.orEmpty()
                            .let { v ->
                                Triple(
                                    v.trimStart('&', '%', '^'),
                                    when (v[0]) {
                                        '&' -> 1 // Chapter
                                        '%' -> 2 // Test Bounds
                                        else -> 0 // Sequence
                                    },
                                    v.contains('^')
                                )
                            }
                        // 'final', index, or var -> string or empty
                        if (mResult.groupName == "final")
                            temValue
                        else {
                            vars.invoke(temValue, scope, searchUp)
                                ?.run { if (isEmpty()) null else this }
                        }
                    }
                    .orEmpty()

                repToOut = repToOut.replaceRange(
                    temRootRange.first,
                    temRootRange.last,
                    temReplace
                )
            }
        }
        return repToOut
    }

    private fun String.toJsonMap(): Map<String, Any> {
        if (isEmpty()) return mapOf()
        return tryOrNull(true) {
            Gson()
                .fromJson(this, LinkedTreeMap::class.java).orEmpty()
                .map { it.key.toString() to it.value }
                .toMap()
        }.orEmpty()
    }

    /**
     * Example
     * ```json
     * {
     *   "aa": {
     *     "stateUse": 4,
     *     "seqSteps": [
     *       {
     *         "Name": "something",
     *         "Commands": [
     *           "?request:body:(some\w+)->toVar",
     *           "response:body:((test:))->(@{1}@{toVar})"
     *         ]
     *       },
     *       {
     *         "Name": "other",
     *         "Commands": [
     *           "var:regCode->(true)",
     *           "esponse:body:(code: )-(code: @{regCode})"
     *         ]
     *       }
     *     ],
     *     "scopeVars": {
     *       "g": "y",
     *       "j": "e"
     *     }
     *   }
     * }
     * ```
     */
    fun parseBody(body: String): MutableMap<String, BoundChapterItem> {
        if (body.isEmpty()) return mutableMapOf()

        val jsonData = body.toJsonMap()
        val validData = jsonData.values.all { it.tryCast<LinkedTreeMap<String, *>>().isNotNull() }
        if (!validData) {
            println("Body is not properly formatted")
            return mutableMapOf()
        }

        return jsonData.asSequence()
            .map { (chapName, value) ->
                chapName to value.tryCast<LinkedTreeMap<String, *>>().orEmpty()
            }
            .map { (chap, values) ->
                chap to BoundChapterItem().also { bounds ->
                    values.forEach { (key, data) ->
                        when (key) {
                            "stateUse" -> bounds.stateUse = (data as Double).toInt()

                            "seqSteps" -> {
                                data.tryCast<ArrayList<LinkedTreeMap<String, *>>>().orEmpty().map { lData ->
                                    SeqActionObject(true) { newSeq ->
                                        lData.forEach { (dKey, dData) ->
                                            when (dKey) {
                                                "ID" -> newSeq.ID = (dData as Double).toInt()
                                                "Name" -> newSeq.Name = dData as String
                                                "Commands" -> newSeq.Commands = dData.tryCast<ArrayList<String>>()
                                                    .orEmpty().map { parseToCommand(it) }.toArrayList()
                                            }
                                        }
                                    }
                                }.also { bounds.seqSteps.addAll(it) }
                            }

                            "scopeVars" -> bounds.scopeVars =
                                data.tryCast<LinkedTreeMap<String, String>>().orEmpty().toMutableMap()
                        }
                    }
                }
            }.toMap().toMutableMap()
    }

    /**
     * Parses [input] to only the groups which are named
     */
    fun parseToContents(input: String): List<MatcherResult> {
        return MatcherCollection.castResult(asRegex.find(input)).asSequence()
            .flatten()
            .filterNotNull()
            .filter { it.groupName.isNotEmpty() }
            .toList()
    }

    /**
     * Parses an [input] to a [P4Command].
     *
     * Invalid [input]s will return a [P4Command] with all options turned off
     */
    fun parseToCommand(input: String): P4Command {
        val cmd = P4Command(parseToContents(input))
        val cmdStr = cmd.toString()
        if (cmdStr != input && cmdStr != "Invalid") {
            val sb = StringBuilder()
                .appendLine("== Parsed P4Command != input".red())
                .appendLine("== Input:  $input".red())
                .appendLine("== Parsed: $cmd".red())
            println(sb.toString())
        }
        return cmd
    }

    fun isValid_Request(action: List<MatcherResult>): Boolean {
        fun find(name: String): Boolean =
            action.any { it.groupName == name }

        return find("cond") || find("act")
    }

    fun isValid_Syntax(action: List<MatcherResult>): Boolean {
        val findNames = action.map { it.groupName }
        fun find(name: String) = findNames.contains(name)

        if (!find("source")) return false

        val isCond = find("cond")
        var hasItem = false
        var isValid = true
        var canWriteToSource = true

        when {
            find("rType") -> {
                when {
                    find("rIn") -> {
                        canWriteToSource = false
                        when {
                            find("rInH") -> {
                                if (find("rInHN"))
                                    hasItem = true
                                else
                                    canWriteToSource = false
                            }
                            find("rInB") -> hasItem = true
                            else -> isValid = false
                        }
                    }

                    find("rOut") -> {
                        when {
                            find("rOutH") -> {
                                if (find("rOutHN"))
                                    hasItem = true
                                if (find("rM"))
                                    hasItem = true
                            }
                            find("rOutB") -> hasItem = true
                            else -> isValid = false
                        }
                    }
                    else -> isValid = false
                }
            }

            find("vType") -> {
                var hasVContent = false
                if (find("vN")) {
                    hasItem = true
                    hasVContent = true
                }

                if (find("vM")) {
                    if (!hasVContent)
                        canWriteToSource = false
                    hasVContent = true
                }
                if (!hasVContent)
                    canWriteToSource = false
            }

            find("uType") ->
                hasItem = true

            else -> isValid = false
        }

        if (find("act")) {
            when {
                find("aM") -> {
                    if (!hasItem or !canWriteToSource)
                        isValid = false
                }
                find("aV") -> Unit
                else -> isValid = false
            }
        } else if (!isCond)
            isValid = false

        return isValid
    }

    fun isValid(action: List<MatcherResult>) =
        isValid_Syntax(action) && isValid_Request(action)
}
