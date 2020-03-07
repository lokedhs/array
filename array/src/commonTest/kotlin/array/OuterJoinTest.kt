package array

import kotlin.test.Test
import kotlin.test.assertTrue

class OuterJoinTest : APLTest() {
    @Test
    fun test1x1() {
        val result = parseAPLExpression("1 2 ×⌺ 1 2 3 4")
        assertTrue(result.dimensions().compare(dimensionsOfSize(2, 4)))
        assertArrayContent(arrayOf(1, 2, 3, 4, 2, 4, 6, 8), result)
    }

    @Test
    fun testArrayResult() {
        val result = parseAPLExpression("1 2 {⍺,⍵}⌺ 9 8 7 6")
        assertTrue(result.dimensions().compare(dimensionsOfSize(2, 4)))
        assertPairs(
            result,
            arrayOf(1, 9),
            arrayOf(1, 8),
            arrayOf(1, 7),
            arrayOf(1, 6),
            arrayOf(2, 9),
            arrayOf(2, 8),
            arrayOf(2, 7),
            arrayOf(2, 6)
        )
    }
}
