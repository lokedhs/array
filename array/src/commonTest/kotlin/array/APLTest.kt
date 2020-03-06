package array

import kotlin.test.assertEquals

open class APLTest {
    fun parseAPLExpression(expr: String): APLValue {
        val engine = Engine()
        val instr = engine.parseString(expr)
        val context = RuntimeContext(engine)
        return instr.evalWithContext(context)
    }

    fun assertArrayContent(value: APLValue, content: Array<Int>) {
        assertEquals(value.size(), content.size)
        for (i in content.indices) {
            assertEquals(value.valueAt(i).ensureNumber().asLong(), content[i].toLong())
        }
    }
}
