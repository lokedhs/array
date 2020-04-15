package array

import kotlin.test.Test
import kotlin.test.assertTrue

class ReduceTest : APLTest() {
    @Test
    fun reduceIotaTest() {
        val result = parseAPLExpression("+/⍳1000")
        assertSimpleNumber(499500, result)
    }

    @Test
    fun reduceTestWithFunction() {
        val result = parseAPLExpression("+/⍳1+2")
        assertSimpleNumber(3, result)
    }

    @Test
    fun reduceWithSingleValue() {
        val result = parseAPLExpression("+/,4")
        assertSimpleNumber(4, result)
    }

    @Test
    fun reduceWithScalar() {
        val result = parseAPLExpression("+/4")
        assertSimpleNumber(4, result)
    }

    @Test
    fun reduceWithEmptyArg() {
        reduceTestWithFunctionName("+", 0)
        reduceTestWithFunctionName("-", 0)
        reduceTestWithFunctionName("×", 1)
        reduceTestWithFunctionName("÷", 1)
        reduceTestWithFunctionName("⋆", 1)
        reduceTestWithFunctionName("=", 1)
        reduceTestWithFunctionName("≠", 0)
        reduceTestWithFunctionName("<", 0)
        reduceTestWithFunctionName(">", 0)
        reduceTestWithFunctionName("≤", 1)
        reduceTestWithFunctionName("≥", 1)
    }

    @Test
    fun reduceWithNonScalarCells() {
        val result = parseAPLExpression("+/ (1 2 3 4) (6 7 8 9)")
        assertDimension(emptyDimensions(), result)

        val v = result.valueAt(0)
        assertDimension(dimensionsOfSize(4), v)
        assertArrayContent(arrayOf(7, 9, 11, 13), v)
    }

    @Test
    fun reduceCustomFn() {
        val result = parseAPLExpression("{⍺+⍵+10}/⍳6")
        assertSimpleNumber(65, result)
    }

    private fun reduceTestWithFunctionName(aplFn: String, correctRes: Int) {
        val result = parseAPLExpression("${aplFn}/0⍴4")
        assertTrue(result.dimensions.compare(emptyDimensions()))
        assertSimpleNumber(correctRes.toLong(), result.valueAt(0))
    }
}
