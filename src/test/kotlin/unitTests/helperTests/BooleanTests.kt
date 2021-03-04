package unitTests.helperTests

import kotlinx.allTrue
import kotlinx.anyTrue
import kotlinx.isFalse
import kotlinx.isTrue
import org.junit.Assert
import org.junit.Test

class BooleanTests {
    @Suppress("RedundantNullableReturnType")
    @Test
    fun nullableIsTrue() {
        val testTrue: Boolean? = true
        Assert.assertTrue(testTrue.isTrue)
    }

    @Test
    fun nullableNulltIsTrue() {
        val testTrue: Boolean? = null
        Assert.assertFalse(testTrue.isTrue)
    }

    @Suppress("RedundantNullableReturnType")
    @Test
    fun nullableIsFalse() {
        val testFalse: Boolean? = true
        Assert.assertFalse(testFalse.isFalse)
    }

    @Test
    fun nullableNulltIsFalse() {
        val testFalse: Boolean? = null
        Assert.assertFalse(testFalse.isFalse)
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
        Assert.assertFalse(result)
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
        Assert.assertFalse(result)
    }
}
