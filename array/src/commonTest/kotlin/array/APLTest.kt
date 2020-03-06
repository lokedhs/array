package array

import kotlin.test.assertEquals
import kotlin.test.assertTrue

open class APLTest {
    fun parseAPLExpression(expr: String): APLValue {
        val engine = Engine()
        val instr = engine.parseString(expr)
        val context = RuntimeContext(engine)
        return instr.evalWithContext(context)
    }

    fun assertArrayContent(content: Array<Int>, value: APLValue) {
        assertEquals(content.size, value.size())
        for (i in content.indices) {
            assertEquals(value.valueAt(i).ensureNumber().asLong(), content[i].toLong())
        }
    }

    fun assertDimension(expectDimensions: Dimensions, result: APLValue) {
        assertTrue(result.dimensions().compare(expectDimensions))
    }
}
