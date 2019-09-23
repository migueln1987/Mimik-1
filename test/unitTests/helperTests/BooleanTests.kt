package unitTests.helperTests

import helpers.allTrue
import helpers.anyTrue
import helpers.isFalse
import helpers.isTrue
import org.junit.Assert
import org.junit.Test

class BooleanTests {
    @Test
    fun nullableIsTrue() {
        val testTrue: Boolean? = true
        Assert.assertTrue(testTrue.isTrue())
    }

    @Test
    fun nullableNulltIsTrue() {
        val testTrue: Boolean? = null
        Assert.assertFalse(testTrue.isTrue())
    }

    @Test
    fun nullableIsFalse() {
        val testFalse: Boolean? = true
        Assert.assertTrue(testFalse.isFalse())
    }

    @Test
    fun nullableNulltIsFalse() {
        val testFalse: Boolean? = null
        Assert.assertFalse(testFalse.isFalse())
    }

    @Test
    fun anyTrueTest_One() {
        val testValues = booleanArrayOf(true, false)
        val result = anyTrue(*testValues)
        Assert.assertTrue(result)
    }

    @Test
    fun anyTrueTest_None() {
        val testValues = booleanArrayOf(false, false)
        val result = anyTrue(*testValues)
        Assert.assertTrue(result)
    }

    @Test
    fun allTrueTest_All() {
        val testValues = booleanArrayOf(true, true)
        val result = allTrue(*testValues)
        Assert.assertTrue(result)
    }

    @Test
    fun allTrueTest_None() {
        val testValues = booleanArrayOf(false, false)
        val result = allTrue(*testValues)
        Assert.assertTrue(result)
    }
}
