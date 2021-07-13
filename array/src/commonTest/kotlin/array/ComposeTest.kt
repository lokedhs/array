package array

import kotlin.test.Test
import kotlin.test.assertFailsWith

class ComposeTest : APLTest() {
    @Test
    fun compose2Arg0() {
        parseAPLExpression("¯2 3 4 (×∘-) 1000").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(2000, -3000, -4000), result)
        }
    }

    @Test
    fun compose2Arg1() {
        parseAPLExpression("¯2 3 4 ×∘- 1000").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(2000, -3000, -4000), result)
        }
    }

    @Test
    fun compose1Arg0() {
        parseAPLExpression("(×∘÷) ¯1 2 3").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(-1, 1, 1), result)
        }
    }

    @Test
    fun compose1Arg1() {
        parseAPLExpression("×∘÷ ¯1 2 3").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(-1, 1, 1), result)
        }
    }

    @Test
    fun mismatchedArgumentCount0() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("¯2 3 4 (×∘-) 1000 11")
        }
    }

    @Test
    fun integerOptimisedArrays() {
        parseAPLExpression("(internal:ensureLong 301 ¯302 303 ¯304 305) (+∘×) (internal:ensureLong ¯10 ¯11 12 13 14)").let { result ->
            assertDimension(dimensionsOfSize(5), result)
            assertArrayContent(arrayOf(300, -303, 304, -303, 306), result)
        }
    }

    @Test
    fun doubleOptimisedArrays() {
        parseAPLExpression("(internal:ensureDouble 301 ¯302 303 ¯304 305) (+∘×) (internal:ensureDouble ¯10 ¯11 12 13 14)").let { result ->
            assertDimension(dimensionsOfSize(5), result)
            assertArrayContent(arrayOf(300.0, -303.0, 304.0, -303.0, 306.0), result)
        }
    }

    @Test
    fun composeWithCustomFunction0() {
        val result = parseAPLExpression(
            """
            |∇ a (x) { x+100 }
            |∇ (x) b (y) { y+1000+x }
            |1000 (b∘a) 2000
            """.trimMargin())
        assertSimpleNumber(4100, result)
    }

    @Test
    fun composeWithCustomFunction1() {
        val result = parseAPLExpression(
            """
            |∇ a (x) { x+100 }
            |∇ (x) b (y) { y+1000+x }
            |1000 b∘a 2000
            """.trimMargin())
        assertSimpleNumber(4100, result)
    }

    @Test
    fun composeWithCustomFunction2() {
        val result = parseAPLExpression(
            """
            |c ⇐ {⍵+2}
            |d ⇐ {⍵+3+⍺}
            |5 (d∘c) 6
            """.trimMargin())
        assertSimpleNumber(16, result)
    }

    @Test
    fun composeWithCustomFunction3() {
        val result = parseAPLExpression(
            """
            |c ⇐ {⍵+2}
            |d ⇐ {⍵+3+⍺}
            |5 d∘c 6
            """.trimMargin())
        assertSimpleNumber(16, result)
    }

    @Test
    fun simpleFork0() {
        parseAPLExpression("1 (⊢⊣,) 2").let { result ->
            assertSimpleNumber(2, result)
        }
    }

    @Test
    fun simpleFork1() {
        parseAPLExpression("1 (⊣⊢,) 2").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(1, 2), result)
        }
    }

    @Test
    fun simple2Train0() {
        parseAPLExpression("10 (-,) 20").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(-10, -20), result)
        }
    }

    @Test
    fun simple2Train1() {
        parseAPLExpression("2 (-*) 5").let { result ->
            assertSimpleNumber(-32, result)
        }
    }
}
