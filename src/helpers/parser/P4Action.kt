package helpers.parser

import helpers.*
import helpers.matchers.MatcherCollection
import helpers.matchers.MatcherResult
import helpers.matchers.matchResults
import mimikMockHelpers.MockUseStates
import networkRouting.testingManager.TestBounds
import networkRouting.testingManager.boundChapterItems
import okhttp3.Headers

class P4Action {
    // todo; below vars are for testing, replace-ish with actual items
    lateinit var bounds: TestBounds
    lateinit var chapItems: boundChapterItems
    lateinit var in_headers: Headers
    lateinit var in_body: String
    lateinit var out_headers: Headers
    lateinit var out_body: String

    val scopeVars: MutableMap<String, String> = mutableMapOf()

    fun setup(config: (P4Action) -> Unit) {
        //    var byChap = bounds.replacerData[chap.name] ?: return this
//    var in_headers = request.headers()
//    var in_body = request.body()?.content().orEmpty()
//    var out_headers = headers()
//    var out_body = body()?.content().orEmpty()
        config.invoke(this)
    }

    fun processCommands(steps: List<P4Command>) {
        scopeVars.clear()

        /**
         * Returns the requested key or key's value
         * @param keyName Key's name to look by
         * @param scopedOnly Range of what vars to check<pre>
         *  - True: check ONLY the Scoped vars
         *  - False: Scoped, then Bound
         * </pre>
         */
        fun varKeyVal(keyName: String, scopedOnly: Boolean): String? {
            val outVal = if (scopedOnly)
                scopeVars[keyName]
            else
                scopeVars[keyName] ?: bounds.boundVars[keyName]
            return if (outVal.isNullOrEmpty())
                null else outVal
        }

        /**
         * Converts "@{..}" content in the string with variables or empty strings.
         *
         * - If nothing matches, then [stepMatch] is returned unchanged
         */
        fun deTemStr(
            stepMatch: String?,
            useSourceItems: (String) -> String? = { null }
        ): String {
            return Parser_v4.deTemplate(stepMatch.orEmpty()) { name, scp ->
                useSourceItems(name) ?: varKeyVal(name, scp)
            }
        }

        fun varKeys_scoped() = scopeVars.keys
        fun varKeys() = bounds.boundVars.keys

        /**
         * 0: none
         * 1: rType
         * 2: vType
         * 3: uType
         */
        var xType = 0
        var pCond = -1

        val processData: MutableList<Triple<String, Int, Int>> = mutableListOf()

        forSteps@ for (step in steps) {
            processData.clear()

            /* Part 1: Convert source types into more digestible content
            = Input
            - BoundSeqSteps

            = Output
            - xType = 1 (rType), 2 (vType), 3 (uType)
            - [processData] = <content string, flag A, flag B>
            - r_headers
            */
            /**
             * Flag to display if the source contains a matcher.
             *
             * (r/v/u type){...}
             */
            val incMatch = if (step.source_match != null) 1 else 0
            var r_headers: Headers? = null

            when {
                step.isType_R -> {
                    xType = 1
                    /**
                     * (-1) = Request
                     * (0) = nothing
                     * (1) = Response
                     */
                    var rType = 0
                    var r_body: String? = null

                    // retrieve headers or body based on request/ response
                    when {
                        step.isRequest -> {
                            rType = -1
                            if (step.isHead)
                                r_headers = in_headers
                            else if (step.isBody)
                                r_body = in_body
                        }

                        step.isResponse -> {
                            rType = 1
                            if (step.isHead)
                                r_headers = out_headers
                            else if (step.isBody)
                                r_body = out_body
                        }
                    }

                    // Create `processData`
                    when {
                        step.source_HasSubItem -> {
                            if (r_headers != null) {
                                val addData = when (step.source_name == null) {
                                    // type 1
                                    true -> r_headers.names()
                                        .map { Triple(it, rType, 1) }

                                    // type 2 or 3
                                    false -> {
                                        if (incMatch == 0)
                                            r_headers.values(step.source_name!!)
                                                .map { Triple(it, rType, 2) }
                                        else
                                            r_headers.names()
                                                .map { Triple(it, rType, 3) }
                                    }
                                }

                                processData.addAll(addData)
                            } else if (r_body != null) {
                                processData.add(
                                    Triple(r_body.orEmpty(), rType, incMatch)
                                )
                            }
                        }

                        else -> {
                            if (r_headers != null) {
                                processData.add(
                                    Triple(r_headers.size().toString(), rType, 0)
                                )
                            } else if (r_body != null) {
                                processData.add(
                                    Triple(r_body, rType, 0)
                                )
                            }
                        }
                    }
                }

                step.isType_V -> {
                    xType = 2
                    when {
                        step.source_HasSubItem -> {
                            when (step.source_name == null) {
                                true -> {
                                    if (step.varType in (0..1)) // 0 or 1
                                        varKeys_scoped().forEach {
                                            processData.add(Triple(it, 1, 1))
                                        }

                                    varKeys().forEach {
                                        processData.add(Triple(it, 2, 1))
                                    }
                                }

                                false -> {
                                    var procSeq: Sequence<Pair<Map.Entry<String, String>, Int>> = emptySequence()

                                    if (step.varType in (0..1)) // 0 or 1
                                        procSeq += scopeVars.asSequence()
                                            .map { it to 1 }

                                    procSeq += bounds.boundVars.asSequence()
                                        .map { it to 2 }

                                    procSeq
                                        .filter { it.first.key == step.source_name }
                                        .map { (data, flag) ->
                                            Pair(
                                                if (step.source_match == null)
                                                    data.value else data.key,
                                                flag
                                            )
                                        }
                                        .forEach { (str, flagA) ->
                                            processData.add(Triple(str, flagA, 2 + incMatch))
                                        }
                                }
                            }
                        }

                        else -> {
                            when (step.varType) {
                                0 -> {
                                    val vItems = kotlin.math.max(varKeys_scoped().size, varKeys().size).toString()
                                    processData.add(
                                        Triple(vItems, 0, 0)
                                    )
                                }

                                1 -> processData.add(
                                    Triple(varKeys_scoped().size.toString(), 1, 0)
                                )

                                2 -> processData.add(
                                    Triple(varKeys().size.toString(), 2, 0)
                                )
                            }
                        }
                    }
                }

                step.isType_U -> {
                    xType = 3
                    when {
                        step.source_HasSubItem -> {
                            when (step.source_name == null) {
                                true -> { // local
                                    processData.add(
                                        Triple(chapItems.stateUse.toString(), 1, 1)
                                    )
                                }

                                else -> { // other chapter
                                    val otherBounds = bounds.boundData[step.source_name!!]

                                    when (step.source_match == null) {
                                        true -> processData.add(
                                            Triple((otherBounds != null).toString(), 2, 2)
                                        )

                                        false -> processData.add(
                                            Triple(
                                                otherBounds?.stateUse?.toString().orEmpty(),
                                                2, 3
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        else -> {
                            val useState = MockUseStates.isEnabled(chapItems.stateUse).toString()
                            processData.add(
                                Triple(useState, 1, 0)
                            )
                        }
                    }
                }
            }

            if (xType == 0)
                break // unknown type, don't continue

            /* Part 2: Process match data
            == Assumptions
            1. `processData` will always have at least something
            2. 3rd field will be the same in all the items

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
            var result: String? = null
            /**
             * DeTemplated successful source matches
             */
            val mResults: MutableList<MatcherCollection> = mutableListOf()
            /** Type of data which `result` holds
             * - 0: none
             * - 1: bool
             * - 2: string
             */
            var resType = 0
            var step2_done = false
            var sourceFlags = (-2 to -2)

            for ((content, flagA, flagB) in processData) {
                when (flagB) {
                    0 -> { // contains data?
                        result = if (step.isType_U)
                            content else
                            content.isNotEmpty().toString()
                        resType = 1
                        step2_done = true
                    }

                    1 -> { // has key (headers/ vars) or has match (body)
                        if (step.isType_U) { // special processing is needed
                            val mResult = processUseNumber(content, deTemStr(step.source_match))

                            if (step.source_match == null) {
                                resType = 1
                                result = (mResult != null).toString()
                            } else {
                                if (mResult == null) {
                                    resType = 1
                                    result = "false"
                                } else {
                                    resType = 2
                                    result = mResult.toString()
                                    mResults.add(
                                        MatcherCollection().loadResult(result)
                                    )
                                }
                            }
                        } else {
                            val mResult = deTemStr(step.source_match).matchResults(content)
                            if (mResult.hasMatches) {
                                mResults.add(mResult)
                                result = if (step.isHead or step.isType_V) {
                                    resType = 1
                                    mResult.hasMatches.toString()
                                } else {
                                    resType = 2
                                    mResult.last().value.orEmpty()
                                }
                            }
                        }
                    }

                    2 -> { // if we are here, then `content` is all we need for now
                        result = content
                        resType = if (step.isType_U && step.source_match == null)
                            1 else 2
                        step2_done = true // we only want the first item
                    }

                    3 -> { // matched content
                        when (xType) {
                            1 -> {
                                if (step.isHead) {
                                    r_headers?.values(content)?.forEach {
                                        val mResult = deTemStr(step.source_match).matchResults(it)
                                        if (mResult.hasMatches) {
                                            mResults.add(mResult)
                                            resType = 2
                                            result = mResult.last().value
                                        }
                                    }
                                }
                            }

                            2 -> {
                                val kValue = varKeyVal(content, flagA == 1)
                                val mResult = deTemStr(step.source_match).matchResults(kValue)
                                if (mResult.hasMatches) {
                                    mResults.add(mResult)
                                    resType = 2
                                    result = mResult.last().value
                                }
                            }

                            3 -> {
                                // todo; testing
                                val mResult = processUseNumber(content, deTemStr(step.source_match))

                                if (step.source_match == null) {
                                    resType = 1
                                    result = (mResult != null).toString()
                                } else {
                                    if (mResult == null) {
                                        resType = 1
                                        result = "false"
                                    } else {
                                        resType = 2
                                        result = mResult.toString()
                                        mResults.add(
                                            MatcherCollection().loadResult(result!!)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (result != null)
                    sourceFlags = (flagA to flagB)
                if (step2_done) break
            }

            /* Part 3: If this is a conditional, determine if it passed/ failed
            = Input
            - resType = (none, bool, string)
            - result = (string)

            = Output
            - canAction = true/ false
            - break (if condition failed)
            */
            /**
             * True: the previous step state set the 'successful condition' flag
             */
            fun hasStPass() = pCond != -1

            var canAction = false
            if (step.isCond) {
                when (resType) {
                    0 -> { // 'Not' cases
                        when (step.cStSrc) {
                            0 -> {
                                if (!hasStPass()) continue@forSteps
                                result = "true"
                                canAction = true
                            }
                            2 -> {
                                result = "true"
                                canAction = true
                                pCond = step.cStSrc
                            }
                        }
                    }

                    1 -> {
                        canAction = when (step.cStSrc) {
                            0 -> {
                                if (hasStPass()) {
                                    result == "true"
                                } else false
                            }
                            1 -> result == "true"
                            2 -> result == "false"
                            else -> false
                        }
                    }

                    2 -> {
                        when (step.cStSrc) {
                            0 -> {
                                if (hasStPass()) {
                                    if (result.isNullOrEmpty()) {
                                        result = "true"
                                        canAction = true
                                    }
                                }
                            }
                            1 -> {
                                if (!result.isNullOrEmpty()) {
                                    pCond = step.cStSrc
                                    canAction = true
                                }
                            }
                            2 -> {
                                if (result.isNullOrEmpty()) {
                                    pCond = step.cStSrc
                                    result = "true"
                                    canAction = true
                                }
                            }
                        }
                    }
                }

                // is a ("? or !" cond) + (not optional) + (no CanAction)
                // then stop all future steps
                if (step.cStSrc > 0 && !step.cStOpt && !canAction)
                    break@forSteps // cond failed, disallow any further steps from running
            } else {
                pCond = -1 //  reset the state
                canAction = true // result != null
            }

            /* Part 4: Process the step's action
            = Assumptions
            1. result is not null

            = Input
            - hasAction (bool)
            - canAction (bool)
            - result (string)
            - mresult (resultCollection)
            - xType (1: rType, 2: vtype, 3: uType)
            - sourceFlags

             */
            if (step.hasAction && canAction) {
                when {
                    // saving data to a variable
                    step.act_name != null -> {
                        if (step.act_nameScoped)
                            scopeVars[step.act_name!!] = result!!
                        else
                            bounds.boundVars[step.act_name!!] = result!!
                    }

                    // we are saving data to the source
                    step.act_match != null -> {
                        if (sourceFlags.first == -1) // -1's are only used on the Request
                            continue@forSteps // can't change inputs

                        /**
                         * De-template the action matcher
                         */
                        fun matchDeTemp(): String {
                            return deTemStr(step.act_match) { name ->
                                val tryInt = name.toIntOrNull()
                                mResults.firstNotNullResult {
                                    if (tryInt != null)
                                        it[tryInt].lastOrNull()?.value
                                    else
                                        it[name].lastOrNull()?.value
                                }
                            }
                        }

                        fun matchToList() = listOf(matchDeTemp())

                        when (xType) {
                            1 -> { // response
                                when {
                                    step.isHead -> {
                                        val headList = out_headers.toMultimap()
                                            .toMutableMap()

                                        when (sourceFlags.second) {
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
                                                val uu = mResults.lastNotNullResult {
                                                    it.lastIndexMatch()
                                                }
                                                val repRange = mResults.last().last().range
                                                headList[step.source_name!!] = headList[step.source_name!!]
                                                    ?.firstOrNull()
                                                    ?.replaceRange(
                                                        repRange.first, repRange.last,
                                                        matchDeTemp()
                                                    ).orEmpty()
                                                    .let { listOf(it) }
                                            }
                                        }

                                        out_headers = headList.toHeaders_dupKeys
                                    }

                                    step.isBody -> {
                                        when (sourceFlags.second) {
                                            // -2 : not matched, use all of the out value
                                            // 0 : replace all of the body with out value
                                            -2, 0 -> out_body = matchDeTemp()

                                            1 -> { // replace all (of the highest index) matches in the body
                                                mResults
                                                    .flatMap { it[it.lastIndex] }
                                                    .sortedByDescending { it.range.first }
                                                    .forEach {
                                                        out_body = out_body.replaceRange(
                                                            it.range.first,
                                                            it.range.last,
                                                            matchDeTemp()
                                                        )
                                                    }
                                            }
                                        }
                                    }
                                }
                            }

                            2 -> { // var
                                when (sourceFlags.second) {
                                    -2 -> { // not matches
                                        step.source_name?.also { keyName ->
                                            when (step.varType) {
                                                1 -> scopeVars[keyName] = matchDeTemp()
                                                0, 2 -> bounds.boundVars[keyName] = matchDeTemp()
                                            }
                                        }
                                    }

                                    0 -> Unit // no source -> do no actions

                                    1 -> { // create/ set the matched keys to "empty"
                                        mResults.asSequence()
                                            .map { it[0] }
                                            .filter { it.isNotEmpty() }
                                            .mapNotNull { it.first().value }
                                            .forEach {
                                                when (sourceFlags.first) {
                                                    1 -> scopeVars[it] = matchDeTemp()
                                                    2 -> bounds.boundVars[it] = matchDeTemp()
                                                }
                                            }
                                    }

                                    2 -> { // save data to the found key
                                        if (result != null)
                                            when (sourceFlags.first) {
                                                1 -> scopeVars[step.source_name!!] = matchDeTemp()
                                                2 -> bounds.boundVars[step.source_name!!] = matchDeTemp()
                                            }
                                    }

                                    3 -> { // save data at key's matched value. Adding a new key if needed
                                        val varKey = processData.first().first
                                        mResults
                                            .flatMap { it[0] }
                                            .forEach { rmData ->
                                                fun String?.updateRange(): String {
                                                    return this?.replaceRange(
                                                        rmData.range.first,
                                                        rmData.range.last,
                                                        matchDeTemp()
                                                    ) ?: ""
                                                }

                                                when (sourceFlags.first) {
                                                    1 -> scopeVars[varKey] =
                                                        scopeVars[varKey].updateRange()
                                                    2 -> bounds.boundVars[varKey] =
                                                        bounds.boundVars[varKey].updateRange()
                                                }
                                            }
                                    }
                                }
                            }

                            3 -> { // use
                                when (sourceFlags.first) {
                                    1 -> {
                                        // second; 0 or 1, only valid set-to option is a number
                                        val asInt = matchDeTemp().toIntOrNull()
                                        if (asInt != null)
                                            chapItems.stateUse = asInt
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
