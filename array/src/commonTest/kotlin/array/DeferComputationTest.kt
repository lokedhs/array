package array

import kotlin.test.Test
import kotlin.test.assertEquals

class DeferComputationTest : APLTest() {
    @Test
    fun simpleLazyEvalution() {
        val (result, out) = parseAPLExpressionWithOutput(
            """
            |∇ foo (a) {
            |  io:print a
            |  a + 5
            |}
            |((foo defer 10) (foo defer 20) (foo defer 30))[2]
            """.trimMargin())
        assertSimpleNumber(35, result)
        assertEquals("30", out)
    }

    @Test
    fun simpleEagerEvalution() {
        val (result, out) = parseAPLExpressionWithOutput(
            """
            |∇ foo (a) {
            |  io:print a
            |  a + 5
            |}
            |((foo 10) (foo 20) (foo 30))[2]
            """.trimMargin())
        assertSimpleNumber(35, result)
        assertEquals("302010", out)
    }
}
