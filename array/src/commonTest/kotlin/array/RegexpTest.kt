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
    fun plainWithMatcher0() {
        val result = parseAPLExpression(
            """
            |(regexp:create "abc") regexp:matches "qweabcasd"
            """.trimMargin())
        assertSimpleNumber(1, result)
    }

    @Test
    fun plainWithMatcher1() {
        val result = parseAPLExpression(
            """
            |(regexp:create "abc") regexp:matches "xyztestcxz"
            """.trimMargin())
        assertSimpleNumber(0, result)
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

    @Test
    fun regexpTestMultiline() {
        val result = parseAPLExpression(
            """
            |(:multiLine regexp:create "^foo") regexp:matches "a
            |fooa
            |a
            |a"
            """.trimMargin())
        assertSimpleNumber(1, result)
    }

    @Test
    fun regexpTestCase() {
        val result0 = parseAPLExpression(
            """
            |(regexp:create "^foo$") regexp:matches "foO"
            """.trimMargin())
        assertSimpleNumber(0, result0)

        val result1 = parseAPLExpression(
            """
            |(:ignoreCase regexp:create "^foo$") regexp:matches "foO"
            """.trimMargin())
        assertSimpleNumber(1, result1)
    }

    @Test
    fun regexpWithIllegalFlags0() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression(
                """
            |:foo regexp:create "foo"
            """.trimMargin())
        }
    }

    @Test
    fun regexpWithIllegalFlags1() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression(
                """
            |:foo :bar regexp:create "foo"
            """.trimMargin())
        }
    }

    @Test
    fun regexpWithIllegalFlags2() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression(
                """
            |1 regexp:create "foo"
            """.trimMargin())
        }
    }

    @Test
    fun regexpWithIllegalFlags3() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression(
                """
            |(2 1 ⍴ :multiLine :ignoreCase) regexp:create "foo"
            """.trimMargin())
        }
    }
}
