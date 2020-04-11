package array

import kotlin.test.Test

class OperatorsTest : APLTest() {
    @Test
    fun commuteTwoArgs() {
        val result = parseAPLExpression("4÷⍨160")
        assertSimpleNumber(40, result)
    }

    @Test
    fun commuteOneArg() {
        val result = parseAPLExpression("+⍨3")
        assertSimpleNumber(6, result)
    }

    @Test
    fun commuteWithArrays() {
        val result = parseAPLExpression("2÷⍨8×⍳10")
        assertDimension(dimensionsOfSize(10), result)
        assertArrayContent(arrayOf(0, 4, 8, 12, 16, 20, 24, 28, 32, 36), result)
    }

    @Test
    fun multiOperators() {
        val result = parseAPLExpression("+⌺⍨1 2 3")
        assertDimension(dimensionsOfSize(3, 3), result)
        assertArrayContent(arrayOf(2, 3, 4, 3, 4, 5, 4, 5, 6), result)
    }
}
