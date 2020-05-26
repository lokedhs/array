package array.options

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ArgParserTest {
    @Test
    fun simpleTest() {
        val parser = ArgParser(Option("foo", true), Option("bar", false))
        val result = parser.parse(arrayOf("--foo=a", "--bar"))
        assertEquals(2, result.size)
        assertEquals("a", result["foo"])
        assertEquals(null, result["bar"])
    }

    @Test
    fun noArgOptionWithArgShouldFail() {
        assertFailsWith<InvalidOption> {
            ArgParser(Option("foo", false)).parse(arrayOf("--foo=a"))
        }
    }

    @Test
    fun argOptionWithoutArgShouldFail() {
        assertFailsWith<InvalidOption> {
            ArgParser(Option("foo", true)).parse(arrayOf("--foo"))
        }
    }

    @Test
    fun noArguments() {
        val result = ArgParser(Option("foo", true), Option("bar", false)).parse(emptyArray())
        assertEquals(0, result.size)
    }

    @Test
    fun invalidOptionFormat0() {
        assertFailsWith<InvalidOption> {
            ArgParser(Option("foo", true), Option("bar", false)).parse(arrayOf("-foo=a"))
        }
    }

    @Test
    fun invalidOptionFormat1() {
        assertFailsWith<InvalidOption> {
            ArgParser(Option("foo", true), Option("bar", false)).parse(arrayOf("-bar"))
        }
    }

    @Test
    fun invalidOptionFormat2() {
        assertFailsWith<InvalidOption> {
            ArgParser(Option("foo", true), Option("bar", false)).parse(arrayOf(" --foo=a"))
        }
    }
}
