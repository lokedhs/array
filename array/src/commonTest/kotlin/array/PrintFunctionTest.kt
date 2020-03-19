package array

import kotlin.test.Test
import kotlin.test.assertEquals

class PrintFunctionTest : APLTest() {
    @Test
    fun testPrint() {
        val output = StringBuilderOutput()
        val engine = Engine().apply {
            standardOutput = output
        }
        val instr = engine.parseString("print 200")
        val result = instr.evalWithContext(RuntimeContext(engine))
        assertSimpleNumber(200, result)
        assertEquals("200", output.buf.toString())
    }

    @Test
    fun testPrintString() {
        val output = StringBuilderOutput()
        val engine = Engine().apply {
            standardOutput = output
        }
        val instr = engine.parseString("print \"a\"")
        val result = instr.evalWithContext(RuntimeContext(engine))
        assertString("a", result)
        assertEquals("\"a\"", output.buf.toString())
    }

    private class StringBuilderOutput : CharacterOutput {
        val buf = StringBuilder()

        override fun writeString(s: String) {
            buf.append(s)
        }
    }
}
