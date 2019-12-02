package unitTests

import mimikMockHelpers.RecordedInteractions
import networkRouting.testingManager.observe
import networkRouting.testingManager.TestBounds
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

        val bounds = TestBounds("")
        bounds.observe(tape) {
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

        val bounds = TestBounds("")
        bounds.observe(tape) {
            tape.apply {
                obsData = chap.uses
            }
        }

        Assert.assertNotEquals(chap.mockUses, obsData)
    }
}
