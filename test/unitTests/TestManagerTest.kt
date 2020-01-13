package unitTests

import helpers.content
import helpers.toJson
import io.mockk.*
import kolor.green
import mimikMockHelpers.RecordedInteractions
import networkRouting.testingManager.observe
import networkRouting.testingManager.TestBounds
import networkRouting.testingManager.TestBounds.Companion.DataTypes.Body
import networkRouting.testingManager.TestBounds.Companion.DataTypes.Head
import networkRouting.testingManager.replaceByTest
import okhttp3.ResponseBody
import org.junit.Assert
import org.junit.Test
import tapeItems.BlankTape

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
    fun replaceBodyTest() {
        val chap = RecordedInteractions {
            it.chapterName = "test1"
        }

        val bodyStr = "aaaabbbbcccc"

        val bodySlot = slot<ResponseBody>()

        val resp = mockk<okhttp3.Response> mResponse@{
            every { body() } returns mockk {
                every { bytes() } returns bodyStr.toByteArray()
                every { contentType() } returns mockk {
                    every { type() } returns "text"
                    every { charset() } returns Charsets.UTF_8
                }
            }
            every { newBuilder() } returns mockk {
                every { body(capture(bodySlot)) } returns this
                every { build() } returns mockk {
                    every { body() } answers { bodySlot.captured }
                }
            }
        }

        var outResp: okhttp3.Response? = null

        TestBounds("").also { test ->
            test.replacerData
                .getOrPut("test1", { mutableMapOf() })
                .getOrPut(Body, { mutableListOf() })
                .also {
                    it.add("b" to "_")
                }

            outResp = resp.replaceByTest(test, chap)
        }

        val outBody = outResp?.body().content()

        Assert.assertNotEquals(bodyStr, outBody)
        Assert.assertTrue(outBody.none { it == 'b' })
    }
}
