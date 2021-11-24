package array

import array.complex.Complex
import kotlin.math.pow
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NearDouble(val expected: Double, val precision: Int) {
    fun assertNear(v: Double, message: String? = null) {
        val dist = 10.0.pow(-precision)
        val messageWithPrefix = if (message == null) "" else ": ${message}"
        assertTrue(expected > v - dist && expected < v + dist, "Expected=${expected}, result=${v}${messageWithPrefix}")
    }
}

class NearComplex(val expected: Complex, val realPrecision: Int, val imPrecision: Int) {
    fun assertNear(v: Complex, message: String? = null) {
        val realDist = 10.0.pow(-realPrecision)
        val imDist = 10.0.pow(-imPrecision)
        val messageWithPrefix = if (message == null) "" else ": ${message}"
        assertTrue(
            expected.real > v.real - realDist
                    && expected.real < v.real + realDist
                    && expected.imaginary > v.imaginary - imDist
                    && expected.imaginary < v.imaginary + imDist, "expected=${expected}, result=${v}${messageWithPrefix}")
    }
}

abstract class APLTest {
    fun parseAPLExpression(expr: String, withStandardLib: Boolean = false, collapse: Boolean = true, numTasks: Int? = null): APLValue {
        return parseAPLExpression2(expr, withStandardLib, collapse, numTasks).first
    }

    fun parseAPLExpression2(
        expr: String,
        withStandardLib: Boolean = false,
        collapse: Boolean = true,
        numTasks: Int? = null
    ): Pair<APLValue, Engine> {
        val engine = Engine(numTasks)
        engine.addLibrarySearchPath("standard-lib")
        if (withStandardLib) {
            engine.parseAndEval(StringSourceLocation("use(\"standard-lib.kap\")"), true)
        }
        val result = engine.parseAndEval(StringSourceLocation(expr), false)
        engine.withThreadLocalAssigned {
            return Pair(if (collapse) result.collapse() else result, engine)
        }
    }

    fun parseAPLExpressionWithOutput(
        expr: String,
        withStandardLib: Boolean = false,
        collapse: Boolean = true,
        numTasks: Int? = null
    ): Pair<APLValue, String> {
        val engine = Engine(numTasks)
        engine.addLibrarySearchPath("standard-lib")
        if (withStandardLib) {
            engine.parseAndEval(StringSourceLocation("use(\"standard-lib.kap\")"), true)
        }
        val output = StringBuilderOutput()
        engine.standardOutput = output
        val result = engine.parseAndEval(StringSourceLocation(expr), false)
        engine.withThreadLocalAssigned {
            return Pair(if (collapse) result.collapse() else result, output.buf.toString())
        }
    }

    fun assertArrayContent(expectedValue: Array<out Any>, value: APLValue) {
        assertEquals(expectedValue.size, value.size, "Array dimensions mismatch")
        for (i in expectedValue.indices) {
            when (val expected = expectedValue[i]) {
                is Int -> assertSimpleNumber(expected.toLong(), value.valueAt(i), "index: ${i}")
                is Long -> assertSimpleNumber(expected, value.valueAt(i), "index: ${i}")
                is Double -> assertSimpleDouble(expected, value.valueAt(i), "index: ${i}")
                is NearDouble -> expected.assertNear(value.valueAt(i).ensureNumber().asDouble(), "index: ${i}")
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

    fun assertNearDouble(nearDouble: NearDouble, result: APLValue, message: String? = null) {
        nearDouble.assertNear(result.ensureNumber().asDouble(), message)
    }

    fun assertComplexWithRange(real: Pair<Double, Double>, imaginary: Pair<Double, Double>, result: APLValue) {
        assertTrue(result.isScalar())
        val complex = result.ensureNumber().asComplex()
        val message = "expected: ${real} ${imaginary}, actual: ${complex}"
        assertTrue(real.first <= complex.real && real.second >= complex.real, message)
        assertTrue(imaginary.first <= complex.imaginary && imaginary.second >= complex.imaginary, message)
    }

    fun assertSimpleComplex(expected: Complex, result: APLValue, message: String? = null) {
        assertTrue(result.isScalar(), message)
        val v = result.unwrapDeferredValue()
        assertTrue(v is APLNumber, message)
        assertEquals(expected, v.ensureNumber().asComplex(), message)
    }

    fun assertString(expected: String, value: APLValue, message: String? = null) {
        val suffix = if (message != null) ": ${message}" else ""
        assertEquals(1, value.dimensions.size, "Expected rank-1, got: ${value.dimensions.size}${suffix}")
        val valueString = value.toStringValue()
        assertEquals(expected, valueString, "Expected '${expected}', got: '${valueString}'${suffix}")
    }

    fun assertAPLNull(value: APLValue) {
        assertDimension(dimensionsOfSize(0), value)
        assertEquals(0, value.dimensions[0])
    }

    fun assertAPLValue(expected: Any, result: APLValue, message: String? = null) {
        when (expected) {
            is Int -> assertSimpleNumber(expected.toLong(), result, message)
            is Long -> assertSimpleNumber(expected, result, message)
            is Double -> assertSimpleDouble(expected, result, message)
            is Complex -> assertSimpleComplex(expected, result, message)
            is String -> assertString(expected, result, message)
            is NearDouble -> assertNearDouble(expected, result, message)
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
