package unitTests.helperTests

import com.google.gson.Gson
import helpers.ensurePrefix
import helpers.isValidJSON
import helpers.isTrue
import helpers.match
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

        val out_reg = regIn.match(input)
        val out_mixed = mixedIn.match(input)
        val out_hard = hardIn.match(input)
        val out_fail = failIn.match(input)
        val out_empty = emptyIn.match(input)

        Assert.assertEquals(1, out_reg.third)
        Assert.assertEquals(3, out_mixed.third)
        Assert.assertEquals(7, out_hard.third)
        Assert.assertEquals(0, out_fail.third)

        Assert.assertTrue(out_hard.third > out_mixed.third)
        Assert.assertTrue(out_mixed.third > out_reg.third)
        Assert.assertTrue(out_reg.third > out_fail.third)
        Assert.assertEquals(out_fail.third, out_empty.third)

        Assert.assertEquals("t 123", out_reg.first)
        Assert.assertEquals(" 123", out_mixed.first)
        Assert.assertEquals("test 123", out_hard.first)
        Assert.assertEquals(null, out_fail.first)
        Assert.assertEquals(null, out_empty.first)

        Assert.assertFalse(out_reg.second)
        Assert.assertFalse(out_mixed.second)
        Assert.assertTrue(out_hard.second) // literal compatible
    }
}
