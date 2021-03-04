package array

import kotlin.test.Test
import kotlin.test.assertFailsWith

class ComposeTest : APLTest() {
    @Test
    fun compose2Arg() {
        parseAPLExpression("¯2 3 4 (×∘-) 1000").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(2000, -3000, -4000), result)
        }
    }

    @Test
    fun compose1Arg() {
        parseAPLExpression("(×∘÷) ¯1 2 3").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(-1, 1, 1), result)
        }
    }


    @Test
    fun mismatchedArgumentCount() {
        assertFailsWith<APLEvalException> {
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
}
