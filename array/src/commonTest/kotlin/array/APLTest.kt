package array

import array.complex.Complex
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

open class APLTest {
    fun parseAPLExpression(expr: String, withStandardLib: Boolean = false): APLValue {
        return parseAPLExpression2(expr, withStandardLib).first
    }

    fun parseAPLExpression2(expr: String, withStandardLib: Boolean = false): Pair<APLValue, Engine> {
        val engine = Engine()
        engine.addLibrarySearchPath("standard-lib")
        if (withStandardLib) {
            engine.parseAndEval(StringSourceLocation("use(\"standard-lib.kap\")"), true)
        }
        val result = engine.parseAndEval(StringSourceLocation(expr), false)
        return Pair(result.collapse(), engine)
    }

    fun parseAPLExpressionWithOutput(expr: String, withStandardLib: Boolean = false): Pair<APLValue, String> {
        val engine = Engine()
        engine.addLibrarySearchPath("standard-lib")
        if (withStandardLib) {
            engine.parseAndEval(StringSourceLocation("use(\"standard-lib.kap\")"), true)
        }
        val output = StringBuilderOutput()
        engine.standardOutput = output
        val result = engine.parseAndEval(StringSourceLocation(expr), false)
        return Pair(result.collapse(), output.buf.toString())
    }

    fun assertArrayContent(expectedValue: Array<out Any>, value: APLValue) {
        assertEquals(expectedValue.size, value.size, "Array dimensions mismatch")
        for (i in expectedValue.indices) {
            when (val expected = expectedValue[i]) {
                is Int -> assertSimpleNumber(expected.toLong(), value.valueAt(i), "index: ${i}")
                is Long -> assertSimpleNumber(expected, value.valueAt(i), "index: ${i}")
                is Double -> assertSimpleDouble(expected, value.valueAt(i), "index: ${i}")
                else -> throw IllegalArgumentException("Cannot check array member at index ${i}, type = ${expected::class.simpleName}")
            }
        }
    }

    fun assertDimension(expectDimensions: Dimensions, result: APLValue) {
        val dimensions = result.dimensions
        assertTrue(result.dimensions.compareEquals(expectDimensions), "expected dimension: $expectDimensions, actual $dimensions")
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
        val v = value.unwrapDeferredValue()
        val prefix = "Expected value: ${expected}, actual: ${value}"
        val exprMessage = if (expr == null) prefix else "${prefix}, expr: ${expr}"
        assertTrue(v.isScalar(), exprMessage)
        assertTrue(v is APLNumber, exprMessage)
        assertEquals(expected, value.ensureNumber().asLong(), exprMessage)
    }

    fun assertDoubleWithRange(expected: Pair<Double, Double>, value: APLValue) {
        assertTrue(value.isScalar())
        val v = value.unwrapDeferredValue()
        assertTrue(v is APLNumber)
        val num = value.ensureNumber().asDouble()
        assertTrue(expected.first <= num, "Comparison is not true: ${expected.first} <= ${num}")
        assertTrue(expected.second >= num, "Comparison is not true: ${expected.second} >= ${num}")
    }

    fun assertSimpleDouble(expected: Double, value: APLValue, message: String? = null) {
        assertTrue(value.isScalar(), message)
        val v = value.unwrapDeferredValue()
        assertTrue(v is APLNumber, message)
        assertEquals(expected, v.ensureNumber().asDouble(), message)
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
        assertEquals(1, value.dimensions.size)
        assertEquals(expected, value.toStringValue())
    }

    fun assertAPLNull(value: APLValue) {
        assertDimension(dimensionsOfSize(0), value)
        assertEquals(0, value.dimensions[0])
    }

    fun assertAPLValue(expected: Any, result: APLValue) {
        when (expected) {
            is Int -> assertSimpleNumber(expected.toLong(), result)
            is Long -> assertSimpleNumber(expected, result)
            is Double -> assertSimpleDouble(expected, result)
            is Complex -> assertSimpleComplex(expected, result)
            is String -> assertString(expected, result)
            else -> throw IllegalArgumentException("No support for comparing values of type: ${expected::class.simpleName}")
        }
    }

    fun assertSymbolName(engine: Engine, name: String, value: APLValue) {
        assertSame(engine.internSymbol(name), value.ensureSymbol().value)
    }

    fun assertSymbolNameCoreNamespace(engine: Engine, name: String, value: APLValue) {
        assertSame(engine.internSymbol(name, engine.coreNamespace), value.ensureSymbol().value)
    }

    @BeforeTest
    fun initTest() {
        nativeTestInit()
    }
}

expect fun nativeTestInit()
