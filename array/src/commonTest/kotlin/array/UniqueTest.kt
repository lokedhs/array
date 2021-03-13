package array

import kotlin.test.Test
import kotlin.test.assertFailsWith

class UniqueTest : APLTest() {
    @Test
    fun simpleTest() {
        parseAPLExpression("∪ 0 3 1 1 0 0").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(0, 3, 1), result)
        }
    }

    @Test
    fun allDistinct() {
        parseAPLExpression("∪ 0 3 1 2").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(0, 3, 1, 2), result)
        }
    }

    @Test
    fun scalarValue() {
        parseAPLExpression("∪ 1").let { result ->
            assertDimension(dimensionsOfSize(1), result)
            assertArrayContent(arrayOf(1), result)
        }
    }

    @Test
    fun errorWhenMultiDimensional() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("∪ 2 2 ⍴ ⍳100")
        }
    }

    @Test
    fun emptyArray() {
        parseAPLExpression("∪⍬").let { result ->
            assertAPLNull(result)
        }
    }

    //////////////////////////////////////////////////////////////////
    // Union
    //////////////////////////////////////////////////////////////////

    @Test
    fun simpleUnion() {
        parseAPLExpression("1 2 3 ∪ 9 8 1").let { result ->
            assertDimension(dimensionsOfSize(5), result)
            assertArrayContent(arrayOf(1, 2, 3, 9, 8), result)
        }
    }

    @Test
    fun sameArgs() {
        parseAPLExpression("1 2 3 ∪ 1 2 3").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(1, 2, 3), result)
        }
    }

    @Test
    fun sameArgsDifferentOrder() {
        parseAPLExpression("1 2 3 ∪ 2 1 3").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(1, 2, 3), result)
        }
    }

    @Test
    fun leftSideScalar() {
        parseAPLExpression("1 ∪ 1 2 3").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(1, 2, 3), result)
        }
    }

    @Test
    fun rightSideScalar() {
        parseAPLExpression("3 ∪ 1 2 3").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(3, 1, 2), result)
        }
    }

    @Test
    fun unionScalar0() {
        parseAPLExpression("1 ∪ 1").let { result ->
            assertArrayContent(arrayOf(1), result)
        }
    }

    @Test
    fun unionScalar2() {
        parseAPLExpression("1 ∪ 2").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(1, 2), result)
        }
    }

    @Test
    fun unionNoSimilar() {
        parseAPLExpression("1 2 3 ∪ 9 8 7").let { result ->
            assertDimension(dimensionsOfSize(6), result)
            assertArrayContent(arrayOf(1, 2, 3, 9, 8, 7), result)
        }
    }

    @Test
    fun leftSideNull() {
        parseAPLExpression("⍬ ∪ 1 2 3").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(1, 2, 3), result)
        }
    }

    @Test
    fun rightSideNull() {
        parseAPLExpression("1 2 3 ∪ ⍬").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(1, 2, 3), result)
        }
    }

    @Test
    fun unionWithSimilarLeft() {
        parseAPLExpression("1 2 2 ∪ 4 5 6").let { result ->
            assertDimension(dimensionsOfSize(5), result)
            assertArrayContent(arrayOf(1, 2, 4, 5, 6), result)
        }
    }

    @Test
    fun unionWithSimilarRight() {
        parseAPLExpression("1 2 3 ∪ 4 5 5").let { result ->
            assertDimension(dimensionsOfSize(5), result)
            assertArrayContent(arrayOf(1, 2, 3, 4, 5), result)
        }
    }

    @Test
    fun unionWithSimilarBothSides() {
        parseAPLExpression("1 1 1 ∪ 2 1 1 4 3 5 6").let { result ->
            assertDimension(dimensionsOfSize(6), result)
            assertArrayContent(arrayOf(1, 2, 4, 3, 5, 6), result)
        }
    }

    @Test
    fun unionWithStringsLeftSideDuplicated() {
        parseAPLExpression("\"foo\" ∪ \"abc\"").let { result ->
            assertString("foabc", result)
        }
    }

    @Test
    fun unionWithStringsBothSidesDifferent() {
        parseAPLExpression("\"abc\" ∪ \"xyz\"").let { result ->
            assertString("abcxyz", result)
        }
    }

    @Test
    fun unionWithStringsSomeCharsShared() {
        parseAPLExpression("\"abcdef\" ∪ \"xyzagb\"").let { result ->
            assertString("abcdefxyzg", result)
        }
    }

    @Test
    fun unionWithStringsAllCharsShared0() {
        parseAPLExpression("\"xyz\" ∪ \"xyz\"").let { result ->
            assertString("xyz", result)
        }
    }

    @Test
    fun unionWithStringsAllCharsShared1() {
        parseAPLExpression("\"foo\" ∪ \"foo\"").let { result ->
            assertString("fo", result)
        }
    }
}
