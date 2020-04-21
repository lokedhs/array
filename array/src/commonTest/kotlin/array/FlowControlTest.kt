package array

import kotlin.test.Test
import kotlin.test.assertEquals

class FlowControlTest : APLTest() {
    @Test
    fun assertIf() {
        assertSimpleNumber(10, parseAPLExpression("if (1) { 10 }"))
        parseAPLExpression("if (0) { 10 }").let { result ->
            assertDimension(dimensionsOfSize(0), result)
        }
    }

    @Test
    fun testIfElse() {
        assertSimpleNumber(10, parseAPLExpression("if (1) { 10 } else { 20 }"))
        assertSimpleNumber(20, parseAPLExpression("if (0) { 10 } else { 20 }"))
    }

    @Test
    fun testIfElseInExpressionLeftSide() {
        assertSimpleNumber(1010, parseAPLExpression("1000 + if (1) { 10 } else { 20 }"))
        assertSimpleNumber(1020, parseAPLExpression("1000 + if (0) { 10 } else { 20 }"))
    }

    @Test
    fun testIfElseInExpressionRightSide() {
        assertSimpleNumber(1004, parseAPLExpression("if (1) { 4 } else { 5 } + 1000"))
        assertSimpleNumber(1005, parseAPLExpression("if (0) { 4 } else { 5 } + 1000"))
    }

    @Test
    fun testIfElseInExpressionBothSides() {
        assertSimpleNumber(11004, parseAPLExpression("10000 + if (1) { 4 } else { 5 } + 1000"))
        assertSimpleNumber(11005, parseAPLExpression("10000 + if (0) { 4 } else { 5 } + 1000"))
    }

    @Test
    fun testSideEffectsInIf() {
        parseAPLExpressionWithOutput("print 10 ◊ if (1) { print 2 } ◊ print 3 ◊ 100").let { (result, s) ->
            assertSimpleNumber(100, result)
            assertEquals("1023", s)
        }
        parseAPLExpressionWithOutput("print 10 ◊ if (0) { print 2 } ◊ print 3 ◊ 100").let { (result, s) ->
            assertSimpleNumber(100, result)
            assertEquals("103", s)
        }
    }

    @Test
    fun testSideEffectsInIfElse() {
        parseAPLExpressionWithOutput("print 10 ◊ if (1) { print 2 } else { print 4 } ◊ print 3 ◊ 100").let { (result, s) ->
            assertSimpleNumber(100, result)
            assertEquals("1023", s)
        }
        parseAPLExpressionWithOutput("print 10 ◊ if (0) { print 2 } else { print 4 } ◊ print 3 ◊ 100").let { (result, s) ->
            assertSimpleNumber(100, result)
            assertEquals("1043", s)
        }
    }
}
