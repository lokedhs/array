package array

import array.complex.Complex
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
    fun parseNumberTypes() {
        val gen = makeGenerator("1 2 1.2 2.0 9. ¯2.3 ¯2. 0 0.0")
        assertInteger(1, gen.nextToken())
        assertInteger(2, gen.nextToken())
        assertDouble(Pair(1.11119, 1.20001), gen.nextToken())
        assertDouble(Pair(1.99999, 2.00001), gen.nextToken())
        assertDouble(Pair(8.99999, 9.00001), gen.nextToken())
        assertDouble(Pair(-2.30001, -2.29999), gen.nextToken())
        assertDouble(Pair(-2.00001, -1.99999), gen.nextToken())
        assertInteger(0, gen.nextToken())
        assertDouble(Pair(-0.00001, 0.00001), gen.nextToken())
        assertSame(EndOfFile, gen.nextToken())
    }

    private fun assertDouble(expected: Pair<Double, Double>, token: Token) {
        assertTrue(token is ParsedDouble)
        assertTrue(expected.first <= token.value)
        assertTrue(expected.second >= token.value)
    }

    private fun assertInteger(expected: Long, token: Token) {
        assertTrue(token is ParsedLong)
        assertEquals(expected, token.value)
    }

    @Test
    fun parseInvalidNumbers() {
        assertFailsWith<IllegalNumberFormat> {
            val gen = makeGenerator("2000a")
            gen.nextToken()
        }
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

    @Test
    fun testSymbolsWithNumbers() {
        val gen = makeGenerator("a1 a2 a3b aa2233")
        gen.nextToken().let { token ->
            assertTokenIsSymbol(gen, token, "a1")
        }
        gen.nextToken().let { token ->
            assertTokenIsSymbol(gen, token, "a2")
        }
        gen.nextToken().let { token ->
            assertTokenIsSymbol(gen, token, "a3b")
        }
        gen.nextToken().let { token ->
            assertTokenIsSymbol(gen, token, "aa2233")
        }
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun complexNumbers() {
        val gen = makeGenerator("1j2 0j2 2j0 1J2 0J2 ¯1j2 1j¯2 ¯1j¯2")
        gen.nextToken().let { token ->
            assertTrue(token is ParsedComplex)
            assertEquals(Complex(1.0, 2.0), token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is ParsedComplex)
            assertEquals(Complex(0.0, 2.0), token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is ParsedComplex)
            assertEquals(Complex(2.0, 0.0), token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is ParsedComplex)
            assertEquals(Complex(1.0, 2.0), token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is ParsedComplex)
            assertEquals(Complex(0.0, 2.0), token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is ParsedComplex)
            assertEquals(Complex(-1.0, 2.0), token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is ParsedComplex)
            assertEquals(Complex(1.0, -2.0), token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is ParsedComplex)
            assertEquals(Complex(-1.0, -2.0), token.value)
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
