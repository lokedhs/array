package array

import kotlin.test.Test

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
}
