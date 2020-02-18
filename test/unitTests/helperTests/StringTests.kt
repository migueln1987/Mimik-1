package unitTests.helperTests

import com.google.gson.Gson
import helpers.*
import org.junit.Assert
import org.junit.Test

class StringTests {

    @Test
    fun removePrefix_Empty() {
        val input = ""
        val test = input.removePrefix("-")
        Assert.assertEquals(test, "")
    }

    @Test
    fun removePrefix_Remove() {
        val input = "-test"
        val test = input.removePrefix("-")
        Assert.assertEquals(test, "test")
    }

    @Test
    fun ensurePrefix_Empty() {
        val input = ""
        val test = input.ensurePrefix("+")
        Assert.assertEquals(test, "+")
    }

    @Test
    fun ensurePrefix_Remove() {
        val input = "test"
        val test = input.ensurePrefix("+")
        Assert.assertEquals(test, "+test")
    }

    @Test
    fun nullableTrueString_Null() {
        val input: String? = null

        Assert.assertFalse(input.isTrue())
    }

    @Test
    fun nullableTrueString_True() {
        val inputs = arrayOf(
            "true",
            "True",
            "TRUE"
        )

        inputs.forEach {
            Assert.assertTrue(it.isTrue())
        }
    }

    @Test
    fun isJson_EmptyString() {
        val test = ""
        Assert.assertTrue(test.isValidJSON)
    }

    @Test
    fun isJson_NullString() {
        val test: String? = null
        Assert.assertTrue(test.isValidJSON)
    }

    class testClass(val string: String = "a", val int: Int = 0)

    @Test
    fun isJson_FromClass() {
        val testClassVal = testClass()
        val test = Gson().toJson(testClassVal)

        Assert.assertNotNull(test)
        Assert.assertTrue(test.isValidJSON)
    }

    @Test
    fun isJson_FromString_newlines() {
        val test = "{\\n    \"Data\": false\\n}"

        Assert.assertTrue(test.isValidJSON)
    }

    @Test
    fun isJson_FromString_arrays() {
        val test = """
            {
                 "array": [
                    "aaa",
                    "bbb",
                    "ccc"
                ]
            }
        """.trimIndent()

        Assert.assertTrue(test.isValidJSON)
    }

    @Test
    fun matches_RegMixedHard() {
        val input = "test 123 end"
        val regIn = """.+ \d+"""
        val mixedIn = ".+123"
        val hardIn = "test 123"
        val failIn = "fail"
        val emptyIn = ""

        val out_reg = regIn.matchResults(input)
        val out_mixed = mixedIn.matchResults(input)
        val out_hard = hardIn.matchResults(input)
        val out_empty = emptyIn.matchResults(input)
        val out_fail = failIn.matchResults(input)

        // == Check if the regex acquired any matches ==
        Assert.assertTrue(out_reg.hasMatches)
        Assert.assertTrue(out_mixed.hasMatches)
        Assert.assertTrue(out_hard.hasMatches)
        Assert.assertFalse(out_empty.hasMatches)
        Assert.assertFalse(out_fail.hasMatches)

        // Literal matches count: filter chars matching input string
        Assert.assertEquals(1, out_reg.first().litMatchCnt)
        Assert.assertEquals(3, out_mixed.first().litMatchCnt)
        Assert.assertEquals(8, out_hard.first().litMatchCnt)
        Assert.assertEquals(-1, out_empty.first().litMatchCnt)
        Assert.assertEquals(-1, out_fail.first().litMatchCnt)

        // literal str > mixed str > reg str > (fail | empty)
        Assert.assertTrue(out_hard.first().litMatchCnt > out_mixed.first().litMatchCnt)
        Assert.assertTrue(out_mixed.first().litMatchCnt > out_reg.first().litMatchCnt)
        Assert.assertTrue(out_reg.first().litMatchCnt > out_fail.first().litMatchCnt)
        Assert.assertEquals(out_empty.first().litMatchCnt, out_fail.first().litMatchCnt)

        // string which the filter matched
        Assert.assertEquals("test 123", out_reg.first().value)
        Assert.assertEquals("test 123", out_mixed.first().value)
        Assert.assertEquals("test 123", out_hard.first().value)
        Assert.assertEquals(null, out_empty.first().value)
        Assert.assertEquals(null, out_fail.first().value)

        Assert.assertFalse(out_reg.first().isLiteral)
        Assert.assertFalse(out_mixed.first().isLiteral)
        Assert.assertTrue(out_hard.first().isLiteral)
    }

    @Test
    fun matches_RegMatchEmptyIn() {
        val input = ""
        val regFilter = ".*" // we are expecting a string of any (or no) length

        val outReg = regFilter.matchResults(input)
        Assert.assertTrue(outReg.hasMatches)
        Assert.assertEquals(1, outReg.matchCount)
        outReg.forEach {
            Assert.assertEquals(input, it.value)
        }
    }

    @Test
    fun matches_spaces() {
        val input = " "
        val regFilter = " "

        val outReg = regFilter.matchResults(input)
        Assert.assertTrue(outReg.hasMatches)
        Assert.assertEquals(1, outReg.matchCount)
        outReg.forEach {
            Assert.assertEquals(input, it.value)
        }
    }

    @Test
    fun matches_blankSpaces() {
        val input = listOf(
            "insert",
            (0 until 5).joinToString(separator = "") { " " }, // insert 5 spaces
            "spaces"
        ).joinToString(separator = "")
        val regFilter = "  " // look for instances of 2 spaces

        val outReg = regFilter.matchResults(input)
        Assert.assertTrue(outReg.hasMatches)
        Assert.assertEquals(2, outReg.matchCount)
        outReg.forEach {
            Assert.assertEquals(regFilter, it.value)
        }
    }

    @Test
    fun matches_GroupAndIndex() {
        val input = "test 123 end"
        val regIn = "(none)?(?<group0>\\d+)(.+)"

        val regOut = regIn.matchResults(input)
        Assert.assertEquals(3, regOut.matchCount)

        // == Match by group name ==
        val nameTest = regOut["group0"]
        Assert.assertTrue(nameTest.isNotEmpty())
        Assert.assertEquals("123", nameTest.first().value)

        //  == By index ==
        val index_0 = regOut[0] // "all" match
        Assert.assertTrue(index_0.isNotEmpty())
        Assert.assertEquals("123 end", index_0.first().value)

        val index_1 = regOut[1] // 1st group, "(none)"
        Assert.assertTrue(index_1.isEmpty())
        Assert.assertEquals(null, index_1.firstOrNull()?.value)

        val index_2 = regOut[2] // 2nd group, "(?<group0>\d+)"
        Assert.assertTrue(index_2.isNotEmpty())
        Assert.assertEquals("123", index_2.first().value)
        Assert.assertEquals("group0", index_2.first().groupName)

        val index_3 = regOut[3] // 3rd group, "(.+)"
        Assert.assertTrue(index_3.isNotEmpty())
        Assert.assertEquals(" end", index_3.first().value)

        val index_4 = regOut[4] // non-existent group
        Assert.assertTrue(index_4.isEmpty())
    }

    @Test
    fun matches_ManyGroupsSameName() {
        val variableMatch = """@\{(?<content>\w+)\}"""
        val input = """@{1}bbb@{2}"""

        val matchResults = variableMatch.matchResults(input)
        val contentGroups = matchResults["content"]
        Assert.assertTrue(
            contentGroups.all { it.groupName == "content" }
        )
        Assert.assertEquals(2, contentGroups.size)
        Assert.assertEquals("1", contentGroups[0].value)
        Assert.assertEquals("2", contentGroups[1].value)
    }
}
