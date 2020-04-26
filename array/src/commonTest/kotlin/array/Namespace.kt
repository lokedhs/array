package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

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
            val tokeniser = makeTokeniser(":bar")
            tokeniser.nextToken()
        }
        assertFailsWith<ParseException> {
            val tokeniser = makeTokeniser("bar:")
            tokeniser.nextToken()
        }
    }

    private fun makeTokeniser(content: String): TokenGenerator {
        val engine = Engine()
        return TokenGenerator(engine, StringSourceLocation(content))
    }
}
