package array

import kotlin.test.Test
import kotlin.test.assertFailsWith

class ConcatenateTest : APLTest() {
    @Test
    fun simpleConcatenate() {
        parseAPLExpression("1 2 3 , 4 5").let { result ->
            assertDimension(dimensionsOfSize(5), result)
            assertArrayContent(arrayOf(1, 2, 3, 4, 5), result)
        }
    }

    @Test
    fun scalarWithVector() {
        parseAPLExpression("1 , 2 3 4 5").let { result ->
            assertDimension(dimensionsOfSize(5), result)
            assertArrayContent(arrayOf(1, 2, 3, 4, 5), result)
        }
    }

    @Test
    fun vectorWithScalar() {
        parseAPLExpression("1 2 3 4 , 5").let { result ->
            assertDimension(dimensionsOfSize(5), result)
            assertArrayContent(arrayOf(1, 2, 3, 4, 5), result)
        }
    }

    @Test
    fun scalarWithScalar() {
        parseAPLExpression("1 , 2").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(1, 2), result)
        }
    }

    @Test
    fun twoDimensionalConcat() {
        parseAPLExpression("(4 5 ⍴ ⍳20) , 1000+⍳4").let { result ->
            assertDimension(dimensionsOfSize(4, 6), result)
            assertArrayContent(
                arrayOf(
                    0, 1, 2, 3, 4, 1000, 5, 6, 7, 8, 9, 1001, 10, 11, 12, 13, 14, 1002,
                    15, 16, 17, 18, 19, 1003
                ), result)
        }
    }

    @Test
    fun twoDimensionalConcatWithExplicitAxis() {
        parseAPLExpression("(4 5 ⍴ ⍳20) ,[1] 1000+⍳4").let { result ->
            assertDimension(dimensionsOfSize(4, 6), result)
            assertArrayContent(
                arrayOf(
                    0, 1, 2, 3, 4, 1000, 5, 6, 7, 8, 9, 1001, 10, 11, 12, 13, 14, 1002,
                    15, 16, 17, 18, 19, 1003
                ), result)
        }
    }

    @Test
    fun twoDimensionalFirstAxis() {
        parseAPLExpression("(4 5 ⍴ ⍳20) ,[0] 1000+⍳5").let { result ->
            assertDimension(dimensionsOfSize(5, 5), result)
            assertArrayContent(
                arrayOf(
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
                    1000, 1001, 1002, 1003, 1004
                ), result)
        }
    }

    @Test
    fun mismatchedDimensions() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("(3 4 ⍴ ⍳12) , 1 2")
        }
    }

    @Test
    fun concatenateScalar() {
        parseAPLExpression("(5 6 ⍴ ⍳30) , 1234").let { result ->
            assertDimension(dimensionsOfSize(5, 7), result)
            assertArrayContent(
                arrayOf(
                    0, 1, 2, 3, 4, 5, 1234, 6, 7, 8, 9, 10, 11, 1234, 12, 13, 14, 15, 16,
                    17, 1234, 18, 19, 20, 21, 22, 23, 1234, 24, 25, 26, 27, 28, 29, 1234
                ), result)
        }
    }
}
