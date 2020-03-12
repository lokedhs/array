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

    fun assertArrayContent(expectedValue: Array<Int>, value: APLValue) {
        assertEquals(expectedValue.size, value.size())
        for (i in expectedValue.indices) {
            assertSimpleNumber(expectedValue[i].toLong(), value.valueAt(i))
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
                assertSimpleNumber(expectedValue[eIndex].toLong(), cell.valueAt(eIndex))
            }
        }
    }

    fun assertSimpleNumber(expected: Long, value: APLValue) {
        assertTrue(value.isScalar())
        val v = value.unwrapDeferredValue()
        assertTrue(v is APLNumber)
        assertEquals(expected, value.ensureNumber().asLong())
    }

    fun assertSimpleDouble(expected: Pair<Double, Double>, value: APLValue) {
        assertTrue(value.isScalar())
        val v = value.unwrapDeferredValue()
        assertTrue(v is APLNumber)
        val num = value.ensureNumber().asDouble()
        assertTrue(expected.first <= num)
        assertTrue(expected.second >= num)
    }

    fun assertString(expected: String, value: APLValue) {
        assertEquals(1, value.dimensions().size)
        assertEquals(expected, arrayAsStringValue(value))
    }

    fun assertAPLNull(value: APLValue) {
        assertDimension(dimensionsOfSize(0), value)
        assertEquals(0, value.dimensions()[0])
    }
}
