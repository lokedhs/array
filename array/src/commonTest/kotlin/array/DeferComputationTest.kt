package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
    fun simpleLazyEvalutionFailsWith2Arg() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression(
                """
            |∇ foo (a) {
            |  io:print a
            |  a + 5
            |}
            |((1 foo defer 10) (1 foo defer 20) (1 foo defer 30))[2]
            """.trimMargin())
        }
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

    @Test
    fun simpleLazyEvalution2Arg() {
        val (result, out) = parseAPLExpressionWithOutput(
            """
            |∇ (a) foo (b) {
            |  io:print b
            |  a + b + 5
            |}
            |((100 foo defer 10) (100 foo defer 20) (100 foo defer 30))[2]
            """.trimMargin())
        assertSimpleNumber(135, result)
        assertEquals("30", out)
    }

    @Test
    fun simpleLazyEvalution2ArgFailsWith1Arg() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression(
                """
            |∇ (a) foo (b) {
            |  io:print b
            |  a + b + 5
            |}
            |((foo defer 10) (foo defer 20) (foo defer 30))[2]
            """.trimMargin())
        }
    }

    @Test
    fun simpleEagerEvalution2Arg() {
        val (result, out) = parseAPLExpressionWithOutput(
            """
            |∇ (a) foo (b) {
            |  io:print b
            |  a + b + 5
            |}
            |((100 foo 10) (100 foo 20) (100 foo 30))[2]
            """.trimMargin())
        assertSimpleNumber(135, result)
        assertEquals("302010", out)
    }
}
