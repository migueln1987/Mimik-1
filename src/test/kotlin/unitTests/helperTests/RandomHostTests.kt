package unitTests.helperTests

import helpers.RandomHost
import io.ktor.html.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class RandomHostTests {
    lateinit var host: RandomHost

    @Before
    fun setup() {
        host = RandomHost()
    }

    @Test
    fun persistValue() {
        val first = host.value
        Assert.assertEquals(first, host.value)

        val second = host.value
        Assert.assertEquals(second, host.value)
    }

    @Test
    fun zeroBoundValue() {
        val test = host.nextRandom(0)
        Assert.assertTrue(test > 0)
    }

    @Test
    fun newValue() {
        val first = host.value
        val second = host.nextRandom()

        Assert.assertNotEquals(first, second)
        Assert.assertEquals(second, host.value)
    }

    @Test
    fun stringValue() {
        val test = host.valueAsChars()
        Assert.assertTrue(test.length in (5..10))
    }

    @Test
    fun valueToValidTests() {
        val checkListSource = listOf(
            ('a'..'c').toList(),
            ('1'..'5').toList()
        )
        val result = host.valueToValid { checkList ->
            checkListSource.forEach { checkList.add(it to 2) }
        }

        val resultItems = result.chunked(2)
        Assert.assertEquals(2, resultItems.size)

        checkListSource.forEachIndexed { index, list ->
            resultItems[index].forEach {
                Assert.assertTrue(it in list)
            }
        }
    }
}
