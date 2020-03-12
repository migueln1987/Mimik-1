package unitTests.testManagerTests

import helpers.content
import helpers.parser.Parser_v4
import io.mockk.*
import mimikMockHelpers.RecordedInteractions
import networkRouting.testingManager.*
import okhttp3.*
import okio.BufferedSink
import org.junit.Assert
import org.junit.Test
import tapeItems.BaseTape
import java.nio.charset.Charset
import kotlin.test.fail

class TestManagerTest {
    val p4Parser by lazy { Parser_v4() }

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

        val actionsResults = listOf(
            Triple(
                "request:body:{\\d+}->numberGrab",
                "var",
                "numberGrab : 123"
            ),
            Triple(
                "request:body:{\\w+}->firstWord",
                "var",
                "firstWord : test"
            ),
            Triple(
                "response:head[something]->{code_@{numberGrab}}",
                "head",
                "something : code_123"
            ),
            Triple(
                "response:body:{(.+)test}->{@{1}final}",
                "body",
                "this is a 123 final"
            )
        )

        val bounds = TestBounds("", mutableListOf())
        bounds.boundData[chapName] = BoundChapterItem().also { chap ->
            actionsResults
                .map { (cmd, _, _) -> p4Parser.parseToSteps(cmd) }
                .also { chap.seqSteps.add(it) }
        }

        val mockRequest = mockk<okhttp3.Request> {
            every { headers() } returns mockk()
            every { body() } returns object : RequestBody() {
                override fun contentType(): MediaType? = null
                override fun writeTo(p0: BufferedSink) {
                    p0.writeString(bodyStr, Charset.defaultCharset())
                }
            }
        }

        val resultBody = slot<ResponseBody>()
        val resultHeader = slot<Headers>()

        val mockResponse = mockk<okhttp3.Response> {
            every { headers() } returns mockk {
                every { values(any()) } returns listOf()
                every { toMultimap() } returns mapOf()
            }
            every { newBuilder() } returns mockk {
                every { headers(capture(resultHeader)) } returns mockk()
                every { body(capture(resultBody)) } returns mockk()
                every { build() } returns mockk()
            }
            every { body() } returns mockk {
                every { contentLength() } returns bodyStr.length.toLong()
                every { bytes() } returns bodyStr.toByteArray()
                every { close() } just runs
                every { contentType() } returns MediaType.parse("text/plain")
            }
        }

        val interaction = mockk<RecordedInteractions> {
            every { name } returns chapName
        }

        // Do the test!
        mockResponse.boundActions(mockRequest, bounds, interaction)

        actionsResults.forEach { (_, type, expected) ->
            // split the key/ values for vars and heads
            val (xKey, xVal) = expected.split(" : ")
                .let { if (it.size == 2) (it[0] to it[1]) else (it[0] to "") }

            when (type) {
                "var" -> {
                    Assert.assertTrue(bounds.boundVars.containsKey(xKey))
                    Assert.assertEquals(
                        bounds.boundVars[xKey],
                        xVal
                    )
                }

                "head" -> {
                    val data = resultHeader.captured

                    Assert.assertTrue(data.get(xKey) != null)
                    Assert.assertEquals(
                        xVal,
                        data.get(xKey)
                    )
                }

                "body" -> {
                    val rBody = resultBody.captured.content()

                    Assert.assertEquals(
                        expected,
                        rBody
                    )
                }

                else -> fail("Unknown type: $type")
            }
        }
    }
}
