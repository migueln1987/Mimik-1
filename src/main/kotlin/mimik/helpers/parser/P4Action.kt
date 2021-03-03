package mimik.helpers.parser

import kotlinUtils.collections.firstNotNullResult
import kotlinUtils.collections.hasIndex
import kotlinUtils.collections.lastNotNullResult
import kotlinUtils.collections.putIfAbsent
import mimik.helpers.matchers.MatcherCollection
import mimik.helpers.matchers.MatcherResult
import mimik.helpers.matchers.matchResults
import mimik.mockHelpers.MockUseStates
import mimik.networkRouting.testingManager.BoundChapterItem
import mimik.networkRouting.testingManager.TestBounds
import okhttp3.Headers
import okhttp3.names
import okhttp3.toHeaders_dupKeys

/**
 * Class which processes the [P4Command]s
 */
@Suppress("PropertyName", "FunctionName", "LocalVariableName")
class P4Action(config: (P4Action) -> Unit = {}) {
    lateinit var testBounds: TestBounds
    lateinit var chapBounds: BoundChapterItem
    lateinit var in_headers: Headers
    lateinit var in_body: String
    lateinit var out_headers: Headers
    lateinit var out_body: String
    private val varSuffixes = """(\w+?)([?#]*(?:_(?:#\d+|[?#]))?)""".toRegex()

    private enum class DataType {
        /** No data set */
        None,

        /** Request/ Response */
        rType,

        /** Variable */
        vType,

        /** Uses */
        uType
    }

    private enum class RDataType(val type: Int) {
        Unknown(0), Request(-1), Response(1)
    }

    private enum class ResultType {
        None, Bool, String
    }

    val scopeVars: MutableMap<String, String> = mutableMapOf()

    init {
        config(this)
    }

    fun setup(config: (P4Action) -> Unit) {
        config(this)
    }

    fun processCommands(steps: List<P4Command>) {
        scopeVars.clear()

        /** Type of data the command is working on */
        var xType = DataType.None

        /** True: Previous command was a successful optional step */
        var pCmdIsOpt = false

        /** Currently processed data
         * <content to process, Flag A, Flag B>
         */
        val processData: MutableList<Triple<String, Int, Int>> = mutableListOf()

        execStep@ for (step in steps) {
            if (!step.isValid) continue@execStep
            processData.clear()

            /* Part 1: Convert source types into more digestible content
            = Input
            - BoundSeqSteps

            = Output
            - xType = (rType), (vType), or (uType)
            - [processData] = <content string, flag A, flag B>
            - r_headers
            */
            /**
             * Flag to display if the source contains a matcher.
             *
             * (r/v/u type){...}
             */
            val incMatch = if (step.source_match != null) 1 else 0
            val hasMatcher = step.source_match != null
            var r_headers: Headers? = null

            when {
                step.isType_R -> {
                    xType = DataType.rType
                    var rType = RDataType.Unknown
                    var r_body: String? = null

                    // retrieve headers or body based on request/ response
                    when {
                        step.isRequest -> {
                            rType = RDataType.Request
                            if (step.isHead)
                                r_headers = in_headers
                            else if (step.isBody)
                                r_body = in_body
                        }

                        step.isResponse -> {
                            rType = RDataType.Response
                            if (step.isHead)
                                r_headers = out_headers
                            else if (step.isBody)
                                r_body = out_body
                        }
                    }

                    // Create `processData`
                    when {
                        step.source_HasSubItem -> {
                            when {
                                r_headers != null -> {
                                    when (step.source_name == null) {
                                        // type 1
                                        true -> r_headers.names(true)
                                            .map { Triple(it, rType.type, 1) }

                                        // type 2 or 3
                                        false -> {
                                            if (!hasMatcher) // type 2
                                                r_headers.names(true)
                                                    .map { Triple(it, rType.type, 2) }
                                            else // type 3
                                                r_headers.values(step.source_name!!)
                                                    .map { Triple(it, rType.type, 3) }
                                        }
                                    }.also { processData.addAll(it) }

                                    if (processData.isEmpty()) {
                                        val hasSrcName = if (step.source_name != null) 1 else 0
                                        processData.add(Triple("", rType.type, 1 + hasSrcName + incMatch))
                                    }
                                }

                                r_body != null -> {
                                    processData.add(
                                        Triple(r_body.orEmpty(), rType.type, incMatch)
                                    )
                                }
                            }
                        }

                        // type 0
                        else -> {
                            when {
                                r_headers != null -> {
                                    r_headers.names(true).forEach {
                                        processData.add(Triple(it, rType.type, 0))
                                    }
                                    if (processData.isEmpty())
                                        processData.add(Triple("", rType.type, 0))
                                }
                                r_body != null -> {
                                    processData.add(
                                        Triple(r_body, rType.type, 0)
                                    )
                                }
                            }
                        }
                    }
                }

                step.isType_V -> {
                    xType = DataType.vType
                    when {
                        step.source_HasSubItem -> {
                            val scopeData = mutableMapOf<String, String>()
                            scopeData.putAll(scopeByLevel(step.varLevel))

                            if (step.varSearchUp) {
                                (step.varLevel..2).forEach { level ->
                                    scopeData.putIfAbsent(scopeByLevel(level))
                                }
                            }

                            when (step.source_name == null) {
                                // type 1
                                true -> {
                                    scopeData.keys.forEach {
                                        processData.add(Triple(it, step.varLevel, 1))
                                    }
                                    if (processData.isEmpty())
                                        processData.add(Triple("", step.varLevel, 1))
                                }

                                // type 2 or 3
                                false -> {
                                    scopeData.asSequence()
                                        .filter { it.key == step.source_name }
                                        .map { data ->
                                            if (step.source_match == null)
                                                (data.key to 2) // type 2
                                            else
                                                (data.value to 3) // type 3
                                        }
                                        .forEach { (str, type) ->
                                            processData.add(Triple(str, step.varLevel, type))
                                        }

                                    if (processData.isEmpty())
                                        processData.add(Triple("", step.varLevel, 2 + incMatch))
                                }
                            }
                        }

                        // type 0
                        else -> {
                            when (step.varLevel) {
                                0 -> scopeVars
                                1 -> chapBounds.scopeVars
                                2 -> testBounds.scopeVars
                                else -> null
                            }.orEmpty().keys.forEach {
                                processData.add(
                                    Triple(it, step.varLevel, 0)
                                )
                            }
                            if (processData.isEmpty())
                                processData.add(Triple("", step.varLevel, 0))
                        }
                    }
                }

                step.isType_U -> {
                    xType = DataType.uType
                    when {
                        step.source_HasSubItem -> {
                            when (step.source_name == null) {
                                // type 1
                                true -> { // local
                                    processData.add(
                                        Triple(chapBounds.stateUse.toString(), 1, 1)
                                    )
                                }

                                // type 2 or 3
                                else -> { // other chapter
                                    val otherBounds = testBounds.boundData[step.source_name!!]?.stateUse.toString()

                                    when (step.source_match == null) {
                                        // type 2
                                        true -> Triple(otherBounds, 2, 2)

                                        // type 3
                                        false -> Triple(otherBounds, 2, 3)
                                    }.also { processData.add(it) }
                                }
                            }
                        }

                        // type 0
                        else -> {
//                            val useState = MockUseStates.isEnabled(chapBounds.stateUse).toString()
                            val useState = chapBounds.stateUse.toString()
                            processData.add(
                                Triple(useState, 1, 0)
                            )
                        }
                    }
                }
            }

            if (xType == DataType.None)
                break // unknown type, don't continue

            /* Part 2: Process match data
            == Assumptions
            1. `processData` will always have at least something
            2. 3rd field (flag B) will be the same in all the items

            == Actions
            = Inputs
            - [processData] = <content string, flag A, flag B>
            - source_match

            = Output
            - result string
            - matched content (0..many)
            - resType (none, bool, string)
            - sourceFlag (int)

            = Output types (by flagB)
            | flag  | result | mResult |
            | ----- | ------ | ------- |
            | 0     | yes    | no      |
            | 1     | yes    | yes     |
            | 2     | yes    | no      |
            | 3     | yes    | yes     |
            */
            /**
             * De-Templated successful source matches
             */
            val mResults: MutableList<MatcherCollection> = mutableListOf()

            val (_, sFlagA, sFlagB) = processData.first()

            var resultExists = false
            var resultCount = 0
            val resultSpread by lazy { mutableListOf<String>() }
            var resultUseVal: Int? = null
            processStep@ for ((content, _, flagB) in processData) {
                when (flagB) {
                    0 -> {
                        when {
                            step.isType_R || step.isType_V -> {
                                resultExists = resultExists || content.isNotEmpty()
                                if (content.isNotEmpty())
                                    resultCount++
                                resultSpread.add(content)
                            }
                            step.isType_U -> {
                                resultUseVal = content.toIntOrNull()
                            }
                        }
                    }

                    1 -> {
                        when {
                            step.isType_R || step.isType_V -> {
                                val mResult = deTemStr(step.source_match, step.varSearchUp)
                                    .matchResults(content)
                                if (mResult.hasMatches)
                                    mResults.add(mResult)
                                resultExists = resultExists || mResult.hasMatches
                                resultCount += mResult.matchCount
                                mResult.matchBundles.mapNotNull { it.last()?.value }
                                    .forEach { resultSpread.add(it) }
//                                    resultSpread.add(mResult.inputStr)
                            }
                            step.isType_U -> {
                                resultUseVal = processUseNumber(content, deTemStr(step.source_match, step.varSearchUp))
                                if (resultUseVal != null)
                                    mResults.add(
                                        MatcherCollection().loadResult(resultUseVal.toString())
                                    )
                            }
                        }
                    }

                    2 -> {
                        when {
                            step.isType_R -> {
                                if (content == step.source_name) {
                                    resultExists = true
                                    if (r_headers != null) {
                                        val values = r_headers.values(content)
                                        resultCount = values.size
                                        resultSpread.addAll(values)
                                        break@processStep
                                    }
                                }
                            }
                            step.isType_V -> {
                                if (content == step.source_name)
                                    resultExists = true
                            }
                            step.isType_U -> {
                                resultUseVal = content.toIntOrNull()
                            }
                        }
                    }

                    3 -> {
                        when {
                            step.isType_R || step.isType_V -> {
                                val mResult = deTemStr(step.source_match, step.varSearchUp)
                                    .matchResults(content)
                                if (mResult.hasMatches)
                                    mResults.add(mResult)
                                resultExists = resultExists || mResult.hasMatches
                                resultCount += mResult.matchCount
                                mResult.matchBundles.forEach {
                                    resultSpread.add(it.last()?.value.orEmpty())
                                }
                            }
                            step.isType_U -> {
                                resultUseVal = processUseNumber(content, deTemStr(step.source_match, step.varSearchUp))
                                if (resultUseVal != null)
                                    mResults.add(
                                        MatcherCollection().loadResult(resultUseVal.toString())
                                    )
                            }
                        }
                    }
                }
            }

            /* Part 3: If this is a conditional, determine if it passed/ failed
            = Input
            - resType = (none, bool, string)
            - result = (string)

            = Output
            - canAction = true/ false
            - break (if condition failed)
            */
            val result by lazy {
                when {
                    step.isType_R || step.isType_V -> resultExists
                    step.isType_U -> MockUseStates.isEnabled(resultUseVal)
                    else -> false
                }
            }

            /** Result of 'was the source successful' */
            var canAction: Boolean

            if (step.isCond) {
                canAction = when (step.cStSrc) {
                    0 -> { // optional only
                        if (!pCmdIsOpt) continue@execStep
                        true
                    }
                    1 -> { // Requires true
                        pCmdIsOpt = result && step.cStOpt
                        result
                    }
                    2 -> { // Requires false
                        pCmdIsOpt = !result && step.cStOpt
                        !result
                    }
                    else -> false
                }

                // is a ("? or !" cond) + (not optional) + (no CanAction)
                // then stop all future steps
                if (step.cStSrc > 0 && !step.cStOpt && !canAction)
                    break@execStep // cond failed, disallow any further steps from running
            } else {
                pCmdIsOpt = false
                canAction = true
            }

            /**
             * De-template the action matcher
             */
            fun stepDeTempMatcher() = deTempMatcher(step, mResults)

            fun matchToList() = listOf(stepDeTempMatcher())

            fun String?.updateRange(rmData: MatcherResult): String {
                return this?.replaceRange(
                    rmData.range.first,
                    rmData.range.last,
                    stepDeTempMatcher()
                ) ?: ""
            }

            /* Part 4: Process the step's action
            = Assumptions
            1. result is not null

            = Input
            - hasAction (bool)
            - canAction (bool)
            - results: exists, count, spread, useVal
            - mresult (resultCollection)
            - xType (rType, vtype, uType)
            - sourceFlags: sFlagA, sFlagB
           */
            if (canAction && step.hasAction) {
                when {
                    // saving data to a variable
                    step.act_name != null -> {
                        val actKey = step.act_name!!.replace(varSuffixes) { it.groupValues[1] }
                        val outputKeys = mutableMapOf<String, String>()

                        // Process data to outputKeys
                        outputKeys[actKey] = resultSpread.firstOrNull() ?: result.toString()
                        if (step.act_nExists)
                            outputKeys["$actKey?"] = resultExists.toString()
                        if (step.act_nCount)
                            outputKeys["$actKey#"] = resultCount.toString()
                        if (step.act_nResult)
                            outputKeys["$actKey@"] = result.toString()
                        if (resultUseVal != null) {
                            outputKeys[actKey] = if (step.isCond)
                                MockUseStates.isEnabled(resultUseVal).toString() else
                                resultUseVal.toString()
                        }
                        if (step.act_nSpread) {
                            when (step.act_nSpreadType) {
                                -1 -> {
                                    resultSpread.forEachIndexed { index, data ->
                                        outputKeys["${actKey}_$index"] = data
                                    }
                                }
                                -2 -> {
                                    outputKeys["${actKey}_?"] = resultSpread.last()
                                }
                                in 0..Int.MAX_VALUE -> {
                                    var usingLast = false
                                    val (data, index) = if (resultSpread.hasIndex(step.act_nSpreadType))
                                        resultSpread[step.act_nSpreadType] to step.act_nSpreadType
                                    else {
                                        usingLast = true
                                        resultSpread.last() to resultSpread.size
                                    }

                                    if (usingLast) {
                                        outputKeys["$actKey#"] = index.toString()
                                        outputKeys["${actKey}_?"] = data
                                    } else
                                        outputKeys["${actKey}_$index"] = data
                                }
                            }
                        }

                        outputKeys.forEach { (key, data) ->
                            updateScopeVar(step.act_scopeLevel, key) { data }
                        }
                    }

                    // we are saving data to the source
                    step.act_match != null -> {
                        if (sFlagA == -1) // -1's are only used on the Request
                            continue@execStep // can't change inputs

                        when (xType) {
                            DataType.rType -> { // response
                                when {
                                    step.isHead -> {
                                        val headList = out_headers.toMultimap()
                                            .toMutableMap()

                                        when (sFlagB) {
                                            -2 -> { // used in the "Not match" cases
                                                if (!step.source_name.isNullOrEmpty())
                                                    headList[step.source_name!!] = matchToList()
                                                else if (!step.source_match.isNullOrBlank())
                                                    headList[step.source_match!!] = matchToList()
                                            }

                                            0 -> Unit // unable to change the entire headers yet

                                            1 -> { // replace all the values for the matching keys
                                                mResults.asSequence()
                                                    .mapNotNull { it[0].first().value }
                                                    .forEach {
                                                        headList[it] = matchToList()
                                                    }
                                            }

                                            2 -> { // writing data to key's value
                                                headList[step.source_name!!] = matchToList()
                                            }

                                            3 -> {
                                                // todo; test which should be used, lastNotNull or last.last
                                                val repRange = mResults
                                                    .lastNotNullResult { it.lastIndexMatch() }?.range
                                                    ?: (0..0)
//                                                val repRange = mResults.last().last().range
                                                headList[step.source_name!!] = headList[step.source_name!!]
                                                    ?.firstOrNull()
                                                    ?.replaceRange(
                                                        repRange.first, repRange.last,
                                                        stepDeTempMatcher()
                                                    ).orEmpty()
                                                    .let { listOf(it) }
                                            }
                                        }

                                        out_headers = headList.toHeaders_dupKeys
                                    }

                                    step.isBody -> {
                                        when (sFlagB) {
                                            // 0 : replace all of the body with out value
                                            0 -> out_body = stepDeTempMatcher()

                                            1 -> { // replace all (of the highest index) matches in the body
                                                mResults
                                                    .flatMap { it[0] }
                                                    .sortedByDescending { it.range.first }
                                                    .forEach {
                                                        out_body = out_body.replaceRange(
                                                            it.range.first,
                                                            it.range.last,
                                                            stepDeTempMatcher()
                                                        )
                                                    }
                                            }
                                        }
                                    }
                                }
                            }

                            DataType.vType -> {
                                when (sFlagB) {
                                    -2 -> { // not matches
                                        step.source_name?.also { keyName ->
                                            updateScopeVar(step.varLevel, keyName) { stepDeTempMatcher() }
                                        }
                                    }

                                    0 -> Unit // no source -> do no actions

                                    1 -> { // create/ set the matched keys to "empty"
                                        mResults.asSequence()
                                            .map { it.inputStr to it.matchBundles.last().last() }
                                            .mapNotNull { (key, rKey) ->
                                                if (rKey == null)
                                                    null else (key to rKey)
                                            }.map { (key, rKey) ->
                                                key to key.updateRange(rKey)
                                            }.forEach { (key, newKey) ->
                                                updateScopeKey(sFlagA, key, newKey)
                                            }
                                    }

                                    2 -> { // save data to the found key
                                        updateScopeVar(sFlagA, step.source_name!!) { stepDeTempMatcher() }
                                    }

                                    3 -> { // save data at key's matched value. Adding a new key if needed
                                        val rData = if (mResults.isEmpty())
                                            null else mResults.last().matchBundles.last().last()
                                        if (rData == null)
                                            updateScopeVar(sFlagA, step.source_name.orEmpty()) { stepDeTempMatcher() }
                                        else
                                            updateScopeVar(sFlagA, step.source_name.orEmpty()) { it.updateRange(rData) }
                                    }
                                }
                            }

                            DataType.uType -> {
                                val asInt = stepDeTempMatcher().toIntOrNull()
                                if (asInt != null)
                                    when (sFlagA) {
                                        1 -> chapBounds.stateUse = asInt
                                        2 -> testBounds.boundData[step.source_name!!]?.stateUse = asInt
                                    }
                            }

                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    fun updateScopeVar(level: Int, key: String, action: (String?) -> String) {
        val currentData = when (level) {
            0 -> scopeVars[key]
            1 -> chapBounds.scopeVars[key]
            2 -> testBounds.scopeVars[key]
            else -> null
        }

        when (level) {
            0 -> scopeVars[key] = action(currentData)
            1 -> chapBounds.scopeVars[key] = action(currentData)
            2 -> testBounds.scopeVars[key] = action(currentData)
        }
    }

    fun updateScopeKey(level: Int, oldKey: String, newKey: String) {
        val currentData = when (level) {
            0 -> scopeVars[oldKey]
            1 -> chapBounds.scopeVars[oldKey]
            2 -> testBounds.scopeVars[oldKey]
            else -> null
        } ?: return

        when (level) {
            0 -> scopeVars.remove(oldKey)
            1 -> chapBounds.scopeVars.remove(oldKey)
            2 -> testBounds.scopeVars.remove(oldKey)
        }

        when (level) {
            0 -> scopeVars[newKey] = currentData
            1 -> chapBounds.scopeVars[newKey] = currentData
            2 -> testBounds.scopeVars[newKey] = currentData
        }
    }

    fun deTempMatcher(
        p4Command: P4Command,
        deTempList: MutableList<MatcherCollection>
    ): String = deTempMatcher(p4Command.act_match, p4Command.varSearchUp, deTempList)

    /**
     * De-template the action matcher
     */
    fun deTempMatcher(
        matcherStr: String?,
        searchUpScope: Boolean,
        deTempList: MutableList<MatcherCollection>
    ): String {
        return deTemStr(matcherStr, searchUpScope) { name ->
            val tryInt = name.toIntOrNull()
            deTempList.firstNotNullResult {
                if (tryInt != null)
                    it[tryInt].lastOrNull()?.value
                else
                    it[name].lastOrNull()?.value
            }
        }
    }

    /**
     * Returns variables, based on the scope [level]
     */
    fun scopeByLevel(
        level: Int,
        filter: (Map.Entry<String, String>) -> Boolean = { true }
    ) = when (level) {
        0 -> scopeVars
        1 -> chapBounds.scopeVars
        2 -> testBounds.scopeVars
        else -> mutableMapOf()
    }.filter { filter(it) }

    /**
     * Returns the requested key or key's value
     * @param keyName Key's name to look by
     * @param scope Where to search for the data<pre>
     *  - 0: Sequence
     *  - 1: Chapter
     *  - 2: Test Bounds
     * </pre>
     * @param searchUpScope If the search should continue up the variable hierarchy
     */
    private fun varKeyVal(keyName: String, scope: Int, searchUpScope: Boolean): String? {
        val scopeData = mutableMapOf<String, String>()
        scopeData.putAll(scopeByLevel(scope) { it.key == keyName })

        if (searchUpScope) {
            (scope..2).forEach { level ->
                scopeData.putIfAbsent(scopeByLevel(level) { it.key == keyName })
            }
        }
        return scopeData.entries.firstOrNull()?.value
    }

    /**
     * Converts "@{..}" content in the string with variables or empty strings.
     *
     * [useSourceItems] is used/searched first, then Sequence/Chapter/Bound variable are searched
     *
     * - If nothing matches, then [stepMatch] is returned unchanged
     */
    private fun deTemStr(
        stepMatch: String?,
        searchUpScope: Boolean,
        useSourceItems: (String) -> String? = { null }
    ): String {
        return Parser_v4.deTemplate(stepMatch.orEmpty()) { name, scope, searchUp ->
            useSourceItems(name) ?: varKeyVal(name, scope, searchUpScope || searchUp)
        }
    }

    fun processUseNumber(input: String, match: String): Int? {
        /** Valid inputs
         * - #
         * - >#
         * - >=#
         * - <#
         * - <=#
         * - #..#
         * - #,#
         * - #,#..#,>#
         */

        val inInt = input.toIntOrNull() ?: return null

        // Step 1, split into easier to process groups
        val itemGroups_a = match.split(",")

        // Step 2, remove invalid options
        val validGroupReg = arrayOf(
            "(\\d+)\\.{2}(\\d+)", // 0
            "(\\d+)", // 1
            ">(\\d+)", // 2
            ">=(\\d+)", // 3
            "<(\\d+)", // 4
            "<=(\\d+)" // 5
        )
            .mapIndexed { index, s -> index to s.toRegex() }

        val isValid = itemGroups_a.asSequence()
            .map { it.replace(" ", "") }
            .mapNotNull { item ->
                // find which reg types the item group is
                // return as (item group, matching reg string)
                (item to validGroupReg.firstOrNull { it.second.matches(item) })
                    .let { if (it.second == null) null else (it.first to it.second!!) }
            }
            .map { (item, reg) ->
                // convert previous step to (reg type, matches)
                val regMatch = reg.second.matchEntire(item)
                val matches = MatcherCollection().loadResults(regMatch)
                reg.first to matches
            }
            .mapNotNull { (type, matches) ->
                // Convert the matches to int values for processing
                fun List<MatcherResult>.tryParseInt() = firstOrNull()?.value?.toIntOrNull()

                val inA = matches[1].tryParseInt()
                val inB = matches[2].tryParseInt()

                val inVals = when (type) {
                    0 -> if (inA == null || inB == null)
                        null else (inA to inB)

                    else -> if (inA == null)
                        null else (inA to 0)
                }

                if (inVals == null)
                    null else (type to inVals)
            }
            .map { (type, vals) ->
                // do the math!
                when (type) {
                    0 -> inInt in (vals.first..vals.second)
                    1 -> inInt == vals.first
                    2 -> inInt > vals.first
                    3 -> inInt >= vals.first
                    4 -> inInt < vals.first
                    5 -> inInt <= vals.first

                    else -> false
                }
            }
            .filter { it } // but only keep those which are true
            .firstOrNull() ?: false // use only the first

        return if (isValid)
            inInt else null
    }
}
