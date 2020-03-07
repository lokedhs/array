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
        val dimensions = result.dimensions()
        assertTrue(result.dimensions().compare(expectDimensions), "expected dimension: $expectDimensions, actual $dimensions")
    }

    fun assertPairs(v: APLValue, vararg values: Array<Int>) {
        for (i in values.indices) {
            val cell = v.valueAt(i)
            val expectedValue = values[i]
            for (eIndex in expectedValue.indices) {
                assertEquals(expectedValue[eIndex].toLong(), cell.valueAt(eIndex).ensureNumber().asLong())
            }
        }
    }
}
