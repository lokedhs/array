package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FindIndexTest : APLTest() {
    @Test
    fun singleIndex() {
        parseAPLExpression("30 ⍳ 10 20 30 40").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1, 1, 0, 1), result)
        }
    }

    @Test
    fun multiIndexes() {
        parseAPLExpression("10 60 ⍳ 10 20 30 40 50 60").let { result ->
            assertDimension(dimensionsOfSize(6), result)
            assertArrayContent(arrayOf(0, 2, 2, 2, 2, 1), result)
        }
    }

    @Test
    fun indexTestPlain() {
        assertSimpleNumber(1, parseAPLExpression("10 20 30 40 50 ⍳ 20"))
    }

    @Test
    fun indexTestMultiArg() {
        parseAPLExpression("10 20 30 40 50 60 70 80 90 ⍳ 10 50").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(0, 4), result)
        }
    }

    @Test
    fun indexNoIndexFound() {
        assertSimpleNumber(4, parseAPLExpression("10 20 30 40 ⍳ 1"))
    }

    @Test
    fun indexNotFoundMultiArg() {
        parseAPLExpression("10 20 30 40 ⍳ 1 2").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(4, 4), result)
        }
    }

    @Test
    fun indexWithMultiDimensionalLeftArg() {
        parseAPLExpression("9 6 1 2 0 ⍳ (2 2 ⍴ ⍳4)").let { result ->
            assertDimension(dimensionsOfSize(2, 2), result)
            assertArrayContent(arrayOf(4, 2, 3, 5), result)
        }
    }

    @Test
    fun indexWithOptimisation0() {
        val (result, out) = parseAPLExpressionWithOutput(
            """
            |a ⇐ { io:print ⍵ }
            |(a¨ 10 11 12 13 14 15 16) ⍳ 12
            """.trimMargin())
        assertSimpleNumber(2, result)
        assertEquals("101112", out)
    }

    @Test
    fun indexWithOptimisation1() {
        val (result, out) = parseAPLExpressionWithOutput(
            """
            |a ⇐ { io:print ⍵ }
            |(a¨ 10 11 12 13 14 15 16) ⍳ 11 12
            """.trimMargin())
        assertDimension(dimensionsOfSize(2), result)
        assertArrayContent(arrayOf(1, 2), result)
        assertEquals("1011101112", out)
    }
}
