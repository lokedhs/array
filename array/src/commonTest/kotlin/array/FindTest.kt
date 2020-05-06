package array

import kotlin.test.Test

class FindTest : APLTest() {
    @Test
    fun testSimple() {
        parseAPLExpression("\"abc\" ⍷ \"fooabcaqswd\"").let { result ->
            assertDimension(dimensionsOfSize(11), result)
            assertArrayContent(arrayOf(0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0), result)
        }
    }

    @Test
    fun testMultipleHits() {
        parseAPLExpression("\"abc\" ⍷ \"fooabctestabc12345\"").let { result ->
            assertDimension(dimensionsOfSize(18), result)
            assertArrayContent(arrayOf(0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0), result)
        }

    }

    @Test
    fun multiDimensions() {
        parseAPLExpression("19 20 ⍷ 3 4 3 ⍴ 19+0 1 5 6 4 5 6").let { result ->
            assertDimension(dimensionsOfSize(3, 4, 3), result)
            assertArrayContent(
                arrayOf(
                    1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0,
                    0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0
                ), result
            )
        }
    }

    @Test
    fun testLargeDimension() {
        parseAPLExpression("(4 3 3 ⍴ 1 1 4 3 3 ↑ 1 2 1 3 1 ↓ 7 8 9 10 11 ⍴ ⍳100000) ⍷ 7 8 9 10 11 ⍴ ⍳100000").let { result ->
            assertDimension(dimensionsOfSize(7, 8, 9, 10, 11), result)
            result.iterateMembersWithPosition { v, i ->
                val expectedValue = if (i == 10044) 1L else 0L
                assertSimpleNumber(expectedValue, v, "index = ${i}")
            }
        }
    }

    @Test
    fun testScalarTargetOneDimensionalContent() {
        parseAPLExpression("3 ⍷ 1 2 3").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(0, 0, 1), result)
        }
    }

    @Test
    fun testScalar() {
        assertSimpleNumber(1, parseAPLExpression("3 ⍷ 3"))
        assertSimpleNumber(0, parseAPLExpression("3 ⍷ 5"))
        assertSimpleNumber(0, parseAPLExpression("3 ⍷ @a"))
    }

    @Test
    fun testNullContent() {
        parseAPLExpression("3 ⍷ ⍬").let { result ->
            assertDimension(dimensionsOfSize(0), result)
        }
    }

    @Test
    fun testTargetHigherRank() {
        parseAPLExpression("(2 3 4 ⍴ ⍳100) ⍷ ⍳3").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(0, 0, 0), result)
        }
    }

    @Test
    fun testLargerTarget() {
        parseAPLExpression("(4 4 4 ⍴ ⍳100) ⍷ 2 2 2 ⍴ ⍳100").let { result ->
            assertDimension(dimensionsOfSize(2, 2, 2), result)
            assertArrayContent(arrayOf(0, 0, 0, 0, 0, 0, 0, 0), result)
        }
    }

    @Test
    fun testTargetSameSize() {
        parseAPLExpression("100 101 102 ⍷ 100 101 102").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(1, 0, 0), result)
        }
    }

    @Test
    fun testNullTarget() {
        assertSimpleNumber(0, parseAPLExpression("⍬ ⍷ 11"))
    }

    @Test
    fun singleDimensionTargetLarger() {
        parseAPLExpression("1 2 3 4 ⍷ 1 2 3").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(0, 0, 0), result)
        }
    }
}
