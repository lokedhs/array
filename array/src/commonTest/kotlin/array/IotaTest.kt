package array

import kotlin.test.Test
import kotlin.test.assertFailsWith

class IotaTest : APLTest() {
    @Test
    fun testIotaOneArg() {
        val result = parseAPLExpression("⍳10")
        assertDimension(dimensionsOfSize(10), result)
        assertArrayContent(arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), result)
    }

    @Test
    fun testIotaSingleValue() {
        val result = parseAPLExpression("⍳1")
        assertDimension(dimensionsOfSize(1), result)
        assertArrayContent(arrayOf(0), result)
    }

    /**
     * This test verifies that the iota function can accept an expression as
     * argument and not just plain numbers.
     */
    @Test
    fun testIotaWithExpressionArg() {
        val result = parseAPLExpression("⍳1+10")
        assertDimension(dimensionsOfSize(11), result)
        assertArrayContent(arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10), result)
    }

    @Test
    fun failWithComplexArgument() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍳2J1").collapse()
        }
    }

    @Test
    fun failWithCharArgument() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍳@p").collapse()
        }
    }

    /**
     * This should work in the future, but currently it's not supported so it should fail
     */
    @Test
    fun failWithArrayArgument() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍳3 4")
        }
    }
}
