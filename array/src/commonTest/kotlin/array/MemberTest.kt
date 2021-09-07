package array

import kotlin.test.Test
import kotlin.test.assertEquals

class MemberTest : APLTest() {
    @Test
    fun oneDimension() {
        parseAPLExpression("2 11 100 10 ∊ 1 2 3 4 10 11").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1, 1, 0, 1), result)
        }
    }

    @Test
    fun twoDimension() {
        parseAPLExpression("(2 2 ⍴ 10 20 30 40) ∊ 1 2 10 100 40 200 300 400 500").let { result ->
            assertDimension(dimensionsOfSize(2, 2), result)
            assertArrayContent(arrayOf(1, 0, 0, 1), result)
        }
    }

    @Test
    fun testScalarRight() {
        parseAPLExpression("1 2 3 ∊ 2").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(0, 1, 0), result)
        }
    }

    @Test
    fun testScalarLeft() {
        parseAPLExpression("1 ∊ 1 2 3").let { result ->
            assertSimpleNumber(1, result)
        }
    }

    @Test
    fun findChars() {
        parseAPLExpression("\"foo\" ∊ \"bbxyzabcf\"").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(1, 0, 0), result)
        }
    }

    @Test
    fun findString() {
        parseAPLExpression("(⊂\"bar\") ∊ \"foo\" \"bar\" \"test\" \"longerstring\"").let { result ->
            assertSimpleNumber(1, result)
        }
    }

    @Test
    fun optimisedFind0() {
        val (result, out) = parseAPLExpressionWithOutput("2 ∊ {io:print ⍵}¨ 0 1 2 3 4")
        assertSimpleNumber(1, result)
        assertEquals("012", out)
    }

    @Test
    fun optimisedFind1() {
        val (result, out) = parseAPLExpressionWithOutput("34 35 ∊ io:print¨ 30+⍳7")
        assertDimension(dimensionsOfSize(2), result)
        assertArrayContent(arrayOf(1, 1), result)
        assertEquals("3031323334303132333435", out)
    }

    @Test
    fun optimisedFind2() {
        val (result, out) = parseAPLExpressionWithOutput("1 ∊ io:print¨ 10+⍳6")
        assertSimpleNumber(0, result)
        assertEquals("101112131415", out)
    }
}
