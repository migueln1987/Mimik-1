@file:Suppress("RemoveRedundantQualifierName")

package unitTests.testManagerTests

import mimik.helpers.content
import mimik.helpers.parser.Parser_v4
import io.mockk.*
import mimik.mockHelpers.RecordedInteractions
import mimik.mockHelpers.SeqActionObject
import mimik.networkRouting.testingManager.BoundChapterItem
import mimik.networkRouting.testingManager.TestBounds
import mimik.networkRouting.testingManager.boundActions
import mimik.networkRouting.testingManager.observe
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.BufferedSink
import org.junit.Assert
import org.junit.Test
import mimik.tapeItems.BaseTape
import java.nio.charset.Charset
import kotlin.test.fail

class TestManagerTest {

    @Test
    fun observerScope() {
        val tape = BaseTape.Builder().build()
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
        val tape = BaseTape.Builder().build()
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
    fun boundActions_test() {
        val chapName = "chapName"
        val bodyStr = "this is a 123 test"

        val requestActions = listOf(
            "request:body:{\\d+}->%numberGrab",
            "request:body:{\\w+}->%firstWord",
            "response:head[something]->{code_@{%numberGrab}}",
            "response:body:{(.+)test}->{@{1}final}"
        )

        val expectedResults = listOf(
            Triple("var", "numberGrab", "123"),
            Triple("var", "firstWord", "this"),
            Triple("head", "something", "code_123"),
            Triple("body", "", "this is a 123 final")
        )

        val bounds = TestBounds("", mutableListOf())
        bounds.boundData[chapName] = BoundChapterItem().also { chap ->
            requestActions.map { Parser_v4.parseToCommand(it) }
                .also { cmds ->
                    chap.seqSteps.add(
                        SeqActionObject(true) { it.Commands.addAll(cmds) }
                    )
                }
        }

        val mockRequest = mockk<okhttp3.Request> {
            every { headers } returns mockk()
            every { body } returns object : RequestBody() {
                override fun contentType(): MediaType? = null
                override fun writeTo(sink: BufferedSink) {
                    sink.writeString(bodyStr, Charset.defaultCharset())
                }
            }
        }

        val resultBody = slot<ResponseBody>()
        val resultHeader = slot<Headers>()

        val mockResponse = mockk<okhttp3.Response> {
            every { headers } returns mockk {
                every { size } returns 0
                every { values(any()) } returns listOf()
                every { toMultimap() } returns mapOf()
            }
            every { newBuilder() } returns mockk {
                every { headers(capture(resultHeader)) } returns mockk()
                every { body(capture(resultBody)) } returns mockk()
                every { build() } returns mockk()
            }
            every { body } returns mockk {
                every { contentLength() } returns bodyStr.length.toLong()
                every { bytes() } returns bodyStr.toByteArray()
                every { close() } just runs
                every { contentType() } returns "text/plain".toMediaTypeOrNull()
            }
        }

        val interaction = mockk<RecordedInteractions> {
            every { name } returns chapName
            every { seqActions } returns arrayListOf()
        }

        // Do the test!
        mockResponse.boundActions(mockRequest, bounds, interaction)

        expectedResults.forEach { (type, xKey, xVal) ->
            when (type) {
                "var" -> {
                    Assert.assertTrue(bounds.scopeVars.containsKey(xKey))
                    Assert.assertEquals(
                        bounds.scopeVars[xKey],
                        xVal
                    )
                }

                "head" -> {
                    val data = resultHeader.captured

                    Assert.assertTrue(data[xKey] != null)
                    Assert.assertEquals(
                        xVal,
                        data[xKey]
                    )
                }

                "body" -> {
                    val rBody = resultBody.captured.content()

                    Assert.assertEquals(
                        xVal,
                        rBody
                    )
                }

                else -> fail("Unknown type: $type")
            }
        }
    }
}
