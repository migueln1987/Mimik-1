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

@Suppress("KDocUnresolvedReference", "PropertyName", "PrivatePropertyName")
class ParserTests {
    val testObject by lazy { TestManager() }

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

    private val regexPattern: String
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
      (?<vbound>
        (?:(?<vC>&)|(?<vB>%))?
        (?<vU>\^)?
      )?
      var
      (?:\[(?<vN>[a-zA-Z]\w*([?#@]*(?:_[\d#?]*)?))\])?
      (?::\{(?<vM>.+?)\}(?=->|\s|$))?
    )|
    (?<uType>use
      (?:\[(?<uN>[a-zA-Z][!-+\--~]*)\])?
      (?::\{(?<uM>[\d=,.<>]+)\})?
    )
)
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
        """.replace("""( {2}|\n|\r)""".toRegex(), "")

    private val validInputCombos = mapOf(
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
    private val invalidInputCombos = mapOf(
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

    private val inputRequireCond = mapOf(
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
                Parser_v4.parseToContents(value),
                items
            )
        }
    }

    private fun Parser_v4.compareParsedToExpected(
        input: String,
        parsed: List<MatcherResult>,
        expected: List<String>,
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
        Assert.assertEquals(regexPattern, Parser_v4.toString())
    }

    @Test
    fun parse_v4_ValidCombos() {
        validInputCombos.toResultMap()
            .forEach { (input, parsed, expected) ->
                Parser_v4.compareParsedToExpected(input, parsed, expected)
            }
    }

    //    @Test
    fun parse_v4_InvalidCombos() {
        invalidInputCombos.toResultMap()
            .forEach { (input, parsed, expected) ->
                Parser_v4.compareParsedToExpected(input, parsed, expected, false)
            }
    }

    @Test // If the input has no action, the it MUST be a conditional
    fun parse_v4_expectAsCondOrAct() {
        inputRequireCond.toResultMap()
            .forEach { (_, parsed, _) ->
                Assert.assertFalse(Parser_v4.isValid_Request(parsed))
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
                    if (s == 1) "cc"
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

        val result = Parser_v4.parseBody(body)

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
}
