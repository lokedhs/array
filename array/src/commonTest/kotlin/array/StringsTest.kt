package array

import array.complex.Complex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StringsTest : APLTest() {
    @Test
    fun testPrint() {
        val out = parseWithOutput("print 200")
        assertSimpleNumber(200, out.result)
        assertEquals("200", out.output)
    }

    @Test
    fun testPrintPretty() {
        val out = parseWithOutput("'pretty print \"a\"")
        assertString("a", out.result)
        assertEquals("\"a\"", out.output)
    }

    @Test
    fun testPrintString() {
        val out = parseWithOutput("print \"a\"")
        assertString("a", out.result)
        assertEquals("a", out.output)
    }

    @Test
    fun readableNumber() {
        val out = parseWithOutput("'read print 1")
        assertSimpleNumber(1, out.result)
        assertEquals("1", out.output)
    }

    @Test
    fun readableString() {
        val out = parseWithOutput("'read print \"foo\"")
        assertString("foo", out.result)
        assertEquals("\"foo\"", out.output)
    }

    @Test
    fun readableComplex() {
        val out = parseWithOutput("'read print 1J2")
        assertEquals(Complex(1.0, 2.0), out.result.ensureNumber().asComplex())
        assertEquals("1.0J2.0", out.output)
    }

    @Test
    fun readCharsAsString() {
        val result = parseAPLExpression("@a @b")
        assertString("ab", result)
    }

    @Test
    fun nonBmpCharsInString() {
        val result = parseAPLExpression("\"\uD835\uDC9F\"")
        assertDimension(dimensionsOfSize(1), result)
        assertString("\uD835\uDC9F", result)
    }

    @Test
    fun nonBmpExplicitCharacter() {
        val result = parseAPLExpression("@\uD835\uDC9F")
        val v = result.unwrapDeferredValue()
        assertTrue(v is APLChar)
        assertEquals("\uD835\uDC9F", v.asString())
    }

    @Test
    fun formatNumber() {
        assertString("8", parseAPLExpression("⍕8"))
        assertString("100", parseAPLExpression("⍕100"))
        assertString("-100", parseAPLExpression("⍕¯100"))
    }

    @Test
    fun formatCharacter() {
        assertString("a", parseAPLExpression("⍕@a"))
        assertString("⍬", parseAPLExpression("⍕@⍬"))
    }

    @Test
    fun formatNull() {
        assertString("", parseAPLExpression("⍕⍬"))
    }

    @Test
    fun formatSelfString() {
        assertString("foo bar", parseAPLExpression("⍕\"foo bar\""))
    }

    class OutputResult(val engine: Engine, val output: String, val result: APLValue)

    private fun parseWithOutput(expr: String): OutputResult {
        val output = StringBuilderOutput()
        val engine = Engine().apply {
            standardOutput = output
        }
        val result = engine.parseAndEval(StringSourceLocation(expr), false)
        return OutputResult(engine, output.buf.toString(), result)
    }
}
