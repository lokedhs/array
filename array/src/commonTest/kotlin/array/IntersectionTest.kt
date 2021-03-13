package array

import kotlin.test.Test
import kotlin.test.assertFailsWith

class IntersectionTest : APLTest() {
    @Test
    fun simpleIntersection() {
        parseAPLExpression("1 2 3 4 5 ∩ 1 2 3 10 11 12").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(1, 2, 3), result)
        }
    }

    @Test
    fun scalarLeftArg() {
        parseAPLExpression("1 ∩ 1 2 3").let { result ->
            assertDimension(dimensionsOfSize(1), result)
            assertArrayContent(arrayOf(1), result)
        }
    }

    @Test
    fun singleValueIntersect() {
        parseAPLExpression("1 2 3 ∩ 2 10 11").let { result ->
            assertDimension(dimensionsOfSize(1), result)
            assertArrayContent(arrayOf(2), result)
        }
    }

    @Test
    fun scalarLeftNoMatch() {
        parseAPLExpression("1 ∩ 10 11 12").let { result ->
            assertAPLNull(result)
        }
    }

    @Test
    fun scalarRightArg() {
        parseAPLExpression("1 22 3 ∩ 22").let { result ->
            assertDimension(dimensionsOfSize(1), result)
            assertArrayContent(arrayOf(22), result)
        }
    }

    @Test
    fun scalarRightNoMatch() {
        parseAPLExpression("1 22 3 ∩ 33").let { result ->
            assertAPLNull(result)
        }
    }

    @Test
    fun intersectionNoMatching() {
        parseAPLExpression("1 2 3 ∩ 10 11 12 13 14 15 16").let { result ->
            assertAPLNull(result)
        }
    }

    @Test
    fun intersectAllMatch() {
        parseAPLExpression("1 2 3 ∩ 1 2 3").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(1, 2, 3), result)
        }
    }

    @Test
    fun intersectAllMatchDifferentOrder() {
        parseAPLExpression("1 2 3 4 ∩ 3 1 2 4").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1, 2, 3, 4), result)
        }
    }

    @Test
    fun intersectAllMatchWithDuplicates() {
        parseAPLExpression("1 1 2 2 3 4 1 ∩ 4 4 3 3 2 1 1 2 2 1 3 4").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1, 2, 3, 4), result)
        }
    }

    @Test
    fun nullLeftArg() {
        parseAPLExpression("⍬ ∩ ⍳10").let { result ->
            assertAPLNull(result)
        }
    }

    @Test
    fun nullRightArg() {
        parseAPLExpression("(⍳10) ∩ ⍬").let { result ->
            assertAPLNull(result)
        }
    }

    @Test
    fun nullBothArgs() {
        parseAPLExpression("⍬ ∩ ⍬").let { result ->
            assertAPLNull(result)
        }
    }

    @Test
    fun invalidDimensionLeftArg() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("(2 2⍴⍳4) ∩ 1 2")
        }
    }

    @Test
    fun invalidDimensionRightArg() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("1 2 ∩ (2 2⍴⍳4)")
        }
    }

    @Test
    fun invalidDimensionBothArgs() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("(2 2⍴⍳4)∩(2 2⍴⍳4)")
        }
    }

    @Test
    fun intersectionWithStrings0() {
        parseAPLExpression("\"abcd\" ∩ \"xycb\"").let { result ->
            assertString("bc", result)
        }
    }

    @Test
    fun intersectionWithStrings1() {
        parseAPLExpression("\"abx\" ∩ \"xyz\"").let { result ->
            assertString("x", result)
        }
    }

    @Test
    fun intersectionWithStrings2() {
        parseAPLExpression("\"abc\" ∩ \"xyz\"").let { result ->
            assertAPLNull(result)
        }
    }

    @Test
    fun intersectionWithStrings3() {
        parseAPLExpression("\"abc\" ∩ \"abc\"").let { result ->
            assertString("abc", result)
        }
    }
}
