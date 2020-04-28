package array

import kotlin.test.Test

class LoadTest : APLTest() {
    @Test
    fun loadSimpleSource() {
        val engine = Engine()
        val context = engine.makeRuntimeContext()
        val instr0 = engine.parseString("load \"test-data/test-source.kap\"")
        val res0 = instr0.evalWithContext(context)
        assertSimpleNumber(10, res0)
        val instr1 = engine.parseString("foo:bar 1")
        val res1 = instr1.evalWithContext(context)
        assertSimpleNumber(101, res1)
    }

    @Test
    fun ensureLoadPreservesOldNamespace() {
        val engine = Engine()
        val context = engine.makeRuntimeContext()
        val instr0 = engine.parseString("namespace(\"a\") x←1 ◊ load \"test-data/test-source.kap\"")
        val res0 = instr0.evalWithContext(context)
        assertSimpleNumber(10, res0)
        val instr1 = engine.parseString("foo:bar 1")
        val res1 = instr1.evalWithContext(context)
        assertSimpleNumber(101, res1)
        val instr2 = engine.parseString("x + 10")
        val res2 = instr2.evalWithContext(context)
        assertSimpleNumber(11, res2)
    }

    @Test
    fun ensureLoadPreservesOldNamespaceOnError() {
        val engine = Engine()
        val context = engine.makeRuntimeContext()
        val instr0 = engine.parseString("namespace(\"a\") x←1 ◊ load \"test-data/parse-error.kap\"")
        try {
            val res0 = instr0.evalWithContext(context)
        } catch (e: ParseException) {
            // expected
        }
        val instr2 = engine.parseString("x + 10")
        val res2 = instr2.evalWithContext(context)
        assertSimpleNumber(11, res2)
    }
}
