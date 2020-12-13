package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CustomFunctionTest : APLTest() {
    @Test
    fun oneArgFunction() {
        val result = parseAPLExpression("∇ foo (A) { A+1 } ◊ foo 10")
        assertSimpleNumber(11, result)
    }

    @Test
    fun twoArgFunction() {
        val result = parseAPLExpression("∇ (B) foo (A) { A+B+1 } ◊ 10 foo 20")
        assertSimpleNumber(31, result)
    }

    @Test
    fun oneArgListFunction() {
        val result = parseAPLExpression("∇ foo (A;B;C;D) { A+B+C+D+1 } ◊ foo (10;20;30;40)")
        assertSimpleNumber(101, result)
    }

    @Test
    fun twoArgListFunction() {
        val result = parseAPLExpression("∇ (E;F) foo (A;B;C;D) { A+B+C+D+E+F+1 } ◊ (1000;2000) foo (10;20;30;40)")
        assertSimpleNumber(3101, result)
    }

    @Test
    fun multipleFunctions() {
        val result = parseAPLExpression("∇ foo (A) { A+1 } ◊ ∇ bar (A) { A+10 } ◊ (foo 10) + (bar 100)")
        assertSimpleNumber(121, result)
    }

    @Test
    fun sideEffects() {
        val engine = Engine()
        val output = StringBuilderOutput()
        engine.standardOutput = output
        val result = engine.parseAndEval(StringSourceLocation("∇ foo (A) { print A ◊ A+10 } ◊ foo(1000) "), false)
        assertSimpleNumber(1010, result)
        assertEquals("1000", output.buf.toString())
    }

    @Test
    fun recursiveFunctionCall() {
        val result = parseAPLExpression("∇ foo (A) { A+1 } ◊ foo foo 2")
        assertSimpleNumber(4, result)
    }

    @Test
    fun recursiveFunctionCallWithMultiArguments() {
        val result = parseAPLExpression("∇ foo (A;B) { A+B+1 } ◊ foo (10 ; foo (1;2))")
        assertSimpleNumber(15, result)
    }

    @Test
    fun recursiveFunctionCallWithMultiArg2() {
        val result = parseAPLExpression("∇ (A;B) foo (C;D) { A+B+C+D+1 } ◊ (8;11) foo (10 ; (100;200) foo (1;2))")
        assertSimpleNumber(334, result)
    }

    @Test
    fun selfRecursion() {
        parseAPLExpression("n←0 ◊ ∇ foo (A) { if(A≡0) { 1 } else {  n←n+1 ◊ foo ¯1+A } } ◊ n,foo 10", true).let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(10, 1), result)
        }
    }

    @Test
    fun multilineFunction() {
        val result = parseAPLExpression(
            """
            |∇ foo (x) {
            |  a ← 10
            |  b ← 2
            |  a+b+x
            |}
            |foo(100)
            """.trimMargin())
        assertSimpleNumber(112, result)
    }

    @Test
    fun tooFewArgumentsLeft() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("∇ (E;F) foo (A;B;C;D) { A+B+C+D+E+F+1 } ◊ (1) foo (2;3;4;5)")
        }
    }

    @Test
    fun tooManyArgumentsLeft() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("∇ (E;F) foo (A;B;C;D) { A+B+C+D+E+F+1 } ◊ (1;2;3) foo (2;3;4;5)")
        }
    }

    @Test
    fun tooFewArgumentsRight() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("∇ (E;F) foo (A;B;C;D) { A+B+C+D+E+F+1 } ◊ (1;2) foo (2;3;4)")
        }

    }

    @Test
    fun tooManyArgumentsRight() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("∇ (E;F) foo (A;B;C;D) { A+B+C+D+E+F+1 } ◊ (1;2) foo (2;3;4;5;6)")
        }
    }

    @Test
    fun illegalTypeInArgument() {
        assertFailsWith<ParseException> {
            parseAPLExpression("∇ (E;1) foo (A;B;C;D) { A+B+C+D+E+1 }")
        }
        assertFailsWith<ParseException> {
            parseAPLExpression("∇ (E;F) foo (A;B;C;D;1) { A+B+C+D+E+F+1 }")
        }
    }

    @Test
    fun duplicatedArgumentsTest() {
        assertFailsWith<ParseException> {
            parseAPLExpression("∇ (A;A) { A }")
        }
    }

    @Test
    fun duplicatedArgumentsTest2() {
        assertFailsWith<ParseException> {
            parseAPLExpression("∇ (A;B;C;D;E;F;G;H;J;D;K;L;M) { A }")
        }
    }

    @Test
    fun functionArgumentsAreLocal() {
        parseAPLExpression("a←1 ◊ ∇ b (a) { a←2+a } ◊ b 100 ◊ a").let { result ->
            assertSimpleNumber(1, result)
        }
    }

    @Test
    fun functionArgumentsAreLocalTwoArg() {
        parseAPLExpression("a←1 ◊ c←2 ◊ ∇ (c) b (a) { a←4 ◊ c←3 } ◊ 1000 b 100 ◊ a c").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(1, 2), result)
        }
    }

    @Test
    fun functionTwoArgCallWithOneArg() {
        assertFailsWith<VariableNotAssigned> {
            parseAPLExpression(
                """
                |∇ (x) foo (y) {
                |  x + y + 1
                |}  
                |foo 10
                """.trimMargin())
        }
    }

    @Test
    fun twoArgFunctionWithArgCheck() {
        val result = parseAPLExpression(
            """
            |∇ (x) foo (y) {
            |  if (isLocallyBound('x)) {
            |    x + y + 1
            |  } else {
            |    y + 1000
            |  }
            |}  
            |(foo 1) (1 foo 2)              
            """.trimMargin(), true)
        assertDimension(dimensionsOfSize(2), result)
        assertArrayContent(arrayOf(1001, 4), result)
    }

    /**
     * This test verifies that after redefining a function, the new definition is used
     * from code which was previously parsed when the old definition was in place.
     */
    @Test
    fun functionRedefinition() {
        val engine = Engine()
        engine.parseAndEval(StringSourceLocation("∇ foo (x) { x+1 }"), false).collapse()
        engine.parseAndEval(StringSourceLocation("foo 100"), false).let { result ->
            assertSimpleNumber(101, result.collapse())
        }
        engine.parseAndEval(StringSourceLocation("∇ bar (x) { foo x + 2 }"), false).collapse()
        engine.parseAndEval(StringSourceLocation("bar 210"), false).let { result ->
            assertSimpleNumber(213, result.collapse())
        }
        // Redefine foo and call bar again to confirm that bar now has the new definition
        engine.parseAndEval(StringSourceLocation("∇ foo (x) { x + 5 }"), false).collapse()
        engine.parseAndEval(StringSourceLocation("foo 310"), false).let { result ->
            assertSimpleNumber(315, result.collapse())
        }
        // This should use the new definition of foo
        engine.parseAndEval(StringSourceLocation("bar 410"), false).let { result ->
            assertSimpleNumber(417, result.collapse())
        }
    }

    /**
     * Test to ensure that native functions cannot be redefined.
     */
    @Test
    fun nativeFunctionRedefinition() {
        assertFailsWith<InvalidFunctionRedefinition> {
            parseAPLExpression("∇ + (x) { 1 + x }")
        }
    }
}
