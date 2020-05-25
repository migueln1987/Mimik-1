package helpers.parser

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import helpers.firstNotNullResult
import helpers.matchers.MatcherCollection
import helpers.matchers.MatcherResult
import helpers.matchers.matchResults
import kolor.red
import networkRouting.testingManager.BoundChapterItem

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
                  (?<vbound>
                    (?:(?<vC>&)|(?<vB>%))?
                    (?<vU>\^)?
                  )?
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
         * 1) must start with a letter
         * 2) body can contain: letters, numbers, and underscores
         * 3) ending flags can be: ?, #, @, _?, _#, _\d
         */
        val varName
            get() = "[a-zA-Z]\\w*([?#@]*(?:_[\\d#?]*)?)"

        private val act
            get() = """
                (?<act>->
                  (?:
                    (?:\{(?<aM>.*?)\}(?=\s|$))|
                    (?<aV>
                      (?<aSVL>
                        (?<aSC>&)|(?<aSB>%)
                      )?
                      (?<aVN>[a-zA-Z]\w*[a-zA-Z0-9])
                      (?<aVT>
                        (?<aVE>\?)?(?<aVC>\#)?(?<aVR>@)?
                        (?<aVX>_(?:(?<aVS>\#(?<aVI>\d+)?)|(?<aVL>\?)))?
                      )?
                    )
                  )
                )?
                """
    }

    override fun toString() = Groups.makeScript()

    val asRegex by lazy { Groups.makeScript().toRegex() }

    /**
     * Extracts templates from a matcher strin
     */
    private const val templateReg = """@\{(.+?)\}"""

    /** Extracts the type of template from a template
     * - content = &?\w+
     * - final = '.+?' or ".+?"
     */
    private val variableMatch = "(%s|%s|%s)".format(
        "(?<index>\\d+)",
        "(?<content>${Groups.varName})",
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
        vars: (String, Int) -> String?
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
                    .firstNotNullResult {
                        val (temValue, scope) = it.value.orEmpty()
                            .let { v ->
                                Pair(
                                    v.trimStart('&', '%'),
                                    when (v[0]) {
                                        '&' -> 1 // Chapter
                                        '%' -> 2 // Test Bounds
                                        else -> 0 // Sequence
                                    }
                                )
                            }
                        // 'final', index, or var -> string or empty
                        if (it.groupName == "final")
                            temValue
                        else
                            vars.invoke(temValue, scope)
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
        return try {
            Gson()
                .fromJson(this, LinkedTreeMap::class.java).orEmpty()
                .map { it.key.toString() to it.value }
                .toMap()
        } catch (e: Exception) {
            e.printStackTrace()
            mutableMapOf()
        }
    }

    /**
     * Example
     * ```json
     * {
     *   "aaa":[
     *      [
     *          "?request:body:(some\w+)->toVar",
     *          "response:body:((test:))->(@{1}@{toVar})"
     *      ],
     *      '
     *      [
     *          "var:regCode->(true)",
     *          "response:body:(code: )-(code: @{regCode})"
     *      ]
     *   ]
     * }
     * ```
     */
    fun parseBody(body: String): MutableMap<String, BoundChapterItem> {
        return body.toJsonMap().asSequence()
            .map { (key, value) ->
                @Suppress("UNCHECKED_CAST")
                key to (value as? ArrayList<ArrayList<String>>).orEmpty()
            }
            .map { (chap, data) ->
                val actionItems = data.asSequence()
                    .map { dStr ->
                        // process the strings into lexicon bites
                        val parsedItems = dStr.asSequence()
                            .map(this::parseToContents)
                            .filter(this::isValid)
                            .toList()

                        // If any of the string lines were invalid
                        // then reject the whole array to be safe.
                        if (parsedItems.size == dStr.size)
                            parsedItems
                        else listOf()
                    }
                    .filter { it.isNotEmpty() }
                    .map { lexM ->
                        lexM.map { P4Command(it) }
                    }
                    .toList()

                val chapItems = BoundChapterItem().also {
                    it.seqSteps.clear()
                    it.seqSteps.addAll(actionItems)
                }

                chap to chapItems
            }
            .toMap().toMutableMap()
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
    fun parseToSteps(input: String): P4Command {
        val cmd = P4Command(parseToContents(input))
        val cmdStr = cmd.toString()
        if (cmdStr != input && cmdStr != "Invalid") {
            val sb = StringBuilder()
                .appendln("== Parsed P4Command != input".red())
                .appendln("== Input:  $input".red())
                .appendln("== Parsed: $cmd".red())
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
