package unitTests.testManagerTests

import helpers.*
import helpers.matchers.MatcherResult
import helpers.parser.P4Action
import helpers.parser.P4Command
import helpers.parser.Parser_v4
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kolor.*
import networkRouting.testingManager.*
import okhttp3.Headers
import org.junit.Assert
import org.junit.Test

class ParserTests {
    val testObject by lazy { TestManager() }
    val parse4Class by lazy { Parser_v4() }

    //    @Test
    fun parse_v3_ManyChaps() {
//        val body = """
//            {
//               "chap_1":{},
//               "chap_2":{}
//            }
//        """.trimIndent()
//
//        val result = testObject.parse_v3(body)
//        Assert.assertEquals(2, result.size)
//        Assert.assertTrue(result.containsKey("chap_1"))
//        Assert.assertTrue(result.containsKey("chap_2"))
    }

    //    @Test
    fun parse_v3_ModifyAndSeq() {
//        val body = """
//            {
//               "chap_name":{
//                  "modify":[
//                     "body{from->to}",
//                     "body{(from)->@{1}also}",
//                     "body{(?<gp>from)->@{gp}also}",
//                     "var{[find]->SaveVar}"
//                  ],
//                  "seqAct":[
//                     {
//                        "when":[
//                           "when:AAA:body:w:w"
//                        ],
//                        "do":[
//                           "do:AAA:use:-:4"
//                        ]
//                     }
//                  ]
//               }
//            }
//        """.trimIndent()
//
//        val result = testObject.parse_v3(body)
//        Assert.assertEquals(1, result.size)
//        Assert.assertTrue(result.containsKey("chap_name"))
//
//        val chapResult = result["chap_name"]!!
//        Assert.assertEquals(3, chapResult.replacers_body.size)
//        Assert.assertEquals(1, chapResult.findVars.size)
//        Assert.assertEquals(1, chapResult.seqSteps_old.size)
    }

    //    @Test
    fun parse_v3_SeqAct_forMats() {
        /*
            == testing items
            - chap name can be empty
            - "from" can contain any alpha-num char without needing field bounds
            - "from" with field bounds can have more special chars
            - "to" field has the same properties as "from"
         */
//        val body = """
//            {
//              "chap_name": {
//                "seqAct": [{
//                    "when": [
//                      "when::body:():()",
//                      "when::any:-:-"
//                    ],
//                    "do": [
//                      "do:thing:use:-:4",
//                      "do:thing:body::(testing)",
//                      "do:thing:body:(test):(end)",
//                      "do:thing:body:(code :):none",
//                      "do:thing:body:code any:none",
//                      "do:thing:body:test:  ",
//                      "do:thing:body:test:",
//                      "do:thing:body:($):end"
//                    ]
//                  }
//                ]
//              }
//            }
//        """.trimIndent()
//
//        val result = testObject.parse_v3(body)
//        Assert.assertEquals(1, result.size)
//        Assert.assertTrue(result.containsKey("chap_name"))
//
//        val chapResult = result["chap_name"]!!
//        val seqStepItems = chapResult.seqSteps_old.first()
//        Assert.assertEquals(2, seqStepItems.step_When.size)
//        Assert.assertEquals(8, seqStepItems.step_Do.size)
    }

    //    @Test
    fun parse_v3_SeqAct_manyActs() {
        /*
            == testing items
            - chap name can be empty
            - "from" can contain any alpha-num char without needing field bounds
            - "from" with field bounds can have more special chars
            - "to" field has the same properties as "from"
         */
//        val body = """
//            {
//               "chap_name":{
//                  "seqAct":[
//                     {
//                        "when":[
//                           "when:AAA:body:w:w"
//                        ],
//                        "do":[
//                           "do:AAA:use:w:w"
//                        ]
//                     },
//                     {
//                        "when":[
//                           "when:BBB:body:w:w"
//                        ],
//                        "do":[
//                           "do:BBB:use:w:w"
//                        ]
//                     }
//                  ]
//               }
//            }
//        """.trimIndent()
//
//        val result = testObject.parse_v3(body)
//        Assert.assertEquals(1, result.size)
//        Assert.assertTrue(result.containsKey("chap_name"))
//
//        val chapResult = result["chap_name"]!!
//        Assert.assertEquals(2, chapResult.seqSteps_old.size)
//
//        val seqStepItems_0 = chapResult.seqSteps_old[0]
//        Assert.assertEquals(1, seqStepItems_0.step_When.size)
//        Assert.assertEquals(1, seqStepItems_0.step_Do.size)
//        Assert.assertTrue(
//            seqStepItems_0.step_When.all { it.name == "AAA" }
//        )
//
//        val seqStepItems_1 = chapResult.seqSteps_old[1]
//        Assert.assertEquals(1, seqStepItems_1.step_When.size)
//        Assert.assertEquals(1, seqStepItems_1.step_Do.size)
//        Assert.assertTrue(
//            seqStepItems_1.step_When.all { it.name == "BBB" }
//        )
    }

    val regexPattern: String
        get() = """
(?<cond>
    (?=[~?!])
    (?<cP>~)?
    (?:(?<cT>\?)|(?<cF>!))?
)?
(?<source>
    (?<rType>(?:
        (?<rIn>request:(?:
            (?<rInH>head(?:\[(?<rInHN>\w+)\])?)|
            (?<rInB>body)
        ))|
        (?<rOut>response:(?:
            (?<rOutH>head(?:\[(?<rOutHN>\w+)\])?)|
            (?<rOutB>body)
        )))
        (?::\{
            (?<rM>.*?)
            \}(?=->|\s|$)
        )?
    )|
    (?<vType>
        (?:(?<vS>&)|(?<vb>b)|)?
        var
        (?:\[(?<vN>[a-zA-Z]\w*)\])?
        (?::\{(?<vM>.+?)\}(?=->|\s|$))?
    )|
    (?<uType>use
        (?:\[(?<uN>[a-zA-Z][!-+\--~]*)\])?
        (?::\{(?<uM>[\d=,.<>]+)\})?
    )
)
(?<act>->(?:
        (?:\{(?<aM>.*?)\}(?=\s|$))|
        (?<aV>
            (?<aVS>&)?
            (?<aVN>[a-zA-Z]\w*)
        )
    )
)?
        """.replace("""( {2}|\n|\r)""".toRegex(), "")

    val validInputCombos = mapOf(
        // == Requests
        // === Head
        "?request:head" to listOf(
            "cond", "cT", "source", "rType", "rIn", "rInH"
        ),
        "?request:head[aa]" to listOf(
            "cond", "cT", "source", "rType", "rIn", "rInH", "rInHN"
        ),
        "?request:head[aa]:{bb}" to listOf(
            "cond", "cT", "source", "rType", "rIn", "rInH", "rInHN", "rM"
        ),
        "?request:head:{bb}" to listOf(
            "cond", "cT", "source", "rType", "rIn", "rInH", "rM"
        ),
        // + actions, to var
        "request:head->cc" to listOf(
            "source", "rType", "rIn", "rInH", "act", "aV", "aVN"
        ),
        "request:head[aa]->cc" to listOf(
            "source", "rType", "rIn", "rInH", "rInHN", "act", "aV", "aVN"
        ),
        "request:head[aa]:{bb}->cc" to listOf(
            "source", "rType", "rIn", "rInH", "rInHN", "rM", "act", "aV", "aVN"
        ),
        "request:head:{bb}->cc" to listOf(
            "source", "rType", "rIn", "rInH", "rM", "act", "aV", "aVN"
        ),

        // === Body
        "?request:body" to listOf(
            "cond", "cT", "source", "rType", "rIn", "rInB"
        ),
        "?request:body:{bb}" to listOf(
            "cond", "cT", "source", "rType", "rIn", "rInB", "rM"
        ),
        // + actions, to var
        "request:body->cc" to listOf(
            "source", "rType", "rIn", "rInB", "act", "aV", "aVN"
        ),
        "request:body:{bb}->cc" to listOf(
            "source", "rType", "rIn", "rInB", "rM", "act", "aV", "aVN"
        ),

        // == Response
        // === Heads
        "?response:head" to listOf(
            "cond", "cT", "source", "rType", "rOut", "rOutH"
        ),
        "?response:head[aa]" to listOf(
            "cond", "cT", "source", "rType", "rOut", "rOutH", "rOutHN"
        ),
        "?response:head[aa]:{bb}" to listOf(
            "cond", "cT", "source", "rType", "rOut", "rOutH", "rOutHN", "rM"
        ),
        "?response:head:{bb}" to listOf(
            "cond", "cT", "source", "rType", "rOut", "rOutH", "rM"
        ),
        // + actions, to var
        "response:head->cc" to listOf(
            "source", "rType", "rOut", "rOutH", "act", "aV", "aVN"
        ),
        "response:head[aa]->cc" to listOf(
            "source", "rType", "rOut", "rOutH", "rOutHN", "act", "aV", "aVN"
        ),
        "response:head[aa]:{bb}->cc" to listOf(
            "source", "rType", "rOut", "rOutH", "rOutHN", "rM", "act", "aV", "aVN"
        ),
        "response:head:{bb}->cc" to listOf(
            "source", "rType", "rOut", "rOutH", "rM", "act", "aV", "aVN"
        ),
        // +actions, to self
        "response:head[aa]->{dd}" to listOf(
            "source", "rType", "rOut", "rOutH", "rOutHN", "act", "aM"
        ),
        "response:head[aa]:{bb}->{dd}" to listOf(
            "source", "rType", "rOut", "rOutH", "rOutHN", "rM", "act", "aM"
        ),
        "response:head:{bb}->{dd}" to listOf(
            "source", "rType", "rOut", "rOutH", "rM", "act", "aM"
        ),

        // === Body
        "?response:body" to listOf(
            "cond", "cT", "source", "rType", "rOut", "rOutB"
        ),
        "?response:body:{bb}" to listOf(
            "cond", "cT", "source", "rType", "rOut", "rOutB", "rM"
        ),
        // + actions, to var
        "response:body->cc" to listOf(
            "source", "rType", "rOut", "rOutB", "act", "aV", "aVN"
        ),
        "response:body:{}->cc" to listOf(
            "source", "rType", "rOut", "rOutB", "rM", "act", "aV", "aVN"
        ),
        "response:body:{bb}->cc" to listOf(
            "source", "rType", "rOut", "rOutB", "rM", "act", "aV", "aVN"
        ),
        // + actions, to self
        "response:body->{dd}" to listOf(
            "source", "rType", "rOut", "rOutB", "act", "aM"
        ),
        "response:body:{bb}->{dd}" to listOf(
            "source", "rType", "rOut", "rOutB", "rM", "act", "aM"
        ),

        // == Var
        "?var" to listOf(
            "cond", "cT", "source", "vType"
        ),
        "?var[aa]" to listOf(
            "cond", "cT", "source", "vType", "vN"
        ),
        "?var:{mm}" to listOf(
            "cond", "cT", "source", "vType", "vM"
        ),
        "?var[aa]:{mm}" to listOf(
            "cond", "cT", "source", "vType", "vM", "vN"
        ),
        // + actions, to var
        "var->cc" to listOf(
            "source", "vType", "act", "aV", "aVN"
        ),
        "var[aa]->cc" to listOf(
            "source", "vType", "vN", "act", "aV", "aVN"
        ),
        "var:{mm}->cc" to listOf(
            "source", "vType", "vM", "act", "aV", "aVN"
        ),
        "var[aa]:{mm}->cc" to listOf(
            "source", "vType", "vM", "vN", "act", "aV", "aVN"
        ),
        // + actions, to self
        "var[aa]->{dd}" to listOf(
            "source", "vType", "vN", "act", "aM"
        ),
        "var[aa]:{mm}->{dd}" to listOf(
            "source", "vType", "vM", "vN", "act", "aM"
        ),

        // use
        "?use:{3}" to listOf(
            "cond", "cT", "source", "uType", "uM"
        ),
        "?use:{>3}" to listOf(
            "cond", "cT", "source", "uType", "uM"
        ),
        "?use:{>=3}" to listOf(
            "cond", "cT", "source", "uType", "uM"
        ),
        "?use:{<3}" to listOf(
            "cond", "cT", "source", "uType", "uM"
        ),
        "?use:{3..4}" to listOf(
            "cond", "cT", "source", "uType", "uM"
        ),
        "?use:{3,5..6,9}" to listOf(
            "cond", "cT", "source", "uType", "uM"
        ),

        // + actions
        "use:{3,5..6,9}->cc" to listOf(
            "source", "uType", "uM", "act", "aV", "aVN"
        ),
        "use->{dd}" to listOf(
            "source", "uType", "act", "aM"
        ),
        "use:{3,5..6,9}->{dd}" to listOf(
            "source", "uType", "uM", "act", "aM"
        ),

        // conditional options
        "?response:body:{bb}->{dd}" to listOf(
            "cond", "cT", "source", "rType", "rOut", "rOutB", "rM", "act", "aM"
        ),
        "!response:body:{bb}->{dd}" to listOf(
            "cond", "cF", "source", "rType", "rOut", "rOutB", "rM", "act", "aM"
        ),
        "~?response:body:{bb}->{dd}" to listOf(
            "cond", "cP", "cT", "source", "rType", "rOut", "rOutB", "rM", "act", "aM"
        ),
        "~!response:body:{bb}->{dd}" to listOf(
            "cond", "cP", "cF", "source", "rType", "rOut", "rOutB", "rM", "act", "aM"
        ),
        "~response:body:{bb}->{dd}" to listOf(
            "cond", "cP", "source", "rType", "rOut", "rOutB", "rM", "act", "aM"
        ),

        // == Misc items
        // body matcher contains '}->', which is also a border between items
        "response:body:{a{b}\\->c}->{w}" to listOf(
            "source", "rType", "rOut", "rOutB", "rM", "act", "aM"
        ),
        // to a scoped variable, instead of test bound variable
        "response:body:{bb}->&cc" to listOf(
            "source", "rType", "rOut", "rOutB", "rM", "act", "aV", "aVS", "aVN"
        )
    )

    // Combos which match the syntax, but aren't computable
    val invalidInputCombos = mapOf(
        // not enough source info
        "request:head->{dd}" to listOf(
            "source", "rType", "rIn", "rInH", "act", "aM"
        ),
        // can't change request items
        "request:head[aa]:{bb}->{dd}" to listOf(
            "source", "rType", "rIn", "rInH", "rInHN", "rM", "act", "aM"
        ),
        "request:head:{bb}->{dd}" to listOf(
            "source", "rType", "rIn", "rInH", "rM", "act", "aM"
        ),
        "request:body:{bb}->{dd}" to listOf(
            "source", "rType", "rIn", "rInB", "rM", "act", "aM"
        ),

        // does nothing, not enough source data
        "response:head->{dd}" to listOf(
            "source", "rType", "rOut", "rOutH", "act", "aM"
        ),

        // not enough source info
        "var->{dd}" to listOf(
            "source", "vType", "act", "aM"
        ),
        // no 'self' var to write to
        "var:{bb}->{dd}" to listOf(
            "source", "vType", "vM", "act", "aM"
        ),

        // names should start with an [a-zA-Z] char
        "var[321test]->{dd}" to listOf(
            "source", "vType", "act", "aM"
        )
    )

    val inputRequireCond = mapOf(
        "request:head" to listOf(
            "source", "rType", "rIn", "rInH"
        ),
        "request:head[aa]" to listOf(
            "source", "rType", "rIn", "rInH", "rInHN"
        ),
        "request:head[aa]:{bb}" to listOf(
            "source", "rType", "rIn", "rInH", "rInHN", "rM"
        ),
        "request:head:{bb}" to listOf(
            "source", "rType", "rIn", "rInH", "rM"
        ),

        "request:body" to listOf(
            "source", "rType", "rIn", "rInB"
        ),
        "request:body:{bb}" to listOf(
            "source", "rType", "rIn", "rInB", "rM"
        ),

        "response:head" to listOf(
            "source", "rType", "rOut", "rOutH"
        ),
        "response:head[aa]" to listOf(
            "source", "rType", "rOut", "rOutH", "rOutHN"
        ),
        "response:head[aa]:{bb}" to listOf(
            "source", "rType", "rOut", "rOutH", "rOutHN", "rM"
        ),
        "response:head:{bb}" to listOf(
            "source", "rType", "rOut", "rOutH", "rM"
        ),

        "response:body" to listOf(
            "source", "rType", "rOut", "rOutB"
        ),
        "response:body:{bb}" to listOf(
            "source", "rType", "rOut", "rOutB", "rM"
        ),

        "var" to listOf(
            "source", "vType"
        ),
        "var[aa]" to listOf(
            "source", "vType", "vN"
        ),
        "var:{mm}" to listOf(
            "source", "vType", "vM"
        ),
        "var[aa]:{mm}" to listOf(
            "source", "vType", "vM", "vN"
        ),

        "use:{3}" to listOf(
            "source", "uType", "uM"
        ),
        "use:{>3}" to listOf(
            "source", "uType", "uM"
        ),
        "use:{>=3}" to listOf(
            "source", "uType", "uM"
        ),
        "use:{<3}" to listOf(
            "source", "uType", "uM"
        ),
        "use:{3..4}" to listOf(
            "source", "uType", "uM"
        ),
        "use:{3,5..6,9}" to listOf(
            "source", "uType", "uM"
        )
    )

    private fun Map<String, List<String>>.toResultMap(): List<Triple<String, List<MatcherResult>, List<String>>> {
        return map { (value, items) ->
            Triple(
                value,
                parse4Class.parseToContents(value),
                items
            )
        }
    }

    private fun Parser_v4.compareParsedToExpected(
        input: String, parsed: List<MatcherResult>, expected: List<String>,
        expectAsValid: Boolean = true
    ) {
        println("Comparing matches for: $input")
        fun parsedAsStr(): String =
            parsed.joinToString { it.groupName }

        val isValidSyx = isValid_Syntax(parsed)
        if (isValidSyx != expectAsValid) {
            val failStr = "Failed the syntax validity test.\n" +
                    "Parsed items: ${parsedAsStr()}"
            Assert.fail(failStr)
        }

        val seqSteps = P4Command(parsed)
        Assert.assertEquals(
            "`input` should be fully reproducible from a BoundSeqSteps flags/ values",
            input, seqSteps.toString()
        )

        if (!isValid_Request(parsed)) {
            val condErrorStr = "Input is missing an action or conditional\n" +
                    "Parsed: ${parsed.joinToString { it.groupName }}\n" +
                    "Actions:\n" +
                    "  1. Prepend '?'. '?$input'\n" +
                    "  2. Add an action. '$input->...' or '$input->{...}'"
            Assert.fail(condErrorStr)
        }

        parsed.forEach {
            if (!expected.contains(it.groupName)) {
                val expStr = "Parsed contains: ${it.groupName}\n" +
                        "Expected only: ${expected.joinToString()}\n" +
                        "Full parsed: ${parsed.joinToString { it.groupName }}\n" +
                        "Did you forget to add '${it.groupName}'?"

                Assert.fail(expStr)
            }
        }

        expected.forEach { eName ->
            val expStr = "Expecting to contain: $eName\n" +
                    "Parsed contains: ${parsedAsStr()}\n" +
                    "Did you forget to remove '$eName'?"
            Assert.assertTrue(expStr,
                parsed.any { it.groupName == eName }
            )
        }
    }

    @Test
    fun parse_v4_expectPattern() {
        Assert.assertEquals(regexPattern, parse4Class.toString())
    }

    @Test
    fun parse_v4_ValidCombos() {
        validInputCombos.toResultMap()
            .forEach { (input, parsed, expected) ->
                parse4Class.compareParsedToExpected(input, parsed, expected)
            }
    }

    //    @Test
    fun parse_v4_InvalidCombos() {
        invalidInputCombos.toResultMap()
            .forEach { (input, parsed, expected) ->
                parse4Class.compareParsedToExpected(input, parsed, expected, false)
            }
    }

    @Test // If the input has no action, the it MUST be a conditional
    fun parse_v4_expectAsCondOrAct() {
        inputRequireCond.toResultMap()
            .forEach { (_, parsed, _) ->
                parse4Class.also { p4 ->
                    Assert.assertFalse(
                        p4.isValid_Request(parsed)
                    )
                }
            }
    }

    @Test
    fun deTemplate_passthroughTest() {
        val cleanInput = "any(group)Text"

        val result = Parser_v4.deTemplate(cleanInput) { _, _ -> "fail" }

        Assert.assertEquals(
            "Input is expected to pass straight through",
            cleanInput, result
        )
    }

    @Test
    fun deTemplate_FinalOpt() {
        val input = "input_@{'item'}_test"
        val expectOut = "input_item_test"

        val result = Parser_v4.deTemplate(input) { _, _ -> "fail" }

        Assert.assertEquals(
            "Single quotes values are final values",
            expectOut, result
        )
    }

    @Test
    fun deTemplate_VarOpt() {
        val input = "input_@{user}_test"
        val expectOut = "input_pass_test"

        val result = Parser_v4.deTemplate(input) { v, _ ->
            when (v) {
                "user" -> "pass"
                else -> "fail"
            }
        }

        Assert.assertEquals(
            "Variable is translated to text",
            expectOut, result
        )
    }

    @Test
    fun deTemplate_IndexOpt() {
        val input = "input_@{2}_test"
        val expectOut = "input_pass_test"

        val result = Parser_v4.deTemplate(input) { v, _ ->
            when (v) {
                "2" -> "pass"
                else -> "fail"
            }
        }

        Assert.assertEquals(
            "Index is translated to text",
            expectOut, result
        )
    }

    @Test
    fun deTemplate_DefaultFinal() {
        val input = "input_@{none|'other'}_test"
        val expectOut = "input_other_test"

        val result = Parser_v4.deTemplate(input) { v, _ ->
            when (v) {
                "item" -> "pass"
                else -> null
            }
        }

        Assert.assertEquals(
            "Given final option was expected",
            expectOut, result
        )
    }

    @Test
    fun deTemplate_Scoped() {
        val input = "input_@{aaa}-@{&aaa}_test"
        val expectOut = "input_bb-cc_test"

        val result = Parser_v4.deTemplate(input) { v, s ->
            when (v) {
                "aaa" -> {
                    if (s) "cc"
                    else "bb"
                }
                else -> null
            }
        }

        Assert.assertEquals(
            "Explicit scoped requests should only search Scoped vars",
            expectOut, result
        )
    }

    @Test
    fun parse_v4_bodyIgnoreInvalid() {
        // Only the first action array contains all valid items
        // Second array has an invalid action, so the whole array is bad (for safety)
        val body = """
            {
               "aa":[
                  [
                     "request:body:{code: .+}->codeA",
                     "request:body:{other: .+}->codeB"
                  ],
                  [
                      "request:body:{code: .+}->codeC",
                      "body:{other: .+}->codeD"
                  ]
               ]
            }
        """.trimIndent()

        val result = testObject.parse_v4(body)

        Assert.assertTrue(result.containsKey("aa"))
        Assert.assertEquals(1, result["aa"]!!.seqSteps.size)
        val firstSeq = result["aa"]!!.seqSteps.first()
        Assert.assertTrue(firstSeq.any { it.act_name == "codeA" })
        Assert.assertTrue(firstSeq.any { it.act_name == "codeB" })
    }

    @Test
    fun p4_numberCmdTest() {
        val tObj = P4Action()

        val inCmds = arrayOf(
            ("4" to "4"),
            ("5" to ">4"),
            ("5" to ">=4"),
            ("3" to "<4"),
            ("3" to "<=4"),
            ("5" to "4..5"),
            ("9" to "3,4..5, 9")
        )

        var test = 0
        inCmds.forEach { (input, cmd) ->
            println("Test #$test, $cmd")
            val result = tObj.processUseNumber(input, cmd)
            Assert.assertEquals(
                input,
                result.toString()
            )
            test++
        }
    }

    class P4MockData {
        var envChaptersUses: MutableMap<String, Int>? = null
        var envChapterUse: Int? = null

        var boundVars: MutableMap<String, String>? = null

        // todo; expChaptersUses
        var expChaptersUses: MutableMap<String, Int>? = null
        var expChapterUse: Int? = null

        var in_headers_raw: List<Pair<String, String>>? = null
            internal set(value) {
                field = value
                in_headers = value?.toHeaders
            }
        var in_headers: Headers? = null
        var in_body: String? = null

        var out_headers_raw: Map<String, List<String>>? = null
            internal set(value) {
                field = value
                out_headers = value?.toHeaders_dupKeys
            }

        var out_headers: Headers? = null
        var out_body: String? = null

        constructor()

        constructor(config: (P4MockData) -> Unit) {
            config.invoke(this)
            in_body = in_body?.also { it.trimIndent() }
            out_body = out_body?.also { it.trimIndent() }
        }
    }

    private val envBoundVars = mapOf(
        "confirm" to "false",
        "wwA" to "zzA",
        "wwB" to "zzB",
        "qCC" to "xxC"
    )

    private val env_outHeads = mapOf(
        "b1" to listOf("bbb1"),
        "b2" to listOf("bbb2"),
        "qq" to listOf("dup_1", "dup_2"),
        "33" to listOf("bbbC")
    )

    private fun createEnv(): P4MockData {
        return P4MockData {
            it.boundVars = envBoundVars.toMutableMap()

            it.in_headers_raw = listOf(
                "a1" to "aaa1",
                "a2" to "aaa2",
                "22" to "aaaC"
            )
            it.in_body = """
                code: 14
                other: 12
            """.trimIndent()
            it.out_headers_raw = env_outHeads
            it.out_body = """
                test: 22
                final: 44
            """.trimIndent()
        }
    }

    private class MockSet(
        val name: String,
        val commands: List<String>,
        mockDataConfig: (P4MockData) -> Unit
    ) {
        operator fun component1() = name
        operator fun component2() = commands
        operator fun component3() = mockData

        val mockData: P4MockData = P4MockData()

        init {
            mockDataConfig.invoke(mockData)
        }
    }

    private fun List<String>.toMockSet(name: String = "", mockSetup: (P4MockData) -> Unit): MockSet =
        MockSet(name, this, mockSetup)

    /**
     * Appended command to conditionals to ensure they were successful in matching
     */
    private val p4CondChk = "var[confirm]->{true}"

    /**
     * Broad list of command combinations and expected results
     * @param returns List<Input commands, expected output env>
     */
    private val mockSuiteCommands: List<MockSet>
        get() {
            val runTests: MutableList<MockSet> = mutableListOf()

            // 0. Baseline
            arrayOf(
                // expect no changes from input env to result env
                listOf("").toMockSet("Baseline") {
                    val defaultEnv = createEnv()
                    it.envChapterUse = 2
                    it.expChapterUse = 2
                    it.boundVars = defaultEnv.boundVars
                    it.in_headers = defaultEnv.in_headers
                    it.in_body = defaultEnv.in_body
                    it.out_headers = defaultEnv.out_headers
                    it.out_body = defaultEnv.out_body
                }
            )
                .also { runTests.addAll(it) }

            // 1. Requests
            arrayOf(
                // 1.1 Heads
                // 1.1.1 Conditionals
                listOf("?request:head", p4CondChk).toMockSet("1.1.1.a") {
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },
                listOf("?request:head[a1]", p4CondChk).toMockSet("1.1.1.b") {
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },
                listOf("?request:head[a1]:{a+1}", p4CondChk).toMockSet("1.1.1.c") {
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },
                listOf("?request:head:{a1}", p4CondChk).toMockSet("1.1.1.d") {
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },

                // 1.1.2 Actions
                listOf("request:head->result").toMockSet("1.1.2.a") {
                    it.boundVars = (envBoundVars + ("result" to "true")).toMutableMap()
                },
                listOf("request:head[a1]->result").toMockSet("1.1.2.b") {
                    it.boundVars = (envBoundVars + ("result" to "aaa1")).toMutableMap()
                },
                listOf("request:head[a1]:{a+}->result").toMockSet("1.1.2.c") {
                    it.boundVars = (envBoundVars + ("result" to "aaa")).toMutableMap()
                },
                listOf("request:head:{a\\d}->result").toMockSet("1.1.2.d") {
                    it.boundVars = (envBoundVars + ("result" to "true")).toMutableMap()
                },

                // 1.2 Body
                // 1.2.1 Conditionals
                listOf("?request:body", p4CondChk).toMockSet("1.2.1.a") {
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },
                listOf("?request:body:{code.+\\d+}", p4CondChk).toMockSet("1.2.1.b") {
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },

                // 1.2.2 Actions
                listOf("request:body->result").toMockSet("1.2.2.a") {
                    it.boundVars = (envBoundVars + ("result" to "true")).toMutableMap()
                },
                listOf("request:body:{code.+?(\\d+)}->result").toMockSet("1.2.2.b") {
                    it.boundVars = (envBoundVars + ("result" to "14")).toMutableMap()
                }
            )
                .also { runTests.addAll(it) }

            // 2. Response
            arrayOf(
                // 2.1 Heads
                // 2.1.1 Conditionals
                listOf("?response:head", p4CondChk).toMockSet("2.1.1.a") {
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },
                listOf("?response:head[b1]", p4CondChk).toMockSet("2.1.1.b") {
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },
                listOf("?response:head[b1]:{b+\\d}", p4CondChk).toMockSet("2.1.1.c") {
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },
                listOf("?response:head:{b\\d}", p4CondChk).toMockSet("2.1.1.d") {
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },

                // 2.1.2 Actions
                // 2.1.2.A to Var
                listOf("response:head->result").toMockSet("2.1.2.A.a") {
                    it.boundVars = (envBoundVars + ("result" to "true")).toMutableMap()
                },
                listOf("response:head[b1]->result").toMockSet("2.1.2.A.b") {
                    it.boundVars = (envBoundVars + ("result" to "bbb1")).toMutableMap()
                },
                listOf("response:head[b1]:{b+}->result").toMockSet("2.1.2.A.c") {
                    it.boundVars = (envBoundVars + ("result" to "bbb")).toMutableMap()
                },
                listOf("response:head:{b\\d}->result").toMockSet("2.1.2.A.d") {
                    it.boundVars = (envBoundVars + ("result" to "true")).toMutableMap()
                },

                // 2.1.2.B to Self
                listOf("response:head[b1]->{dd}").toMockSet("2.1.2.B.a") {
                    it.out_headers_raw = env_outHeads + ("b1" to listOf("dd"))
                },
                listOf("response:head[b1]:{b+}->{dd}").toMockSet("2.1.2.B.b") {
                    it.out_headers_raw = env_outHeads + ("b1" to listOf("dd1"))
                },
                listOf("response:head:{[a-z]\\d}->{dd}").toMockSet("2.1.2.B.c") {
                    it.out_headers_raw = env_outHeads + arrayOf(
                        "b1" to listOf("dd"),
                        "b2" to listOf("dd")
                    )
                },

                // 2.2 Body
                // 2.2.1 Conditionals
                listOf("?response:body", p4CondChk).toMockSet("2.2.1.a") {
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },
                listOf("?response:body:{test: \\d+}", p4CondChk).toMockSet("2.2.1.b") {
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },

                // 2.2.2 Actions
                // 2.2.2.A to Var
                listOf("response:body->result").toMockSet("2.2.2.A.a") {
                    it.boundVars = (envBoundVars + ("result" to "true")).toMutableMap()
                },
                listOf("response:body:{test: (\\d+)}->result").toMockSet("2.2.2.A.b") {
                    it.boundVars = (envBoundVars + ("result" to "22")).toMutableMap()
                },

                // 2.2.2.B to Self
                listOf("response:body->{body text}").toMockSet("2.2.2.B.a") {
                    it.out_body = "body text"
                },
                listOf("response:body:{(test: )(\\d+)}->{@{1}done}").toMockSet("2.2.2.B.b") {
                    it.out_body = """
                            test: done
                            final: 44
                        """.trimIndent()
                }
            )
                .also { runTests.addAll(it) }

            // 3. Variables
            arrayOf(
                // 3.1 Conditionals
                listOf("?var", p4CondChk).toMockSet("3.1.a") {
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },
                listOf("?var[wwA]", p4CondChk).toMockSet("3.1.b") {
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },
                listOf("?var:{w+A}", p4CondChk).toMockSet("3.1.c") {
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },
                listOf("?var[wwA]:{.+A}", p4CondChk).toMockSet("3.1.d") {
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },

                // 3.2 Actions
                // 3.2.1 to Var
                listOf("var->result").toMockSet("3.2.1.a") {
                    it.boundVars = (envBoundVars + ("result" to "true")).toMutableMap()
                },
                listOf("var:{w+A}->result").toMockSet("3.2.1.b") {
                    it.boundVars = (envBoundVars + ("result" to "true")).toMutableMap()
                },
                listOf("var[wwA]->result").toMockSet("3.2.1.c") {
                    it.boundVars = (envBoundVars + ("result" to "zzA")).toMutableMap()
                },
                listOf("var[wwA]:{z+}->result").toMockSet("3.2.1.d") {
                    it.boundVars = (envBoundVars + ("result" to "zz")).toMutableMap()
                },

                // 3.2.2 to Self
                // 3.2.2.1 Basic
                listOf("var:{w+.}->{dd}").toMockSet("3.2.2.1.a") {
                    it.boundVars = (envBoundVars +
                            arrayOf("wwA" to "dd", "wwB" to "dd"))
                        .toMutableMap()
                },
                listOf("var[wwA]->{dd}").toMockSet("3.2.2.1.b") {
                    it.boundVars = (envBoundVars + ("wwA" to "dd")).toMutableMap()
                },
                listOf("var[wwA]:{zz}->{dd}").toMockSet("3.2.2.1.c") {
                    it.boundVars = (envBoundVars + ("wwA" to "ddA")).toMutableMap()
                },

                // 3.2.2.2 Clearing the variable
                listOf(
                    "var[testA]->{123}",// variable we will clear
                    "var[testB]->{456}",// proof the variables are being set
                    "var[testA]:{.+}->{}"// clear the variable
                ).toMockSet("3.2.2.2.a") {
                    it.boundVars = (envBoundVars + ("testB" to "456")).toMutableMap()
                }
            )
                .also { runTests.addAll(it) }

            // 4. Uses
            arrayOf(
                // 4.1 Conditionals
                // 4.1.1 Basic maths
                listOf("?use:{3}", p4CondChk).toMockSet("4.1.1.a") {
                    it.envChapterUse = 3
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },
                listOf("?use:{>3}", p4CondChk).toMockSet("4.1.1.b") {
                    it.envChapterUse = 5
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },
                listOf("?use:{>=3}", p4CondChk).toMockSet("4.1.1.c") {
                    it.envChapterUse = 5
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },
                listOf("?use:{<3}", p4CondChk).toMockSet("4.1.1.d") {
                    it.envChapterUse = 2
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },
                listOf("?use:{3..4}", p4CondChk).toMockSet("4.1.1.e") {
                    it.envChapterUse = 3
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },
                listOf("?use:{3,5..6,9}", p4CondChk).toMockSet("4.1.1.f") {
                    it.envChapterUse = 6
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },

                // 4.1.2 Accessing "other"'s use info
                // 4.1.2.1 Testing is another chapter exists
                // 4.1.2.1.A Pass
                listOf("?use[other]", p4CondChk).toMockSet("4.1.2.1.A") {
                    it.envChaptersUses = mutableMapOf(("other" to 3))
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },

                // 4.1.2.1.B Fail
                listOf("?use[something]", p4CondChk).toMockSet("4.1.2.1.B") {
                    it.envChaptersUses = mutableMapOf(("none" to 3))
                    it.boundVars = envBoundVars.toMutableMap()
                },

                // 4.1.2.2 Testing the value of another chapter
                // 4.1.2.2.A Pass
                listOf("?use[other]:{3}", p4CondChk).toMockSet("4.1.2.2.A") {
                    it.envChaptersUses = mutableMapOf(("other" to 3))
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },

                // 4.1.2.2.B Fail
                listOf("?use[other]:{5}", p4CondChk).toMockSet("4.1.2.2.B") {
                    it.envChaptersUses = mutableMapOf(("other" to 3))
                    it.boundVars = envBoundVars.toMutableMap()
                },

                // 4.2 Actions
                // 4.2.1 to Var
                // 4.2.1.A Pass, save to var
                listOf("use:{3,5..6,9}->result").toMockSet("4.2.1.A") {
                    it.envChapterUse = 9
                    it.boundVars = (envBoundVars + ("result" to "9")).toMutableMap()
                },

                // 4.2.1.B Fail, use default var
                listOf(
                    "use:{>6}->result",
                    "var[result]:{false}->{}",
                    "var[test]->{@{result|'none'}}"
                ).toMockSet("4.2.1.B") {
                    it.envChapterUse = 2
                    it.boundVars = (envBoundVars + ("test" to "none")).toMutableMap()
                },

                // 4.2.2 to Self
                // 4.2.2.1 set the value only
                // 4.2.2.1.A Pass
                listOf("use->{8}").toMockSet("4.2.2.1.A") {
                    it.envChapterUse = 4
                    it.expChapterUse = 8
                },
                // 4.2.2.1.B Fail
                listOf("use->{w}").toMockSet("4.2.2.1.B") {
                    it.envChapterUse = 4
                    it.expChapterUse = 4
                },

                // 4.2.2.2 Ensure range, then set
                // 4.2.2.2.A Pass
                listOf("use:{3,5..6,9}->{2}").toMockSet("4.2.2.2.A") {
                    it.envChapterUse = 5
                    it.expChapterUse = 2
                },

                // 4.2.2.2.B Fail
                listOf("use:{3,5..6,9}->{w}").toMockSet("4.2.2.2.B") {
                    it.envChapterUse = 5
                    it.expChapterUse = 5
                }
            )
                .also { runTests.addAll(it) }

            // 5. Conditional Types
            arrayOf(
                // 5.1 Require true
                // 5.1.A & continue
                listOf(
                    "?response:body:{test: (\\d+)}->&hold", // collect the value to scoped
                    "var[hold]:{\\d+}->result"// export variable for assert testing
                ).toMockSet("5.1.A") {
                    it.boundVars = (envBoundVars + ("result" to "22")).toMutableMap()
                },

                // 5.1.B no continue
                listOf(
                    "?response:body:{other: (\\d+)}->&hold",
                    "var[hold]:{\\d+}->result"
                ).toMockSet("5.1.B") {
                    it.boundVars = envBoundVars.toMutableMap()
                },

                // 5.2 Require false
                // 5.2.A & continue
                listOf(
                    "!response:body:{other: (\\d+)}->&hold",// no match (as requested),'true' -> &hold
                    "?var[hold]->result"// if we saved to &hold, then export it
                ).toMockSet("5.2.A") {
                    it.boundVars = (envBoundVars + ("result" to "true")).toMutableMap()
                },

                // 5.2.B no continue
                listOf(
                    // source will passed, which we don't want
                    "!response:body:{test: (\\d+)}->FTest",
                    // `hold` will have no data, so this will fail. Thus `result` will not be set
                    "?var[FTest]->result"
                ).toMockSet("5.2.B") {
                    it.boundVars = envBoundVars.toMutableMap()
                },

                // 5.3 Optional Require true
                // 5.3.A pass, continue
                listOf(
                    // We expect this to pass; `FTest` will be set
                    "~?response:body:{test: (\\d+)}->FTest",
                    // but this should still run
                    "var[confirm]->{true}"
                ).toMockSet("5.3.A") {
                    it.boundVars = (envBoundVars + arrayOf(
                        "FTest" to "22",
                        "confirm" to "true"
                    )).toMutableMap()
                },

                // 5.3.B fail, continue
                listOf(
                    // We expect this to fail, and `FTest` NOT be set
                    "~?response:body:{none: (\\d+)}->FTest",
                    // but this should still run
                    "var[confirm]->{true}"
                ).toMockSet("5.3.B") {
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },

                // 5.4 Optional Require false
                // 5.4.A pass, continue
                listOf(
                    "~!response:body:{none: (\\d+)}->FTest",
                    "var[confirm]->{true}"
                ).toMockSet("5.4.A") {
                    it.boundVars = (envBoundVars + arrayOf(
                        "FTest" to "true",
                        "confirm" to "true"
                    )).toMutableMap()
                },

                // 5.4.B fail, continue
                listOf(
                    "~!response:body:{test: (\\d+)}->FTest",
                    "var[confirm]->{true}"
                ).toMockSet("5.4.B") {
                    it.boundVars = (envBoundVars + ("confirm" to "true")).toMutableMap()
                },

                // 5.5 Dependent optionals
                // 5.5.A Parent passes, dependent runs, continue
                listOf(
                    // this must pass (which it will)
                    "~?request:body:{code.+?(\\d+)}->&hold",
                    // to trigger this step
                    "~var[test]->{@{hold}}",
                    // this will always run
                    "var[extra]->{true}"
                ).toMockSet("5.5.A") {
                    it.boundVars = (envBoundVars + arrayOf(
                        "test" to "14",
                        "extra" to "true"
                    )).toMutableMap()
                },

                // 5.5.B Parent fails, dependent does not run, continue
                listOf(
                    // expect this to fail
                    "~?request:body:{none.+?(\\d+)}->&hold",
                    // thus this won't run
                    "~var[test]->{@{hold}}",
                    // this will always run
                    "var[extra]->{true}"
                ).toMockSet("5.5.B") {
                    it.boundVars = (envBoundVars + ("extra" to "true")).toMutableMap()
                }
            )
                .also { runTests.addAll(it) }

            // 6. Data manipulation
            arrayOf(
                // 6.1 Data collection to output
                listOf(
                    "request:body:{code.+?(\\d+)}->&hold1",// grab "code" data
                    "request:body:{other.+?(\\d+)}->&hold2",// grab "other" data
                    "?&var[hold1]",// ensure we have both items of data
                    "?&var[hold2]",
                    // Create a local var which we do var processing on
                    "&var[hold3]->{_@{hold1}@{hold2}}",
                    // use the new var, plus add extra chars
                    "response:body:{(test.+?)(\\d+)}->{@{1}+@{hold3}+}"
                ).toMockSet("6.1.A") {
                    it.out_body = """
                        test: +_1412+
                        final: 44
                    """.trimIndent()
                }
            )
                .also { runTests.addAll(it) }

            // 7. Misc
            arrayOf(
                // including "}->" in the "save to" and "source match"
                listOf(
                    "response:body:{test: 22}->{qqq}->c}",
                    "response:body:{qqq}\\->c}->result"
                ).toMockSet("7.A") {
                    it.boundVars = (envBoundVars + ("result" to "qqq}->c")).toMutableMap()
                }
            )
                .also { runTests.addAll(it) }

            return runTests
        }

    @Test
    fun fullTestSuite() {
        val testObj = P4Action()
        var testsRun = 0

        val toTest = mockSuiteCommands
        val testMax = toTest.size

        println("Running full-suit test: $testMax items".cyan())
        println("=".repeat(30))

        toTest.forEach { (setName, commands, exp_env) ->
            val sb = StringBuilder()

            sb.appendln(
                "Test #%s%s".format(
                    testsRun,
                    if (setName.isEmpty()) "" else ": $setName"
                ).yellow()
            )
            sb.appendln("Testing commands:")
            val printCmdLines = commands.joinToString("\n") {
                (if (it.isEmpty()) "{empty string}" else it)
                    .ensurePrefix("- ")
                    .cyan()
            }
            sb.appendln(printCmdLines)
            println(sb.toString())

            val p4MockEnv = createEnv()
            testObj.setup { setup ->
                setup.bounds = mockk {
                    exp_env.envChaptersUses?.also { envChaps ->
                        val mockChaps =
                            envChaps.map { (key, value) ->
                                key to BoundChapterItem().also {
                                    it.stateUse = value
                                }
                            }.toMap().toMutableMap()

                        val dataSlot = slot<String>()
                        every { boundData[capture(dataSlot)] } answers {
                            mockChaps[dataSlot.captured]
                        }
                        every { boundData } returns mockChaps
                    }

                    p4MockEnv.boundVars?.also { bVars ->
                        every { boundVars } returns bVars
                    }
                }

                exp_env.envChapterUse?.also { use ->
                    setup.chapItems = BoundChapterItem() { it.stateUse = use }
                }

                p4MockEnv.in_headers?.also { setup.in_headers = it }
                p4MockEnv.in_body?.also { setup.in_body = it }
                p4MockEnv.out_headers?.also { setup.out_headers = it }
                p4MockEnv.out_body?.also { setup.out_body = it }
            }

            // Process inputs for results
            val steps = commands.asSequence()
                .map { it to parse4Class.parseToSteps(it) }
                .onEach {
                    fun errorMsg() = "Processed command does not match input" +
                            "\nInput: ${it.first}" +
                            "\nParsed: ${it.second}\n"

                    Assert.assertEquals(
                        errorMsg(),
                        it.first,
                        it.second.toString()
                    )
                }
                .map { it.second }
                .toList()

            testObj.processCommands(steps)

            // Test params with expected outputs
            if (exp_env.boundVars != null) {
                val resultVars = p4MockEnv.boundVars.orEmpty()
                exp_env.boundVars!!.forEach { (key, value) ->
                    fun errorMsg_1() = "Missing:\n  [$key] = $value" +
                            "\n\nResult Vars:\n${resultVars.toJson}\n"

                    Assert.assertTrue(
                        errorMsg_1(),
                        resultVars.containsKey(key)
                    )
                    fun errorMsg_2() =
                        "Result's Bound Variables don't match" +
                                "\nExpected: [$key] = $value" +
                                "\nFound: [$key] = ${resultVars[key]}" +
                                "\n\nResult vars:\n${resultVars.toJson}\n\n"

                    Assert.assertEquals(
                        errorMsg_2(),
                        value, resultVars[key]
                    )
                }

                val lingerKeys = resultVars.keys.minus(exp_env.boundVars!!.keys)
                if (lingerKeys.isNotEmpty()) {
                    println("Lingering keys".blue())
                    lingerKeys.forEach { key ->
                        val kVal = resultVars[key]
                            .let {
                                if (it.isNullOrEmpty())
                                    "{Empty string}" else it
                            }
                        println("+ [$key] = $kVal".blue())
                    }
                    println()
                }
            }

            if (exp_env.out_headers != null) {
                val resultHeaders = testObj.out_headers
                    .toMultimap().orEmpty()
                exp_env.out_headers!!.toMultimap().orEmpty().forEach { t, u ->
                    Assert.assertTrue(
                        "Missing Header key: $t",
                        resultHeaders.containsKey(t)
                    )

                    val rHeadVals = resultHeaders[t]!!
                    fun errorMsg(expKey: String) = "Missing header value: {$t : $expKey}" +
                            "\n\n${resultHeaders.toJson}"

                    u.forEach {
                        Assert.assertTrue(
                            errorMsg(it),
                            rHeadVals.contains(it)
                        )
                    }
                }
            }

            if (exp_env.out_body != null) {
                Assert.assertEquals(
                    exp_env.out_body!!,
                    testObj.out_body
                )
            }

            if (exp_env.expChapterUse != null) {
                Assert.assertEquals(
                    exp_env.expChapterUse,
                    testObj.chapItems.stateUse
                )
            }
            testsRun++
        }
        println("Full test suite complete: $testsRun / $testMax".green())
    }
}
