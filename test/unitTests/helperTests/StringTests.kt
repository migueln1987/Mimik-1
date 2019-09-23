package unitTests.helperTests

import com.beust.klaxon.Parser
import com.google.gson.Gson
import helpers.ensurePrefix
import helpers.isJSONValid
import helpers.isTrue
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
        Assert.assertTrue(test.isJSONValid)
    }

    @Test
    fun isJson_NullString() {
        val test: String? = null
        Assert.assertTrue(test.isJSONValid)
    }

    class testClass(val string: String = "a", val int: Int = 0)

    @Test
    fun isJson_FromClass() {
        val testClassVal = testClass()
        val test = Gson().toJson(testClassVal)

        Assert.assertNotNull(test)
        Assert.assertTrue(test.isJSONValid)
    }

    @Test
    fun isJson_FromString_newlines() {
        val test = "{\\n    \"Data\": false\\n}"

        Assert.assertTrue(test.isJSONValid)
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

        Assert.assertTrue(test.isJSONValid)
    }
}
