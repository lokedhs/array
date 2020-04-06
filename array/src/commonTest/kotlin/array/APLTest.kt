package array

import array.complex.Complex
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

    fun assertSimpleNumber(expected: Long, value: APLValue, expr: String? = null) {
        assertTrue(value.isScalar())
        val v = value.unwrapDeferredValue()
        val exprMessage = if (expr == null) null else "Expression: ${expr}"
        assertTrue(v is APLNumber, exprMessage)
        assertEquals(expected, value.ensureNumber().asLong(), exprMessage)
    }

    fun assertDoubleWithRange(expected: Pair<Double, Double>, value: APLValue) {
        assertTrue(value.isScalar())
        val v = value.unwrapDeferredValue()
        assertTrue(v is APLNumber)
        val num = value.ensureNumber().asDouble()
        assertTrue(expected.first <= num)
        assertTrue(expected.second >= num)
    }

    fun assertSimpleDouble(expected: Double, value: APLValue) {
        assertTrue(value.isScalar())
        val v = value.unwrapDeferredValue()
        assertTrue(v is APLNumber)
        assertEquals(expected, v.ensureNumber().asDouble())
    }

    fun assertComplexWithRange(real: Pair<Double, Double>, imaginary: Pair<Double, Double>, result: APLValue) {
        assertTrue(result.isScalar())
        val complex = result.ensureNumber().asComplex()
        val message = "expected: ${real} ${imaginary}, actual: ${complex}"
        assertTrue(real.first <= complex.real && real.second >= complex.real, message)
        assertTrue(imaginary.first <= complex.imaginary && imaginary.second >= complex.imaginary, message)
    }

    fun assertSimpleComplex(expected: Complex, result: APLValue) {
        assertTrue(result.isScalar())
        val v = result.unwrapDeferredValue()
        assertTrue(v is APLNumber)
        assertEquals(expected, v.ensureNumber().asComplex())
    }

    fun assertString(expected: String, value: APLValue) {
        assertEquals(1, value.dimensions().size)
        assertEquals(expected, arrayAsStringValue(value))
    }

    fun assertAPLNull(value: APLValue) {
        assertDimension(dimensionsOfSize(0), value)
        assertEquals(0, value.dimensions()[0])
    }

    fun assertAPLValue(expected: Any, result: APLValue) {
        when (expected) {
            is Int -> assertSimpleNumber(expected.toLong(), result)
            is Long -> assertSimpleNumber(expected, result)
            is Double -> assertSimpleDouble(expected, result)
            is Complex -> assertSimpleComplex(expected, result)
            is String -> assertString(expected, result)
            else -> throw IllegalArgumentException("No support for comparing values of type: ${expected::class.qualifiedName}")
        }
    }
}
