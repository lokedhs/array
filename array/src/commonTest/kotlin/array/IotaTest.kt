package array

import kotlin.test.Test
import kotlin.test.assertTrue

class IotaTest : APLTest() {
    @Test
    fun testIotaOneArg() {
        val result = parseAPLExpression("⍳10")
        assertTrue(result.dimensions().compare(dimensionsOfSize(10)))
        assertArrayContent(arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), result)
    }

    /**
     * This test verifies that the iota function can accept an expression as
     * argument and not just plain numbers.
     */
    @Test
    fun testIotaWithExpressionArg() {
        val result = parseAPLExpression("⍳1+10")
        assertTrue(result.dimensions().compare(dimensionsOfSize(11)))
        assertArrayContent(arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10), result)
    }
}
