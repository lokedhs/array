package array

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class EvalLambdaFunc : APLTest() {
    @Test
    fun simpleLambdaStatement() {
        val result = parseAPLExpression("a ← λ { 1 + ⍵ } ◊ (⍞a 1) + ⍞a 5")
        assertSimpleNumber(8, result)
    }

    @Test
    fun simpleTestTwoArg() {
        val result = parseAPLExpression("foo ← λ{⍺+⍵+1} ◊ 10 ⍞foo 3000")
        assertSimpleNumber(3011, result)
    }

    @Test
    fun callWithArray1ArgAndLeftExpression() {
        val result = parseAPLExpression("foo ← λ{⍵+1} ◊ 20 + ⍞foo 10 20 30 40")
        assertArrayContent(arrayOf(31, 41, 51, 61), result)
    }

    /**
     * This test should fail, since the lambda function expects a left argument, but there is none provided.
     */
    @Test
    fun callWithArray2ArgAndLeftExpressionError() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("foo ← λ{⍺+⍵+1} ◊ 20 + ⍞foo 10 20 30 40")
        }
    }

    @Test
    fun callWithArray2ArgAndLeftExpression() {
        val result = parseAPLExpression("foo ← λ{⍺+⍵+1} ◊ 20 + 6 ⍞foo 10 20 30 40")
        assertArrayContent(arrayOf(37, 47, 57, 67), result)
    }

    @Test
    fun applyOnNonFunction() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("x ← 1 ◊ ⍞x 10")
        }
    }

    @Test
    fun applyOnExpression() {
        val result = parseAPLExpression("foo ← λ { 1 + ⍵ } ◊ bar ← λ { ⍵ } ◊ ⍞(⍞bar foo) 7")
        assertSimpleNumber(8, result)
    }

    @Test
    fun typeOfSimpleLambda() {
        val engine = Engine()
        val result = evalWithEngine(engine, "typeof λ { 1 + ⍵ }")
        assertSymbolName(engine, "function", result)
    }

    @Test
    fun arrayOfFunctionTypes() {
        val engine = Engine()
        val result = evalWithEngine(engine, "(typeof λ { 1 + ⍵ }) (typeof λ { 5 + ⍵ })")
        assertDimension(dimensionsOfSize(2), result)
        assertSymbolName(engine, "function", result.valueAt(0))
        assertSymbolName(engine, "function", result.valueAt(1))
    }

    @Test
    fun arrayOfFunctions() {
        val engine = Engine()
        val result = evalWithEngine(engine, "x←λ { 1 + ⍵ } λ { 5 + ⍵ } ◊ (typeof x) (typeof 0⌷x)")
        assertDimension(dimensionsOfSize(2), result)
        assertSymbolName(engine, "array", result.valueAt(0))
        assertSymbolName(engine, "function", result.valueAt(1))
    }

    @Test
    fun applyWithOperator() {
        val result = parseAPLExpression("x←λ { 1 + ⍵ } ◊ ⍞x¨ 1 2 3 4")
        assertDimension(dimensionsOfSize(4), result)
        assertArrayContent(arrayOf(2, 3, 4, 5), result)
    }

    private fun evalWithEngine(engine: Engine, expr: String): APLValue {
        val instr = engine.parseString(expr)
        return instr.evalWithContext(RuntimeContext(engine))
    }

    private fun assertSymbolName(engine: Engine, name: String, value: APLValue) {
        assertSame(engine.internSymbol(name), value.ensureSymbol().value)
    }
}
