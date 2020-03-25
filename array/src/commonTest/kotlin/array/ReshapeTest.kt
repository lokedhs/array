package array

import kotlin.test.Test

class ReshapeTest : APLTest() {
    @Test
    fun simpleReshape() {
        val result = parseAPLExpression("3 4 ⍴ ⍳12")
        assertDimension(dimensionsOfSize(3, 4), result)
        assertArrayContent(arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), result)
    }

    @Test
    fun reshapeDecreaseSize() {
        val result = parseAPLExpression("3 ⍴ 1 2 3 4 5 6 7 8")
        assertDimension(dimensionsOfSize(3), result)
        assertArrayContent(arrayOf(1, 2, 3), result)
    }

    @Test
    fun reshapeIncreaseSize() {
        val result = parseAPLExpression("14 ⍴ 1 2 3 4")
        assertDimension(dimensionsOfSize(14), result)
        assertArrayContent(arrayOf(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2), result)
    }

    @Test
    fun reshapeScalarToSingleDimension() {
        val result = parseAPLExpression("4 ⍴ 1")
        assertDimension(dimensionsOfSize(4), result)
        assertArrayContent(arrayOf(1, 1, 1, 1), result)
    }

    @Test
    fun reshapeScalarToMultiDimension() {
        val result = parseAPLExpression("2 4 ⍴ 1")
        assertDimension(dimensionsOfSize(2, 4), result)
        assertArrayContent(arrayOf(1, 1, 1, 1, 1, 1, 1, 1), result)
    }
}
