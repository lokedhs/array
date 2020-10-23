package array

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ComplexExpressionsTest : APLTest() {
    @Test
    fun parenExpressionWithScalarValue() {
        val result = parseAPLExpression("(1+2)")
        assertSimpleNumber(3, result)
    }

    @Test
    fun nestedArrayNoExpression() {
        val result = parseAPLExpression("(1 2) (3 4)")
        assertDimension(dimensionsOfSize(2), result)
        assertArrayContent(arrayOf(1, 2), result.valueAt(0))
        assertArrayContent(arrayOf(3, 4), result.valueAt(1))
    }

    @Test
    fun nestedArrayScalarValue() {
        val result = parseAPLExpression("(1) (2 3)")
        assertDimension(dimensionsOfSize(2), result)
        assertSimpleNumber(1, result.valueAt(0))
        assertArrayContent(arrayOf(2, 3), result.valueAt(1))
    }

    @Test
    fun nestedArrayWithScalarValueFromFn() {
        val result = parseAPLExpression("∇ foo (x) {1+x} ◊ (foo 1) (foo 6)")
        assertDimension(dimensionsOfSize(2), result)
        assertSimpleNumber(2, result.valueAt(0))
        assertSimpleNumber(7, result.valueAt(1))
    }

    @Test
    fun nestedArrayWithScalarValueFromExpr() {
        val result = parseAPLExpression("(1+2) (3+4) (1+5)")
        assertDimension(dimensionsOfSize(3), result)
        assertSimpleNumber(3, result.valueAt(0))
        assertSimpleNumber(7, result.valueAt(1))
        assertSimpleNumber(6, result.valueAt(2))
    }

    @Test
    fun doubleNestedArrays() {
        val result = parseAPLExpression("(⍳3) (10+⍳10)")
        assertDimension(dimensionsOfSize(2), result)
        result.valueAt(0).let { value ->
            assertDimension(dimensionsOfSize(3), value)
            assertArrayContent(arrayOf(0, 1, 2), value)
        }
        result.valueAt(1).let { value ->
            assertDimension(dimensionsOfSize(10), value)
            assertArrayContent(arrayOf(10, 11, 12, 13, 14, 15, 16, 17, 18, 19), value)
        }
    }

    @Test
    fun closeParenMissing() {
        assertFailsWith<ParseException> {
            parseAPLExpression("(1+2+3")
        }
    }

    @Test
    fun openParenMissing() {
        assertFailsWith<ParseException> {
            parseAPLExpression("1+2+3)")
        }
    }

    @Test
    fun closeBracketMissing() {
        assertFailsWith<ParseException> {
            parseAPLExpression("1 2 4 5 6 7 +/[")
        }
    }

    @Test
    fun openBracketMissing() {
        assertFailsWith<ParseException> {
            parseAPLExpression("1 2 3 4 5 6 7 +/2] 1")
        }
    }

    @Test
    fun closeBraceMissing() {
        assertFailsWith<ParseException> {
            parseAPLExpression("{1+2+3")
        }
    }

    @Test
    fun openBraceMissing() {
        assertFailsWith<ParseException> {
            parseAPLExpression("1+2+3}")
        }
    }

    @Test
    fun incorrectlyNestedParens1() {
        assertFailsWith<ParseException> {
            parseAPLExpression("(1+2 {3+4)}")
        }
    }

    @Test
    fun incorrectlyNestedParens2() {
        assertFailsWith<ParseException> {
            parseAPLExpression("{1+2 (3+4} 5 6 7)")
        }
    }

    @Test
    fun nestedFunctions() {
        val result = parseAPLExpression("{⍵+{1+⍵} 4} 5")
        assertSimpleNumber(10, result)
    }

    @Test
    fun nestedTwoArgFunctions() {
        val result = parseAPLExpression("200 {⍺+⍵+10 {1+⍺+⍵} 4} 5 ")
        assertSimpleNumber(220, result)
    }

    @Test
    fun multilineExpression() {
        parseAPLExpressionWithOutput(
            """
            |print 3
            |2
        """.trimMargin()).let { (result, output) ->
            assertSimpleNumber(2, result)
            assertEquals("3", output)
        }
    }

    @Test
    fun multilineExpressionWithBlankLines() {
        parseAPLExpressionWithOutput(
            """
            |print 3
            |
            |2
            |
        """.trimMargin()).let { (result, output) ->
            assertSimpleNumber(2, result)
            assertEquals("3", output)
        }
    }

    @Test
    fun functionInParens() {
        parseAPLExpression("8 16 32 (÷) 2").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(4, 8, 16), result)
        }
    }

    @Test
    fun twoFunctionCalls() {
        parseAPLExpression("1 + 2 + 3").let { result ->
            assertSimpleNumber(6, result)
        }
    }

    // Test ignored since it's not clear how the parser is supposed to handle this case at the moment
    @Test
    @Ignore
    fun multilineExpressionsShouldFail() {
        assertFailsWith<ParseException> {
            val v = parseAPLExpression("1 2 (\n4\n)").collapse()
            println("v = ${v}")
        }
        assertFailsWith<ParseException> {
            parseAPLExpression("1 2 (4\n)").collapse()
        }
    }
}
