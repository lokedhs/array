package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TakeTest : APLTest() {
    @Test
    fun testDropSimple() {
        val result = parseAPLExpression("↓1 2 3 4")
        assertDimension(dimensionsOfSize(3), result)
        assertArrayContent(arrayOf(2, 3, 4), result)
    }

    @Test
    fun testDropFunctionResult() {
        val result = parseAPLExpression("↓10 + 1 2 3 4")
        assertDimension(dimensionsOfSize(3), result)
        assertArrayContent(arrayOf(12, 13, 14), result)
    }

    @Test
    fun testDropFromArray1() {
        val result = parseAPLExpression("↓,1")
        assertDimension(dimensionsOfSize(0), result)
        assertEquals(0, result.size)
    }

    @Test
    fun testTakeSimple() {
        val result = parseAPLExpression("↑1 2 3 4")
        assertTrue(result.isScalar())
        assertEquals(1L, result.ensureNumber().asLong())
    }

    @Test
    fun testTakeFromArray1() {
        val result = parseAPLExpression("↑,1")
        assertTrue(result.isScalar())
        assertEquals(1L, result.ensureNumber().asLong())
    }
}
