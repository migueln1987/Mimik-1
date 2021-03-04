package unitTests.helperTests

import com.google.gson.Gson
import kotlinUtils.ensurePrefix
import kotlinUtils.isStrTrue
import kotlinUtils.isValidJSON
import mimik.helpers.matchers.matchResults
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
        Assert.assertFalse(input.isStrTrue())
    }

    @Test
    fun nullableTrueString_True() {
        val inputs = arrayOf(
            "true",
            "True",
            "TRUE"
        )

        inputs.forEach {
            Assert.assertTrue(it.isStrTrue())
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

    @Test
    fun isJson_FromClass() {
        @Suppress("unused")
        class TestClass(val string: String = "a", val int: Int = 0)

        val testClassVal = TestClass()
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

        val outReg = regIn.matchResults(input)
        val outMixed = mixedIn.matchResults(input)
        val outHard = hardIn.matchResults(input)
        val outEmpty = emptyIn.matchResults(input)
        val outFail = failIn.matchResults(input)

        // == Check if the regex acquired any matches ==
        Assert.assertTrue(outReg.hasMatches)
        Assert.assertTrue(outMixed.hasMatches)
        Assert.assertTrue(outHard.hasMatches)
        Assert.assertFalse(outEmpty.hasMatches)
        Assert.assertFalse(outFail.hasMatches)

        // Literal matches count: filter chars matching input string
        Assert.assertEquals(1, outReg.first().litMatchCnt)
        Assert.assertEquals(3, outMixed.first().litMatchCnt)
        Assert.assertEquals(8, outHard.first().litMatchCnt)
        Assert.assertEquals(-1, outEmpty.first().litMatchCnt)
        Assert.assertEquals(-1, outFail.first().litMatchCnt)

        // literal str > mixed str > reg str > (fail | empty)
        Assert.assertTrue(outHard.first().litMatchCnt > outMixed.first().litMatchCnt)
        Assert.assertTrue(outMixed.first().litMatchCnt > outReg.first().litMatchCnt)
        Assert.assertTrue(outReg.first().litMatchCnt > outFail.first().litMatchCnt)
        Assert.assertEquals(outEmpty.first().litMatchCnt, outFail.first().litMatchCnt)

        // string which the filter matched
        Assert.assertEquals("test 123", outReg.first().value)
        Assert.assertEquals("test 123", outMixed.first().value)
        Assert.assertEquals("test 123", outHard.first().value)
        Assert.assertEquals(null, outEmpty.first().value)
        Assert.assertEquals(null, outFail.first().value)

        Assert.assertFalse(outReg.first().isLiteral)
        Assert.assertFalse(outMixed.first().isLiteral)
        Assert.assertTrue(outHard.first().isLiteral)
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
        val index0 = regOut[0] // "all" match
        Assert.assertTrue(index0.isNotEmpty())
        Assert.assertEquals("123 end", index0.first().value)

        val index1 = regOut[1] // 1st group, "(none)"
        Assert.assertTrue(index1.isEmpty())
        Assert.assertEquals(null, index1.firstOrNull()?.value)

        val index2 = regOut[2] // 2nd group, "(?<group0>\d+)"
        Assert.assertTrue(index2.isNotEmpty())
        Assert.assertEquals("123", index2.first().value)
        Assert.assertEquals("group0", index2.first().groupName)

        val index3 = regOut[3] // 3rd group, "(.+)"
        Assert.assertTrue(index3.isNotEmpty())
        Assert.assertEquals(" end", index3.first().value)

        val index4 = regOut[4] // non-existent group
        Assert.assertTrue(index4.isEmpty())
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
