package array

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class RegexpTest : APLTest() {
    @Test
    fun plainRegexpMatches() {
        val result = parseAPLExpression(
            """
            |"abc" regexp:matches "qweabcasd"
            """.trimMargin())
        assertSimpleNumber(1, result)
    }

    @Test
    fun plainRegexpNoMatch() {
        val result = parseAPLExpression(
            """
            |"abc" regexp:matches "xyztestcxz"
            """.trimMargin())
        assertSimpleNumber(0, result)
    }

    @Test
    fun testFullStringMatch() {
        val result = parseAPLExpression(
            """
            |"^xyz*w$" regexp:matches "xyw"
            """.trimMargin())
        assertSimpleNumber(1, result)
    }

    @Test
    fun plainRegexSyntaxError() {
        assertFailsWith<InvalidRegexp> {
            parseAPLExpression("\"a[z\" regexp:matches \"foo\"")
        }
        assertFailsWith<InvalidRegexp> {
            parseAPLExpression("\"a(z\" regexp:matches \"foo\"")
        }
    }

    @Test
    fun regexpFind0() {
        val result = parseAPLExpression(
            """
            |"^zx:([a-z]+):x${'$'}" regexp:find "zx:test:x"
            """.trimMargin())
        assertDimension(dimensionsOfSize(2), result)
        assertString("zx:test:x", result.valueAt(0))
        assertString("test", result.valueAt(1))
    }

    @Test
    fun regexpFind1() {
        val (result, engine) = parseAPLExpression2(
            """
            |"^zx:(foo)?:x${'$'}" regexp:find "zx::x"
            """.trimMargin())
        assertDimension(dimensionsOfSize(2), result)
        assertString("zx::x", result.valueAt(0))
        assertSame(engine.keywordNamespace.internSymbol("undefined"), result.valueAt(1).ensureSymbol().value)
    }
}
