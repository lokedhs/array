package array

import kotlin.test.Test
import kotlin.test.assertFailsWith
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

    @Test
    fun discloseCreate2Dimension() {
        parseAPLExpression("⊃ (1 2 3) (4 5 6) (7 8 9)").let { result ->
            assertDimension(dimensionsOfSize(3, 3), result)
            assertArrayContent(arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9), result)
        }
    }

    @Test
    fun disclose3Dimensional() {
        parseAPLExpression("⊃ (2 2 ⍴ 1 2 3 4) (2 3 ⍴ 10 11 12 13 14 15) (2 2 ⍴ 200 201) (3 3 ⍴ 30 31 32 33 34 35 36 37 38)").let { result ->
            assertDimension(dimensionsOfSize(4, 3, 3), result)
            assertArrayContent(
                arrayOf(
                    1, 2, 0, 3, 4, 0, 0, 0, 0, 10, 11, 12, 13, 14, 15, 0, 0, 0,
                    200, 201, 0, 200, 201, 0, 0, 0, 0, 30, 31, 32,
                    33, 34, 35, 36, 37, 38), result)
        }
    }

    @Test
    fun discloseWithAxis0() {
        parseAPLExpression("⊃[0] 2 3 2 ⍴ (0 1) (2 3) (4 5) (6 7) (8 9)").let { result ->
            assertDimension(dimensionsOfSize(2, 2, 3, 2), result)
            assertArrayContent(arrayOf(0, 2, 4, 6, 8, 0, 2, 4, 6, 8, 0, 2, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3), result)
        }
    }

    @Test
    fun discloseWithAxis1() {
        parseAPLExpression("⊃[1] 2 3 2 ⍴ (0 1) (2 3) (4 5) (6 7) (8 9)").let { result ->
            assertDimension(dimensionsOfSize(2, 2, 3, 2), result)
            assertArrayContent(arrayOf(0, 2, 4, 6, 8, 0, 1, 3, 5, 7, 9, 1, 2, 4, 6, 8, 0, 2, 3, 5, 7, 9, 1, 3), result)
        }
    }

    @Test
    fun discloseWithAxis2() {
        parseAPLExpression("⊃[2] 2 3 2 ⍴ (0 1) (2 3) (4 5) (6 7) (8 9)").let { result ->
            assertDimension(dimensionsOfSize(2, 3, 2, 2), result)
            assertArrayContent(arrayOf(0, 2, 1, 3, 4, 6, 5, 7, 8, 0, 9, 1, 2, 4, 3, 5, 6, 8, 7, 9, 0, 2, 1, 3), result)
        }
    }

    @Test
    fun discloseWithAxisMultiDimensional() {
        parseAPLExpression("⊃[2] 2 3 4 ⍴ (2 2 ⍴ 0 1 10 20) (2 2 ⍴ 2 3 30 40) (2 2 ⍴ 4 5 10 20) (2 2 ⍴ 6 7 30 40) (2 2 ⍴ 8 9 50 60)").let { result ->
            assertDimension(dimensionsOfSize(2, 3, 2, 2, 4), result)
            assertArrayContent(
                arrayOf(
                    0, 2, 4, 6, 1, 3, 5, 7, 10, 30, 10, 30, 20, 40, 20, 40, 8, 0, 2, 4, 9,
                    1, 3, 5, 50, 10, 30, 10, 60, 20, 40, 20, 6, 8, 0, 2, 7, 9, 1, 3, 30,
                    50, 10, 30, 40, 60, 20, 40, 4, 6, 8, 0, 5, 7, 9, 1, 10, 30, 50, 10,
                    20, 40, 60, 20, 2, 4, 6, 8, 3, 5, 7, 9, 30, 10, 30, 50, 40, 20, 40,
                    60, 0, 2, 4, 6, 1, 3, 5, 7, 10, 30, 10, 30, 20, 40, 20, 40
                ), result)
        }
    }

    @Test
    fun discloseWithAxisMultiDimensional2() {
        parseAPLExpression("⊃[1] 2 3 4 ⍴ (2 2 ⍴ 0 1 10 20) (2 2 ⍴ 2 3 30 40) (2 2 ⍴ 4 5 10 20) (2 2 ⍴ 6 7 30 40) (2 2 ⍴ 8 9 50 60)").let { result ->
            assertDimension(dimensionsOfSize(2, 2, 2, 3, 4), result)
            assertArrayContent(
                arrayOf(
                    0, 2, 4, 6, 8, 0, 2, 4, 6, 8, 0, 2, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1,
                    3, 10, 30, 10, 30, 50, 10, 30, 10, 30, 50, 10, 30, 20, 40, 20, 40, 60,
                    20, 40, 20, 40, 60, 20, 40, 4, 6, 8, 0, 2, 4, 6, 8, 0, 2, 4, 6, 5, 7,
                    9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 10, 30, 50, 10, 30, 10, 30, 50, 10, 30,
                    10, 30, 20, 40, 60, 20, 40, 20, 40, 60, 20, 40, 20, 40
                ), result)
        }
    }

    @Test
    fun discloseWithArrayAxis1() {
        parseAPLExpression("⊃[1 2]2 3 4 ⍴ (2 2 ⍴ 0 1 101 102) (2 2 ⍴ 2 3 103 104) (2 2 ⍴ 4 5 105 106) (2 2 ⍴ 6 7 107 108) (2 2 ⍴ 8 9 109 110)").let { result ->
            assertDimension(dimensionsOfSize(2, 2, 2, 3, 4), result)
            assertArrayContent(
                arrayOf(
                    0, 2, 4, 6, 8, 0, 2, 4, 6, 8, 0, 2, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1,
                    3, 101, 103, 105, 107, 109, 101, 103, 105, 107, 109, 101, 103, 102,
                    104, 106, 108, 110, 102, 104, 106, 108, 110, 102, 104, 4, 6, 8, 0, 2,
                    4, 6, 8, 0, 2, 4, 6, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 105, 107,
                    109, 101, 103, 105, 107, 109, 101, 103, 105, 107, 106, 108, 110, 102,
                    104, 106, 108, 110, 102, 104, 106, 108
                ), result)
        }
    }

    @Test
    fun discloseWithArrayAxis2() {
        parseAPLExpression("⊃[2 1]2 3 4 ⍴ (2 2 ⍴ 0 1 101 102) (2 2 ⍴ 2 3 103 104) (2 2 ⍴ 4 5 105 106) (2 2 ⍴ 6 7 107 108) (2 2 ⍴ 8 9 109 110)").let { result ->
            assertDimension(dimensionsOfSize(2, 2, 2, 3, 4), result)
            assertArrayContent(
                arrayOf(
                    0, 2, 4, 6, 8, 0, 2, 4, 6, 8, 0, 2, 101, 103, 105, 107, 109, 101, 103,
                    105, 107, 109, 101, 103, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 102, 104,
                    106, 108, 110, 102, 104, 106, 108, 110, 102, 104, 4, 6, 8, 0, 2, 4, 6,
                    8, 0, 2, 4, 6, 105, 107, 109, 101, 103, 105, 107, 109, 101, 103, 105,
                    107, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 106, 108, 110, 102, 104, 106,
                    108, 110, 102, 104, 106, 108
                ), result)
        }
    }

    @Test
    fun discloseWithArrayAxis3() {
        parseAPLExpression("⊃[0 2]2 3 4 ⍴ (2 2 ⍴ 0 1 101 102) (2 2 ⍴ 2 3 103 104) (2 2 ⍴ 4 5 105 106) (2 2 ⍴ 6 7 107 108) (2 2 ⍴ 8 9 109 110)").let { result ->
            assertDimension(dimensionsOfSize(2, 2, 2, 3, 4), result)
            assertArrayContent(
                arrayOf(
                    0, 2, 4, 6, 8, 0, 2, 4, 6, 8, 0, 2, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1,
                    3, 4, 6, 8, 0, 2, 4, 6, 8, 0, 2, 4, 6, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3,
                    5, 7, 101, 103, 105, 107, 109, 101, 103, 105, 107, 109, 101, 103, 102,
                    104, 106, 108, 110, 102, 104, 106, 108, 110, 102, 104, 105, 107, 109,
                    101, 103, 105, 107, 109, 101, 103, 105, 107, 106, 108, 110, 102, 104,
                    106, 108, 110, 102, 104, 106, 108), result)
        }
    }

    @Test
    fun discloseWithIllegalAxis() {
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("⊃[4] 2 3 2 ⍴ (0 1) (2 3) (4 5) (6 7) (8 9)")
        }
    }

    @Test
    fun illegalAxis2() {
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("⊃[1] 1")
        }
    }


    @Test
    fun discloseScalarWithAxis() {
        assertSimpleNumber(1, parseAPLExpression("⊃[0] 1"))
    }

    @Test
    fun discloseEmpty() {
        parseAPLExpression("⊃⍬").let { result ->
            assertDimension(dimensionsOfSize(0), result)
        }
    }

    /////////////////////////////
    // Tests for pick
    /////////////////////////////

    @Test
    fun pickOneDimension() {
        assertSimpleNumber(102, parseAPLExpression("2⊃100+⍳100"))
    }

    @Test
    fun pickTwoDimensions() {
        assertSimpleNumber(167, parseAPLExpression("(⊂8 7)⊃10 20 ⍴ 100+⍳100"))
    }

    @Test
    fun pickFailOutOfRange() {
        assertFailsWith<APLIndexOutOfBoundsException> {
            parseAPLExpression("4 ⊃ 1 2 3").collapse()
        }
    }

    @Test
    fun pickFailInvalidDimensions() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("(⊂8 7) ⊃ 100+⍳100").collapse()
        }
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("7 ⊃ 3 10 ⍴ 100+⍳100").collapse()
        }
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("2⊃1").collapse()
        }
    }

    @Test
    fun pickInvalidSelectorDimension() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("0 (1 0) 0 ⊃ ((1 2) (3 4)) ((5 6) (7 8))").collapse()
        }
    }

    @Test
    fun pickInvalidSelectorType() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("0 @a 0 ⊃ ((1 2) (3 4)) ((5 6) (7 8))").collapse()
        }
    }

    @Test
    fun pickThreeDimensions() {
        assertSimpleNumber(3, parseAPLExpression("0 1 0 ⊃ ((1 2) (3 4)) ((5 6) (7 8))"))
    }

    @Test
    fun pickDepthTwo() {
        parseAPLExpression("2 1 ⊃ (10 11 12 13 14 15) (20 21 22 23 24 25) (30 31 32 33 34 35)").let { result ->
            assertSimpleNumber(31, result)
        }
    }

    @Test
    fun pickDepthTwoWithDifferentSizedArrays() {
        parseAPLExpression("2 6 ⊃ (10 11) (20 21 22 23 24 25 26 27 28 29) (30 31 32 33 34 35 36)").let { result ->
            assertSimpleNumber(36, result)
        }
    }

    @Test
    fun pickFailWhenInnerDimensionsMismatch() {
        assertFailsWith<APLIndexOutOfBoundsException> {
            parseAPLExpression("2 6 ⊃ (10 11 12 13 14 15 16 17 18 19) (20 21 22 23 24 25 26 27 28 29) (30 31 32 33) (1 2 3 4 5 6 7)").collapse()
        }
    }

    @Test
    fun pickWithExpressionAsSelector() {
        assertSimpleNumber(4, parseAPLExpression("0 (¯99+↑⍴⍳100) ({¯2+⍵} 3) ⊃ ((1 2) (3 4)) ((5 6) (7 8))"))
    }

    @Test
    fun pickScalar() {
        assertSimpleNumber(99, parseAPLExpression("⍬⊃99"))
    }
}
