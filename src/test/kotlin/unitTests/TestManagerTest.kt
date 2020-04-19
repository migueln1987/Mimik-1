package unitTests

import helpers.content
import io.mockk.*
import mimikMockHelpers.RecordedInteractions
import networkRouting.testingManager.*
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import org.junit.Assert
import org.junit.Test
import tapeItems.BlankTape
import java.nio.charset.Charset

class TestManagerTest {
    @Test
    fun observerScope() {
        val tape = BlankTape.Builder().build()
        val chap = RecordedInteractions {
            it.mockUses = 3
        }

        val pre = tape.run { chap.uses }
        var inPre = 0
        val newVal = 8
        var inPost = 0

        TestBounds("").observe(tape) {
            tape.apply {
                inPre = chap.uses
                chap.uses = newVal
                inPost = chap.uses
            }
        }

        val post = tape.run { chap.uses }

        Assert.assertEquals("Contain changes to within 'Observe'", pre, post)
        Assert.assertNotEquals("Changes in 'Observe' persist", inPre, inPost)
        Assert.assertEquals("New data in 'Observe' applies", inPost, newVal)
    }

    @Test
    fun observerUsesOriginalData() {
        val tape = BlankTape.Builder().build()
        val chap = RecordedInteractions {
            it.mockUses = 3
        }

        chap.mockUses = 1
        var obsData = 0

        TestBounds("").observe(tape) {
            tape.apply {
                obsData = chap.uses
            }
        }

        Assert.assertNotEquals(chap.mockUses, obsData)
    }

    @Test
    fun saveRequestVars() {
        val chapName = "chapName"
        val requestBodyStr = "this is a 123 test"
        val filterFindVars = mutableListOf(
            "\\d+" to "numberGrab",
            "^\\w+" to "firstWord"
        )

        val reqBody = object : RequestBody() {
            override fun contentType(): MediaType? = null

            override fun writeTo(p0: BufferedSink) {
                p0.writeString(requestBodyStr, Charset.defaultCharset())
            }
        }

        val mockRequest = mockk<okhttp3.Request> {
            every { body() } returns reqBody
        }

        val bounds = TestBounds("", mutableListOf())
        bounds.replacerData[chapName] = mockk {
            every { findVars } returns filterFindVars
        }

        val interaction = mockk<RecordedInteractions> {
            every { name } returns chapName
        }

        // Do the test!
        mockRequest.collectVars(bounds, interaction)

        Assert.assertTrue(bounds.boundVars.containsKey(filterFindVars[0].second))
        val varData_1 = bounds.boundVars[filterFindVars[0].second]
        Assert.assertEquals("123", varData_1)

        Assert.assertTrue(bounds.boundVars.containsKey(filterFindVars[1].second))
        val varData_2 = bounds.boundVars[filterFindVars[1].second]
        Assert.assertEquals("this", varData_2)
    }

    @Test
    fun saveResponseVars() {
        val chapName = "chapName"
        val responseBodyStr = "this is a 123 test"
        val filterFindVars = mutableListOf(
            "\\d+" to "numberGrab",
            "^\\w+" to "firstWord"
        )

        val mockResponse = mockk<okhttp3.Response> {
            val resp = this
            every { body() } returns mockk {
                every { contentType() } returns null
                every { close() } just runs
                every { bytes() } returns responseBodyStr.toByteArray()
                every { newBuilder() } returns mockk {
                    every { body(any()) } returns this
                    every { build() } returns mockk()
                }
            }
        }

        val bounds = TestBounds("", mutableListOf())
        bounds.replacerData[chapName] = mockk {
            every { findVars } returns filterFindVars
        }

        val interaction = mockk<RecordedInteractions> {
            every { name } returns chapName
        }

        mockResponse.collectVars(bounds, interaction)

        Assert.assertTrue(bounds.boundVars.containsKey(filterFindVars[0].second))
        val varData_1 = bounds.boundVars[filterFindVars[0].second]
        Assert.assertEquals("123", varData_1)

        Assert.assertTrue(bounds.boundVars.containsKey(filterFindVars[1].second))
        val varData_2 = bounds.boundVars[filterFindVars[1].second]
        Assert.assertEquals("this", varData_2)
    }

    @Test
    fun useVarsByIndexAndName() {
        val chapName = "chapName"
        val bodyOut = "abc def xyz"

        val boundReplace = mutableListOf(
            "f" to "P",                     // normal
            // "abc def xyz" -> "abc deP xyz"
            "x(.{2})" to "+@{1}_@{1}+",     // indexed
            // "abc deP xyz" -> "abc deP +yz_yz+"
            "(a)(?<tt>.)" to "@{1}333@{tt}",  // index and local named var
            // "abc deP +yz_yz+" -> "a333bc deP +yz_yz+"
            "d" to "@{testVar}"             // test bound named var
            // "a333bc deP +yz_yz+" -> "a333bc 456eP +yz_yz+"
        )

        val bounds = TestBounds("", mutableListOf())
            .apply {
                replacerData[chapName] = mockk {
                    every { replacers_body } returns boundReplace
                }
                boundVars["testVar"] = "456"
            }

        val interaction = mockk<RecordedInteractions> {
            every { name } returns chapName
        }

        val result = slot<ResponseBody>()

        val mockResponse = mockk<okhttp3.Response> response@{
            every { body() } returns mockk {
                every { contentType() } returns null
                every { close() } just runs
                every { bytes() } answers { bodyOut.toByteArray() }
                every { newBuilder() } answers {
                    mockk {
                        every { body(capture(result)) } returns this
                        every { build() } returns this@response
                    }
                }
            }
        }

        // Do the test, result is captured in body(input)
        mockResponse.replaceByTest(bounds, interaction)

        Assert.assertEquals(
            "a333bc 456eP +yz_yz+",
            result.captured.content()
        )
    }
}
