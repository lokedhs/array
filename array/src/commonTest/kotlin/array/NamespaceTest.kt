package array

import kotlin.test.*

class NamespaceTest : APLTest() {
    @Test
    fun namespaceComparison() {
        val engine = Engine()
        val foo = engine.makeNamespace("foo")
        assertEquals("foo", foo.name)
        val bar = engine.makeNamespace("bar")
        assertEquals("bar", bar.name)
        val bar1 = engine.makeNamespace("bar")
        assertEquals("bar", bar1.name)
        assertSame(bar, bar1)
    }

    @Test
    fun namespaceSymbols() {
        val engine = Engine()
        val tokeniser = TokenGenerator(engine, StringSourceLocation("foo:bar bar aa:bar foo test bar:bar"))
        tokeniser.nextTokenWithType<Symbol>().let { token ->
            assertSame(engine.makeNamespace("foo"), token.namespace)
            assertEquals("foo", token.namespace.name)
            assertEquals("bar", token.symbolName)
        }
        tokeniser.nextTokenWithType<Symbol>().let { token ->
            assertSame(engine.currentNamespace, token.namespace)
            assertEquals("bar", token.symbolName)
        }
        tokeniser.nextTokenWithType<Symbol>().let { token ->
            assertSame(engine.makeNamespace("aa"), token.namespace)
            assertEquals("aa", token.namespace.name)
            assertEquals("bar", token.symbolName)
        }
        tokeniser.nextTokenWithType<Symbol>().let { token ->
            assertSame(engine.currentNamespace, token.namespace)
            assertEquals("foo", token.symbolName)
        }
        tokeniser.nextTokenWithType<Symbol>().let { token ->
            assertSame(engine.currentNamespace, token.namespace)
            assertEquals("test", token.symbolName)
        }
        tokeniser.nextTokenWithType<Symbol>().let { token ->
            assertSame(engine.makeNamespace("bar"), token.namespace)
            assertEquals("bar", token.namespace.name)
            assertEquals("bar", token.symbolName)
        }
        assertSame(EndOfFile, tokeniser.nextToken())
    }

    @Test
    fun aplCompareSymbols() {
        assertSimpleNumber(0, parseAPLExpression("'foo:bar ≡ 'aa:bar"))
        assertSimpleNumber(1, parseAPLExpression("'foo:bar ≡ 'foo:bar"))
        assertSimpleNumber(0, parseAPLExpression("'foo:bar ≡ 'foo:aa"))
    }

    @Test
    fun assignToNamespaceVariables() {
        parseAPLExpression("foo:bar ← 1 ◊ a:bar ← 2 ◊ foo:abc ← 3 ◊ foo:bar a:bar foo:abc").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(1, 2, 3), result)
        }
    }

    @Test
    fun invalidSymbolNames() {
        assertFailsWith<ParseException> {
            val tokeniser = makeTokeniser("foo:bar:test")
            tokeniser.nextToken()
        }
        assertFailsWith<ParseException> {
            val tokeniser = makeTokeniser("10bar")
            tokeniser.nextToken()
        }
        assertFailsWith<ParseException> {
            val tokeniser = makeTokeniser("bar:")
            tokeniser.nextToken()
        }
    }

    @Test
    fun changeNamespace() {
        parseAPLExpression("namespace(\"foo\") 'bar").let { result ->
            assertSym("bar", "foo", result)
        }
    }

    @Test
    fun changeNamespace2() {
        val result = parseAPLExpression("""
            |namespace("foo")
            |a ← 'aa:bb
            |b ← 'cc
            |namespace("bar")
            |c ← 'foo:dd
            |d ← 'ee
            |e ← 'aa:ff
            |namespace("xyz")
            |foo:a foo:b bar:c bar:d bar:e
        """.trimMargin())
        assertDimension(dimensionsOfSize(5), result)
        assertSym("bb", "aa", result.valueAt(0))
        assertSym("cc", "foo", result.valueAt(1))
        assertSym("dd", "foo", result.valueAt(2))
        assertSym("ee", "bar", result.valueAt(3))
        assertSym("ff", "aa", result.valueAt(4))
    }

    @Test
    fun defaultNamespaceFallback() {
        val result = parseAPLExpression("""
            |namespace("foo")
            |a ← 'cc
            |namespace("bar")
            |import("foo")
            |x ← 'a 'b 'foo:a
            |namespace("foo")
            |'a 'b , bar:x
        """.trimMargin())
        assertDimension(dimensionsOfSize(5), result)
        assertSym("a", "foo", result.valueAt(0))
        assertSym("b", "foo", result.valueAt(1))
        assertSym("a", "foo", result.valueAt(2))
        assertSym("b", "bar", result.valueAt(3))
        assertSym("a", "foo", result.valueAt(4))
    }

    @Test
    fun kapNamespaceIsAlwaysImported() {
        val result = parseAPLExpression("""
            |kap:foo ← 3
            |namespace("bar")
            |foo + 100
        """.trimMargin())
        assertSimpleNumber(103, result)
    }

    private fun makeTokeniser(content: String): TokenGenerator {
        val engine = Engine()
        return TokenGenerator(engine, StringSourceLocation(content))
    }

    private fun assertSym(expectedName: String, expectedPkg: String, result: APLValue) {
        assertTrue(result is APLSymbol)
        assertEquals(expectedName, result.value.symbolName)
        assertEquals(expectedPkg, result.value.namespace.name)
    }
}
