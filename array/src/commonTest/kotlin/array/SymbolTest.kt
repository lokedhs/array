package array

import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class SymbolTest : APLTest() {
    @Test
    fun testIntern() {
        val engine = Engine()
        val symbol1 = engine.internSymbol("symbol1")
        val symbol2 = engine.internSymbol("symbol2")
        val symbol3 = engine.internSymbol("symbol1")
        assertNotSame(symbol1, symbol2)
        assertNotSame(symbol2, symbol3)
        assertSame(symbol1, symbol3)
    }

    @Test
    fun testParseSymbol() {
        val engine = Engine()
        val result = engine.parseAndEval(StringSourceLocation("'foo"), false)
        assertSame(engine.internSymbol("foo"), result.ensureSymbol().value)
    }

    @Test
    fun keywordSymbol() {
        val engine = Engine()
        val result = engine.parseAndEval(StringSourceLocation(":foo"), false)
        assertSame(engine.makeNamespace("keyword").internSymbol("foo"), result.ensureSymbol().value)
    }
}
