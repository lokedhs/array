package array

import kotlin.test.Test

class LoadTest : APLTest() {
    @Test
    fun loadSimpleSource() {
        val engine = Engine()
        val instr0 = engine.parseStringInRootEnvironment("load \"test-data/test-source.kap\"")
        val res0 = instr0.evalWithContext(engine.rootContext)
        assertSimpleNumber(10, res0)
        val instr1 = engine.parseStringInRootEnvironment("foo:bar 1")
        val res1 = instr1.evalWithContext(engine.rootContext)
        assertSimpleNumber(101, res1)
    }

    @Test
    fun ensureLoadPreservesOldNamespace() {
        val engine = Engine()
        val instr0 = engine.parseStringInRootEnvironment("namespace(\"a\") x←1 ◊ load \"test-data/test-source.kap\"")
        val res0 = instr0.evalWithContext(engine.rootContext)
        assertSimpleNumber(10, res0)
        val instr1 = engine.parseStringInRootEnvironment("foo:bar 1")
        val res1 = instr1.evalWithContext(engine.rootContext)
        assertSimpleNumber(101, res1)
        val instr2 = engine.parseStringInRootEnvironment("x + 10")
        val res2 = instr2.evalWithContext(engine.rootContext)
        assertSimpleNumber(11, res2)
    }

    @Test
    fun ensureLoadPreservesOldNamespaceOnError() {
        val engine = Engine()
        val instr0 = engine.parseStringInRootEnvironment("namespace(\"a\") x←1 ◊ load \"test-data/parse-error.kap\"")
        try {
            val res0 = instr0.evalWithContext(engine.rootContext)
        } catch (e: ParseException) {
            // expected
        }
        val instr2 = engine.parseStringInRootEnvironment("x + 10")
        val res2 = instr2.evalWithContext(engine.rootContext)
        assertSimpleNumber(11, res2)
    }
}
