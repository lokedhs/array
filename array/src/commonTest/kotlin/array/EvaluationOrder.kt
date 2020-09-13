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
        val result = engine.parseAndEval(StringSourceLocation("(print 1) foo[print 2] print 3"), false)

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
}
