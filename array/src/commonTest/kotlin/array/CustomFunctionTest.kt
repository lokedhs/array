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
        val result = engine.parseAndEval(StringSourceLocation("∇ foo (A) { io:print A ◊ A+10 } ◊ foo(1000) "), false)
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
    fun duplicatedArgumentsTest0() {
        assertFailsWith<ParseException> {
            parseAPLExpression("∇ foo (A;A) { A }")
        }
    }

    @Test
    fun duplicatedArgumentsTest1() {
        assertFailsWith<ParseException> {
            parseAPLExpression("∇ (A;A) foo (B) { A+B }")
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

    @Test
    fun simpleFunction0() {
        parseAPLExpression("f ⇐ - ⋄ (f 5) (3 f 1)").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(-5, 2), result)
        }
    }

    @Test
    fun simpleFork0() {
        parseAPLExpression("f ⇐ ⊢⊣, ⋄ 1 f 2").let { result ->
            assertSimpleNumber(2, result)
        }
    }

    @Test
    fun simpleFork1() {
        parseAPLExpression("f ⇐ ⊣⊢, ⋄ 1 f 2").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(1, 2), result)
        }
    }

    @Test
    fun simple2Train0() {
        parseAPLExpression("f ⇐ -, ⋄ 10 f 20").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(-10, -20), result)
        }
    }

    @Test
    fun simple2Train1() {
        parseAPLExpression("f ⇐ -* ⋄ 2 f 5").let { result ->
            assertSimpleNumber(-32, result)
        }
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

    @Test
    fun assignedFunction() {
        val result = parseAPLExpression(
            """
            |∇ foo { 1 + ⍵ }
            |200 + foo 100
            """.trimMargin())
        assertSimpleNumber(301, result)
    }

    @Test
    fun assignedFunctionTwoArg() {
        val result = parseAPLExpression(
            """
            |∇ foo { 1 + ⍵ + ⍺ }
            |200 + 5 foo 100
            """.trimMargin())
        assertSimpleNumber(306, result)
    }

    @Test
    fun assignedFunctionTwoArgWithOneArgShouldFail() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression(
                """
                |∇ foo { 1 + ⍵ + ⍺ }
                |200 + foo 100
                """.trimMargin())
        }
    }

    @Test
    fun assignedFunctionMultiline() {
        val result = parseAPLExpression(
            """
            |∇ foo {
            |  x ← 300
            |  1 + ⍵ + x
            |}
            |200 + foo 100
            """.trimMargin())
        assertSimpleNumber(601, result)
    }

    @Test
    fun multilineDefaultArgNames1Arg() {
        val result = parseAPLExpression(
            """
            |∇ bar {
            |  ⍵ + 10
            |}
            |bar 9
            """.trimMargin())
        assertSimpleNumber(19, result)
    }

    @Test
    fun multilineDefaultArgNames2Arg() {
        val result = parseAPLExpression(
            """
            |∇ bar {
            |  ⍵ + 10 + ⍺
            |}
            |2 bar 9
            """.trimMargin())
        assertSimpleNumber(21, result)
    }

    @Test
    fun localFunctionScope0() {
        val result = parseAPLExpression(
            """
            |foo ⇐ {
            |  30 + ⍵
            |}
            |∇ bar { 
            |  foo ⇐ {
            |    x ← 300
            |    1 + ⍵ + x
            |  }
            |  200 + foo ⍵
            |}
            |(foo 1) + bar 100 
            """.trimMargin())
        assertSimpleNumber(632, result)
    }

    @Test
    fun localFunctionScope1() {
        assertFailsWith<VariableNotAssigned> {
            parseAPLExpression(
                """
                |∇ bar { 
                |  foo ⇐ {
                |    1 + ⍵
                |  }
                |  100 + foo ⍵
                |}
                |foo 10 
                """.trimMargin())
        }
    }

    @Test
    fun localFunctionScope2() {
        val result = parseAPLExpression(
            """
            |foo ⇐ {
            |  30 + ⍵
            |}
            |bar ⇐ { 
            |  foo ⇐ {
            |    x ← 300
            |    1 + ⍵ + x
            |  }
            |  200 + foo ⍵
            |}
            |(foo 1) + bar 100 
            """.trimMargin())
        assertSimpleNumber(632, result)
    }

    @Test
    fun localFunctionScope3() {
        val result = parseAPLExpression(
            """
            |foo ⇐ {
            |  30 + ⍵
            |}
            |∇ bar { 
            |  200 + ⍵
            |}
            |bar 100 
            """.trimMargin())
        assertSimpleNumber(300, result)
    }

    @Test
    fun simpleFormMultiline0() {
        val result = parseAPLExpression(
            """
            |foo ⇐ {
            |  x ← 300
            |  1 + ⍵ + x
            |}
            |200 + foo 10
            """.trimMargin())
        assertSimpleNumber(511, result)
    }

    @Test
    fun simpleFormMultiline1() {
        val result = parseAPLExpression(
            """
            |foo ⇐ {
            |  x ← 300
            |  1 + ⍵ + ⍺ + ⍺ + x
            |}
            |200 + 3 foo 10
            """.trimMargin())
        assertSimpleNumber(517, result)
    }

    @Test
    fun invalidName0() {
        assertFailsWith<ParseException> {
            parseAPLExpression(
                """
                |1 ⇐ { ⍵ + 100 } 
                """.trimMargin())
        }
    }

    @Test
    fun invalidName1() {
        assertFailsWith<ParseException> {
            parseAPLExpression(
                """
                |1 x ⇐ { ⍵ + 100 } 
                """.trimMargin())
        }
    }

    /*
Monadic single argument      ∇             foo x           {
Destructuring                ∇             foo (x0;x1)     {
Dyadic single argument       ∇           x foo y           {
Destructuring                ∇     (x0;y0) foo (x1;y1)     {
Monadic op, monadic arg      ∇         (fn foo) x          {
Monadic op, dyadic arg       ∇       x (fn foo) y          {
Dyadic op, monadic arg       ∇         (f0 foo f1) x       {
Dyadic op, dyadic arg        ∇       x (f0 foo f1) y       {
Destructuring                ∇ (x0;x1) (f0 foo f1) (y0;y1) {
Fn parens optional:
Monadic single arg:          ∇            (foo) x          {
                             ∇            (foo) (x)        {
                             ∇        (y) (foo) (x)        {
                             ∇          y (foo) x          {
     */

// Testing the different formats of function definitions

    @Test
    fun monadicSingleArgument() {
        parseAPLExpression("∇ foo x { x+1 } ◊ foo 2").let { result ->
            assertSimpleNumber(3, result)
        }
    }

    @Test
    fun monadicDestructuring() {
        parseAPLExpression("∇ foo (x0;x1) { x0+x1+1 } ◊ foo (1;2)").let { result ->
            assertSimpleNumber(4, result)
        }
    }

    @Test
    fun dyadicSingleArgument() {
        parseAPLExpression("∇ x foo y { x+y } ◊ 1 foo 2").let { result ->
            assertSimpleNumber(3, result)
        }
    }

    @Test
    fun dyadicDestructuring() {
        parseAPLExpression("∇ (x0;x1) foo (y0;y1) { x0+x1+y0+y1+3 } ◊ (10;11) foo (1;2)").let { result ->
            assertSimpleNumber(27, result)
        }
    }

    @Test
    fun dyadicDestructuringNoSemicolonBoth() {
        assertFailsWith<ParseException> {
            parseAPLExpression("∇ (x0 x1) foo (y0 y1) { x0+x1+y0+y1+3 } ◊ (10;11) foo (1;2)")
        }
    }

    @Test
    fun dyadicDestructuringNoSemicolonLeft() {
        assertFailsWith<ParseException> {
            parseAPLExpression("∇ (x0 x1) foo (y0;y1) { x0+x1+y0+y1+3 } ◊ (10;11) foo (1;2)")
        }
    }

    @Test
    fun parenMonadicSingleArgument() {
        parseAPLExpression("∇ (foo) x { x+1 } ◊ foo 2").let { result ->
            assertSimpleNumber(3, result)
        }
    }

    @Test
    fun parenMonadicDestructuring() {
        parseAPLExpression("∇ (foo) (x0;x1) { x0+x1+1 } ◊ foo (1;2)").let { result ->
            assertSimpleNumber(4, result)
        }
    }

    @Test
    fun parenMonadicDestructuringNoSemicolon() {
        assertFailsWith<ParseException> {
            parseAPLExpression("∇ (foo) (x0 x1) { x0+x1+1 } ◊ foo (1;2)")
        }
    }

    @Test
    fun parenDyadicSingleArgument() {
        parseAPLExpression("∇ x (foo) y { x+y } ◊ 1 foo 2").let { result ->
            assertSimpleNumber(3, result)
        }
    }

    @Test
    fun parenLeftArgDyadicSingleArgument() {
        parseAPLExpression("∇ (x) foo y { x+y } ◊ 1 foo 2").let { result ->
            assertSimpleNumber(3, result)
        }
    }

    @Test
    fun parenRightArgDyadicSingleArgument() {
        parseAPLExpression("∇ x foo (y) { x+y } ◊ 1 foo 2").let { result ->
            assertSimpleNumber(3, result)
        }
    }

    @Test
    fun parenDyadicDestructuring() {
        parseAPLExpression("∇ (x0;x1) (foo) (y0;y1) { x0+x1+y0+y1+3 } ◊ (10;11) foo (1;2)").let { result ->
            assertSimpleNumber(27, result)
        }
    }

    @Test
    fun parenArgMonadicSingleArgument() {
        parseAPLExpression("∇ foo (x) { x+1 } ◊ foo 2").let { result ->
            assertSimpleNumber(3, result)
        }
    }

    @Test
    fun parenArgDyadicSingleArgument() {
        parseAPLExpression("∇ (x) foo (y) { x+y } ◊ 1 foo 2").let { result ->
            assertSimpleNumber(3, result)
        }
    }

    @Test
    fun parenFnAndArgumentMonadicSingleArgument() {
        parseAPLExpression("∇ foo x { x+1 } ◊ foo 2").let { result ->
            assertSimpleNumber(3, result)
        }
    }

    @Test
    fun parenFnAndArgDyadicSingleArgument() {
        parseAPLExpression("∇ (x) (foo) (y) { x+y } ◊ 1 foo 2").let { result ->
            assertSimpleNumber(3, result)
        }
    }

    @Test
    fun parenFnAndLeftArgDyadicSingleArgument() {
        parseAPLExpression("∇ (x) (foo) y { x+y } ◊ 1 foo 2").let { result ->
            assertSimpleNumber(3, result)
        }
    }

    @Test
    fun parenFnAndRightArgDyadicSingleArgument() {
        parseAPLExpression("∇ x (foo) (y) { x+y } ◊ 1 foo 2").let { result ->
            assertSimpleNumber(3, result)
        }
    }

    @Test
    fun emptyFunctionName() {
        assertFailsWith<ParseException> {
            parseAPLExpression("∇ (a;b) () (c;d) { a+b+c+d }")

        }
    }

    @Test
    fun tooManyFunctionArgs() {
        assertFailsWith<ParseException> {
            parseAPLExpression("∇ (a;b) foo (c;d) x { a+b+c+d }")

        }
    }

    /////////////////////////////////////////////////////////
    // Operators
    /////////////////////////////////////////////////////////

    @Test
    fun oneArgOperator0() {
        parseAPLExpression("∇ (x foo) a { 10 ⍞x a } ◊ +foo 2").let { result ->
            assertSimpleNumber(12, result)
        }
    }

    @Test
    fun oneArgOperator1() {
        parseAPLExpression("∇ a (x foo) b { a ⍞x b } ◊ 10 +foo 2").let { result ->
            assertSimpleNumber(12, result)
        }
    }

    @Test
    fun oneArgOperator2() {
        parseAPLExpression("∇ (x foo) (a0;a1;a2;a3) { 10 ⍞x (a0×a1×a2 ⍞x a3) } ◊ +foo (2;2;3;1)").let { result ->
            assertSimpleNumber(26, result)
        }
    }

    @Test
    fun oneArgOperatorSemicolon() {
        assertFailsWith<ParseException> {
            parseAPLExpression("∇ (x;foo) (a0;a1;a2;a3) { 10 ⍞x (a0×a1×a2 ⍞x a3) } ◊ +foo 2 2 3 1")
        }
    }

    @Test
    fun oneArgOperatorSemicolonTooManyArgs() {
        assertFailsWith<ParseException> {
            parseAPLExpression("∇ (x;foo;y;z) (a0;a1;a2;a3) { 10 ⍞x (a0×a1×a2 ⍞x a3) } ◊ +foo 2 2 3 1")
        }
    }

    @Test
    fun twoArgOperator0() {
        parseAPLExpression("∇ (x foo y) a { 100 ⍞y 10 ⍞x a } ◊ -foo+ 3").let { result ->
            assertSimpleNumber(107, result)
        }
    }

    @Test
    fun twoArgOperator1() {
        parseAPLExpression("∇ a (x foo y) b { 100 ⍞y a ⍞x b } ◊ 10 -foo+ 2").let { result ->
            assertSimpleNumber(108, result)
        }
    }

    @Test
    fun twoArgOperator2() {
        parseAPLExpression("∇ (a0;a1) (x foo y) b { 100 ⍞y a0 ⍞x a1 ⍞x b } ◊ (10;11) -foo+ 4").let { result ->
            assertSimpleNumber(103, result)
        }
    }

    @Test
    fun twoArgOperatorSemicolonNames() {
        assertFailsWith<ParseException> {
            parseAPLExpression("∇ (a0;a1) (x;foo;y) b { 100 ⍞y a0 ⍞x a1 ⍞x b } ◊ (10;11) -foo+ 4")
        }
    }

    @Test
    fun tooManyOperatorArguments() {
        assertFailsWith<ParseException> {
            parseAPLExpression("∇ (a0;a1) (z x foo y) b { 100 ⍞y a0 ⍞x a1 ⍞x b } ◊ (10;11) -foo+ 4")
        }
    }

    @Test
    fun twoArgOperatorMultiDatatype() {
        val result = parseAPLExpression(
            """
            |∇ a (x foo y) b {
            |  c ← if('function ≡ typeof(y)) {
            |    b ⍞y 1
            |  } else {
            |    y + b
            |  }
            |  c ⍞x a
            |}
            |(10 (×foo-) 20) (10 (×foo 20) 30)
            """.trimMargin(), true)
        assertArrayContent(arrayOf(190, 500), result)
    }

    @Test
    fun operatorWithLambdaFunctionLeftArg() {
        val result = parseAPLExpression(
            """
            |∇ foo x {
            |  x+100
            |}
            |∇ (x bar) y {
            |  2 + ⍞x y
            |}
            |f0 ← λfoo
            |⍞f0 bar 10
            """.trimMargin())
        assertSimpleNumber(112, result)
    }

    @Test
    fun operatorWithLambdaFunctionRightArgWithParen() {
        val result = parseAPLExpression(
            """
            |∇ foo x {
            |  x+30
            |}
            |∇ (x bar y) a {
            |  ⍞y 200 ⍞x a
            |}
            |f0 ← λfoo
            |(-bar ⍞f0) 10  
            """.trimMargin())
        assertSimpleNumber(220, result)
    }

    @Test
    fun operatorWithLambdaFunctionRightArgWithoutParen() {
        val result = parseAPLExpression(
            """
            |∇ foo x {
            |  x+100
            |}
            |∇ (x bar y) a {
            |  ⍞y 200 ⍞x a
            |}
            |f0 ← λfoo
            |-bar ⍞f0 10                    
            """.trimMargin())
        assertSimpleNumber(290, result)
    }

    /////////////////////////////////////////////////////////
    // Short form
    /////////////////////////////////////////////////////////

    @Test
    fun shortFormWithSimpleFunction() {
        val result = parseAPLExpression(
            """
            |foo ⇐ +
            |1 foo 2
            """.trimMargin())
        assertSimpleNumber(3, result)
    }

    @Test
    fun shortFormWith2Train() {
        val result = parseAPLExpression(
            """
            |foo ⇐ ×-
            |foo 5
            """.trimMargin())
        assertSimpleNumber(-1, result)
    }

    @Test
    fun shortFormWith3Train() {
        val result = parseAPLExpression(
            """
            |foo ⇐ ⊢⊣,
            |1 foo 2
            """.trimMargin())
        assertSimpleNumber(2, result)
    }

    @Test
    fun shortFormWithFunctionExpr() {
        val result = parseAPLExpression(
            """
            |foo ⇐ {⍵+1}
            |foo 100
            """.trimMargin())
        assertSimpleNumber(101, result)
    }

    @Test
    fun shortFormWithOperator() {
        val result = parseAPLExpression(
            """
            |∇ bar (a) { a+1 } 
            |foo ⇐ bar¨
            |foo 100 200 300 400 500 600
            """.trimMargin())
        assertDimension(dimensionsOfSize(6), result)
        assertArrayContent(arrayOf(101, 201, 301, 401, 501, 601), result)
    }

    @Test
    fun shortFormWithLeftArgAndCommuteShouldFail() {
        assertFailsWith<ParseException> {
            parseAPLExpression("foo ⇐ 10+⊢")
        }
    }

    @Test
    fun shortFormWithLeftArgAndForkShouldFail() {
        assertFailsWith<ParseException> {
            parseAPLExpression("foo ⇐ 10+⊢⊣")
        }
    }

    @Test
    fun shortFormWithLeftArgShouldFail() {
        assertFailsWith<ParseException> {
            parseAPLExpression("foo ⇐ 10+")
        }
    }

    @Test
    fun shortFormWithSimpleOperator() {
        parseAPLExpression("foo ⇐ ×/ ⋄ foo 1 2 3").let { result ->
            assertSimpleNumber(6, result)
        }
    }

    @Test
    fun redefineStdToShortForm() {
        val (result, out) = parseAPLExpressionWithOutput(
            """
            |∇ foo (x) { x+10 }
            |io:print foo 100
            |foo ⇐ { ⍵+20 }
            |io:print foo 101
            """.trimMargin())
        assertSimpleNumber(121, result)
        assertEquals("110121", out)
    }

    @Test
    fun redefineShortToStdForm() {
        val (result, out) = parseAPLExpressionWithOutput(
            """
            |foo ⇐ { ⍵+10 }
            |io:print foo 100
            |∇ foo (x) { x+20 }
            |io:print foo 160
            """.trimMargin())
        assertSimpleNumber(170, result)
        assertEquals("110170", out)
    }

    /**
     * This test ensures that functions with local scope are only visible within the scope it's called from.
     */
    @Test
    fun localScopeFunction() {
        val (result, out) = parseAPLExpressionWithOutput(
            """
            |∇ foo (x) { x+50 }
            |∇ bar (x) {
            |  foo ⇐ { ⍵+100 }
            |  foo x
            |}
            |∇ xyz (x) {
            |  io:print foo x+200
            |  io:print bar x+1000
            |}
            |xyz 8
            """.trimMargin())
        assertEquals("2581108", out)
        assertSimpleNumber(1108, result)
    }

    /**
     * This test ensures that functions with local scope can be referenced from outside the scope
     * if a closure is returned from the defining function.
     */
    @Test
    fun localScopeFunctionWithLambda() {
        val (result, out) = parseAPLExpressionWithOutput(
            """
            |∇ foo (x) {x+50 }
            |∇ bar (x) {
            |  foo ⇐ { ⍵+100 }
            |  (foo x) λfoo
            |}
            |∇ xyz (x) {
            |  io:print foo x+200
            |  a ← bar x+1000
            |  io:print a[0]
            |  b ← a[1]
            |  io:print ⍞b 31
            |}
            |xyz 100
            """.trimMargin())
        assertEquals("3501200131", out)
        assertSimpleNumber(131, result)
    }
}
