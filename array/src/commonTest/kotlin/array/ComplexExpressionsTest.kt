package array

import kotlin.test.Test

class ComplexExpressionsTest : APLTest() {
    @Test
    fun parenExpressionWithScalarValue() {
        val result = parseAPLExpression("(1+2)")
        assertSimpleNumber(3, result)
    }

    @Test
    fun nestedArrayNoExpression() {
        val result = parseAPLExpression("(1 2) (3 4)")
        assertDimension(dimensionsOfSize(2), result)
        assertArrayContent(arrayOf(1, 2), result.valueAt(0))
        assertArrayContent(arrayOf(3, 4), result.valueAt(1))
    }

    @Test
    fun nestedArrayScalarValue() {
        val result = parseAPLExpression("(1) (2 3)")
        assertDimension(dimensionsOfSize(2), result)
        assertSimpleNumber(1, result.valueAt(0))
        assertArrayContent(arrayOf(2, 3), result.valueAt(1))
    }

    @Test
    fun nestedArrayWithScalarValueFromFn() {
        val result = parseAPLExpression("∇ foo x {1+x} ◊ (foo 1) (foo 6)")
        assertDimension(dimensionsOfSize(2), result)
        assertSimpleNumber(2, result.valueAt(0))
        assertSimpleNumber(7, result.valueAt(1))
    }

    @Test
    fun doubleNestedArrays() {
        val result = parseAPLExpression("(⍳3) (10+⍳10)")
        assertDimension(dimensionsOfSize(2), result)
        result.valueAt(0).let { value ->
            assertDimension(dimensionsOfSize(3), value)
            assertArrayContent(arrayOf(0, 1, 2), value)
        }
        result.valueAt(1).let { value ->
            assertDimension(dimensionsOfSize(10), value)
            assertArrayContent(arrayOf(10, 11, 12, 13, 14, 15, 16, 17, 18, 19), value)
        }
    }
}
