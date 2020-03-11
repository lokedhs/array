package array

import kotlin.test.*

class TokenGeneratorTest {
    @Test
    fun testSimpleToken() {
        val gen = makeGenerator("foo")
        val token = gen.nextToken()
        assertTokenIsSymbol(gen, token, "foo")
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun testMultipleTokens() {
        val gen = makeGenerator("foo bar test abc test")
        val expectedTokens = arrayOf("foo", "bar", "test", "abc", "test")
        expectedTokens.forEach { name ->
            val token = gen.nextToken()
            assertTokenIsSymbol(gen, token, name)
        }
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun testMultipleSpaces() {
        val gen = makeGenerator("     foo       bar     test        ")
        val expectedTokens = arrayOf("foo", "bar", "test")
        expectedTokens.forEach { name ->
            val token = gen.nextToken()
            assertTokenIsSymbol(gen, token, name)
        }
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun singleCharFunction() {
        val gen = makeGenerator("+-,,")
        val expectedTokens = arrayOf("+", "-", ",", ",")
        expectedTokens.forEach { name ->
            val token = gen.nextToken()
            assertTokenIsSymbol(gen, token, name)
        }
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun parseNumbers() {
        val gen = makeGenerator("10 20 1 ¯1 ¯10")
        val expectedTokens = arrayOf(10, 20, 1, -1, -10)
        expectedTokens.forEach { value ->
            val token = gen.nextToken()
            assertTrue(token is ParsedLong)
            assertEquals(value.toLong(), token.value)
        }
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun parseComments() {
        val gen = makeGenerator("foo ⍝ test comment")
        val token = gen.nextToken()
        assertTokenIsSymbol(gen, token, "foo")
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun testStrings() {
        val gen = makeGenerator("\"foo\" \"embedded\\\"quote\"")
        gen.nextToken().let { token ->
            assertTrue(token is StringToken)
            assertEquals("foo", token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is StringToken)
            assertEquals("embedded\"quote", token.value)
        }
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun testUnterminatedString() {
        val gen = makeGenerator("\"bar")
        assertFailsWith<ParseException> {
            gen.nextToken()
        }
    }

    private fun makeGenerator(content: String): TokenGenerator {
        val engine = Engine()
        return TokenGenerator(engine, StringCharacterProvider(content))
    }

    private fun assertTokenIsSymbol(gen: TokenGenerator, token: Token, name: String) {
        assertTrue(token is Symbol)
        assertEquals(gen.engine.internSymbol(name), token)
        assertEquals(name, token.value)
    }
}
