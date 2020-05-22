package array

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class FlowControlTest : APLTest() {
    @Test
    fun assertIf() {
        assertSimpleNumber(10, parseAPLExpression("if (1) { 10 }", true))
        parseAPLExpression("if (0) { 10 }", true).let { result ->
            assertDimension(dimensionsOfSize(0), result)
        }
    }

    @Test
    fun testIfElse() {
        assertSimpleNumber(10, parseAPLExpression("if (1) { 10 } else { 20 }", true))
        assertSimpleNumber(20, parseAPLExpression("if (0) { 10 } else { 20 }", true))
    }

    @Test
    fun testIfElseInExpressionLeftSide() {
        assertSimpleNumber(1010, parseAPLExpression("1000 + if (1) { 10 } else { 20 }", true))
        assertSimpleNumber(1020, parseAPLExpression("1000 + if (0) { 10 } else { 20 }", true))
    }

    @Test
    fun testIfElseInExpressionRightSide() {
        assertSimpleNumber(1004, parseAPLExpression("if (1) { 4 } else { 5 } + 1000", true))
        assertSimpleNumber(1005, parseAPLExpression("if (0) { 4 } else { 5 } + 1000", true))
    }

    @Test
    fun testIfElseInExpressionBothSides() {
        assertSimpleNumber(11004, parseAPLExpression("10000 + if (1) { 4 } else { 5 } + 1000", true))
        assertSimpleNumber(11005, parseAPLExpression("10000 + if (0) { 4 } else { 5 } + 1000", true))
    }

    @Test
    fun testSideEffectsInIf() {
        parseAPLExpressionWithOutput("print 10 ◊ if (1) { print 2 } ◊ print 3 ◊ 100", true).let { (result, s) ->
            assertSimpleNumber(100, result)
            assertEquals("1023", s)
        }
        parseAPLExpressionWithOutput("print 10 ◊ if (0) { print 2 } ◊ print 3 ◊ 100", true).let { (result, s) ->
            assertSimpleNumber(100, result)
            assertEquals("103", s)
        }
    }

    @Test
    fun testSideEffectsInIfElse() {
        parseAPLExpressionWithOutput("print 10 ◊ if (1) { print 2 } else { print 4 } ◊ print 3 ◊ 100", true).let { (result, s) ->
            assertSimpleNumber(100, result)
            assertEquals("1023", s)
        }
        parseAPLExpressionWithOutput("print 10 ◊ if (0) { print 2 } else { print 4 } ◊ print 3 ◊ 100", true).let { (result, s) ->
            assertSimpleNumber(100, result)
            assertEquals("1043", s)
        }
    }

    @Test
    fun testMultilineIf() {
        val result = parseAPLExpression(
            """
            |if (1) {
            |    10
            |}
        """.trimMargin(), true)
        assertSimpleNumber(10, result)
    }

    @Test
    fun testMultilineIfWithElse() {
        val result0 = parseAPLExpression(
            """
            |if (1) {
            |    10
            |} else {
            |    20
            |}
        """.trimMargin(), true)
        assertSimpleNumber(10, result0)

        val result1 = parseAPLExpression(
            """
            |if (0) {
            |    10
            |} else {
            |    20
            |}
        """.trimMargin(), true)
        assertSimpleNumber(20, result1)
    }

    @Ignore
    @Test
    fun recursionTest() {
        val (result, out) = parseAPLExpressionWithOutput(
            """
            |∇ foo (x) { if (x>0) { print x ◊ foo x-1 } else { 123 } }
            |foo 10
            """.trimMargin(), true)
        assertSimpleNumber(123, result)
        assertEquals("10987654321", out)
    }

    @Test
    fun lambdaRecursionTest() {
        val (result, out) = parseAPLExpressionWithOutput(
            """
            |foo ← λ{ x←⍵ ◊ if(x>0) { print x ◊ ⍞foo x-1 } else { 123 } }
            |⍞foo 10
            """.trimMargin(), true)
        assertSimpleNumber(123, result)
        assertEquals("10987654321", out)
    }
}
