package array

import array.complex.Complex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StringsTest : APLTest() {
    @Test
    fun testPrint() {
        parseAPLExpressionWithOutput("io:print 200").let { (result, out) ->
            assertSimpleNumber(200, result)
            assertEquals("200", out)
        }
    }

    @Test
    fun testPrintPretty() {
        parseAPLExpressionWithOutput("'pretty io:print \"a\"").let { (result, out) ->
            assertString("a", result)
            assertEquals("\"a\"", out)
        }
    }

    @Test
    fun testPrintString() {
        parseAPLExpressionWithOutput("io:print \"a\"").let { (result, out) ->
            assertString("a", result)
            assertEquals("a", out)
        }
    }

    @Test
    fun readableNumber() {
        parseAPLExpressionWithOutput("'read io:print 1").let { (result, out) ->
            assertSimpleNumber(1, result)
            assertEquals("1", out)
        }
    }

    @Test
    fun readableString() {
        parseAPLExpressionWithOutput("'read io:print \"foo\"").let { (result, out) ->
            assertString("foo", result)
            assertEquals("\"foo\"", out)
        }
    }

    @Test
    fun readableComplex() {
        parseAPLExpressionWithOutput("'read io:print 1J2").let { (result, out) ->
            assertEquals(Complex(1.0, 2.0), result.ensureNumber().asComplex())
            assertEquals("1.0J2.0", out)
        }
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
}
