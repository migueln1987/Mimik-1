package unitTests.helperTests

import helpers.RandomHost
import helpers.RegBuilder
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
    fun newValue() {
        val first = host.value
        val second = host.nextInt()

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

    @Test
    fun rangeConvertTester() {
        val builder = RegBuilder()
        val input = "01236789<=>ABCDEFGHJKLMOPQRSTUVXYZabcdeghijklmnopqrsuyz".toMutableList()
        val expected = "0-36-9<=>A-HJ-MO-VXYZa-eg-suyz"
        val output = builder.rangeConverter(input)
        Assert.assertEquals(expected, output)
    }
}
