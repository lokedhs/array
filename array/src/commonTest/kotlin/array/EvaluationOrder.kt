package array

import kotlin.test.Test
import kotlin.test.assertEquals

class EvaluationOrder : APLTest() {
    /**
     * Check that evaluation order is right-to left:
     *
     * ```
     * A foo[B] C
     * ```
     *
     * The evaluation order should be C, B, A
     */
    @Test
    fun functionCallEvaluationOrder() {
        class FooFunction : APLFunctionDescriptor {
            inner class FooFunctionImpl(pos: Position) : APLFunction(pos) {
                override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
                    return (a.ensureNumber(pos).asInt() + b.ensureNumber(pos).asInt() + axis!!.ensureNumber(pos)
                        .asInt() * 2).makeAPLNumber()
                }
            }

            override fun make(pos: Position) = FooFunctionImpl(pos)
        }

        val engine = Engine()
        engine.registerFunction(engine.internSymbol("foo", engine.currentNamespace), FooFunction())

        val output = StringBuilderOutput()
        engine.standardOutput = output
        val result = engine.parseAndEval(StringSourceLocation("(io:print 1) foo[io:print 2] io:print 3"), false)

        assertEquals("321", output.buf.toString())
        assertAPLValue(8, result)
    }

    /**
     * Ensure that the variable `a` is assigned before its value is read.
     */
    @Test
    fun expressionEvaluationOrder() {
        parseAPLExpression("a + 1 + a←2").let { result ->
            assertSimpleNumber(5, result)
        }
    }

    /**
     * Ensure that calls are evaluated when the result of a call is not used.
     * This is needed if a call is for side-effects only.
     */
    @Test
    fun collapseResultWhenNotUsed() {
        parseAPLExpressionWithOutput(
            """
            |∇ printx (v) {
            |  io:print v
            |  v
            |}
            |
            |printx¨11 22
            |33
        """.trimMargin()).let { (result, out) ->
            assertEquals("1122", out)
            assertSimpleNumber(33, result)
        }
    }
}
