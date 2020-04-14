package array

import kotlin.test.Test
import kotlin.test.assertFalse

class DiscloseTest : APLTest() {
    @Test
    fun discloseArrayTest() {
        val result = parseAPLExpression("⊃1 2 3 4")
        assertFalse(result.isScalar())
        assertDimension(dimensionsOfSize(4), result)
        assertArrayContent(arrayOf(1, 2, 3, 4), result)
    }

    @Test
    fun discloseNumberTest() {
        val result = parseAPLExpression("⊃6")
        assertSimpleNumber(6, result)
    }
}
