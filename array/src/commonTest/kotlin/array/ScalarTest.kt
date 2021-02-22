package array

import array.complex.Complex
import kotlin.math.pow
import kotlin.math.sign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ScalarTest : APLTest() {
    @Test
    fun testReshape() {
        val result = parseAPLExpression("3 4 ⍴ ⍳100")
        assertDimension(dimensionsOfSize(3, 4), result)
        assertArrayContent(arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), result)
    }

    @Test
    fun testAdd() {
        runScalarTest("+") { a, b -> a + b }
    }

    @Test
    fun testSub() {
        runScalarTest("-") { a, b -> a - b }
    }

    @Test
    fun testMul() {
        runScalarTest("×") { a, b -> a * b }
    }

    @Test
    fun testDiv() {
        runScalarTest("÷") { a, b -> a / b }
    }

    @Test
    fun testPow() {
        runScalarTest("⋆") { a, b -> a.pow(b) }
    }

    @Test
    fun testAdd1Arg() {
        runScalarTest1Arg("+") { a -> a }
    }

    @Test
    fun testSub1Arg() {
        runScalarTest1Arg("-") { a -> -a }
    }

    @Test
    fun testMulArg() {
        runScalarTest1Arg("×") { a -> a.sign }
    }

    @Test
    fun testDivArg() {
        runScalarTest1Arg("÷") { a -> if (a == 0.0) 0.0 else 1 / a }
    }

    @Test
    fun testCompareEquals() {
        runScalarTest("=") { a, b -> if (a == b) 1.0 else 0.0 }
    }

    @Test
    fun testCompareNotEquals() {
        runScalarTest("≠") { a, b -> if (a != b) 1.0 else 0.0 }
    }

    @Test
    fun additionWithAxis0() {
        val result = parseAPLExpression("10 20 30 40 +[0] 4 3 2 ⍴ 100+⍳24")
        assertDimension(dimensionsOfSize(4, 3, 2), result)
        assertArrayContent(
            arrayOf(
                110, 111, 112, 113, 114, 115, 126, 127, 128, 129, 130, 131, 142, 143, 144, 145, 146, 147, 158, 159, 160, 161, 162, 163),
            result)
    }

    @Test
    fun additionWithAxis1() {
        val result = parseAPLExpression("10 20 30 +[1] 4 3 2 ⍴ 100+⍳24")
        assertDimension(dimensionsOfSize(4, 3, 2), result)
        assertArrayContent(
            arrayOf(
                110, 111, 122, 123, 134, 135, 116, 117, 128, 129, 140, 141, 122, 123, 134, 135, 146, 147, 128, 129, 140, 141, 152, 153),
            result)
    }

    @Test
    fun additionWithAxis2() {
        val result = parseAPLExpression("10 20 +[2] 4 3 2 ⍴ 100+⍳24")
        assertDimension(dimensionsOfSize(4, 3, 2), result)
        assertArrayContent(
            arrayOf(
                110, 121, 112, 123, 114, 125, 116, 127, 118, 129, 120, 131, 122, 133,
                124, 135, 126, 137, 128, 139, 130, 141, 132, 143),
            result)
    }

    @Test
    fun multiFunctionPlus() {
        parseAPLExpression("0 1 2 + 10 11 12 + 20 21 22").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(30, 33, 36), result)
        }
    }

    @Test
    fun multiFunctionPlusWithSpecialisedArray() {
        parseAPLExpression("(internal:ensureLong 0 1 2) + (internal:ensureLong 10 11 12) + (internal:ensureLong 20 21 22)").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(30, 33, 36), result)
        }
    }

    @Test
    fun multiFunctionPlusAndMinus() {
        parseAPLExpression("20 21 22 - 0 1 2 + 10 11 12").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(10, 9, 8), result)
        }
    }

    @Test
    fun multiFunctionPlusAndMinusSpecialised() {
        parseAPLExpression("(internal:ensureLong 20 21 22) - (internal:ensureLong 0 1 2) + (internal:ensureLong 10 11 12)").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(10, 9, 8), result)
        }
    }

    @Test
    fun failWithWrongRank() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("(2 3 ⍴ ⍳6) +[0] 2 3 4 ⍴ ⍳24")
        }
    }

    @Test
    fun testMax() {
        // ints
        runMaxTest(2, "⌈", "1", "2")
        runMaxTest(0, "⌈", "0", "0")
        runMaxTest(1, "⌈", "1", "0")
        runMaxTest(2, "⌈", "¯10", "2")
        runMaxTest(-10, "⌈", "¯10", "¯20")
        // doubles
        runMaxTest(2.0, "⌈", "1.0", "2.0")
        runMaxTest(0.0, "⌈", "0.0", "0.0")
        runMaxTest(1.0, "⌈", "1.0", "0.0")
        runMaxTest(2.0, "⌈", "¯10.0", "2.0")
        runMaxTest(-10.0, "⌈", "¯10.0", "¯20.0")
        // combination
        runMaxTest(2, "⌈", "2", "1.0")
        runMaxTest(2.0, "⌈", "2.0", "¯9")
        runMaxTest(0.0, "⌈", "0.0", "¯2")
        runMaxTest(10, "⌈", "10", "1.0")
        // complex
        runMaxTest(Complex(2.0, 3.0), "⌈", "2J3", "1J4")
        runMaxTest(Complex(4.0, 6.0), "⌈", "4J2", "4J6")
        // characters
        parseAPLExpression("@a⌈@b").let { result ->
            val v = result.unwrapDeferredValue()
            assertTrue(v is APLChar)
            assertEquals('b'.toInt(), v.value)
        }
        parseAPLExpression("@C⌈@D").let { result ->
            val v = result.unwrapDeferredValue()
            assertTrue(v is APLChar)
            assertEquals('D'.toInt(), v.value)
        }
    }

    @Test
    fun testMin() {
        runMaxTest(1, "⌊", "1", "2")
        runMaxTest(0, "⌊", "0", "0")
        runMaxTest(0, "⌊", "1", "0")
        runMaxTest(-10, "⌊", "¯10", "2")
        runMaxTest(-20, "⌊", "¯10", "¯20")

        runMaxTest(1.0, "⌊", "1.0", "2.0")
        runMaxTest(0.0, "⌊", "0.0", "0.0")
        runMaxTest(0.0, "⌊", "1.0", "0.0")
        runMaxTest(-10.0, "⌊", "¯10.0", "2.0")
        runMaxTest(-20.0, "⌊", "¯10.0", "¯20.0")

        runMaxTest(1.0, "⌊", "2", "1.0")
        runMaxTest(-9, "⌊", "2.0", "¯9")
        runMaxTest(-2, "⌊", "0.0", "¯2")
        runMaxTest(1.0, "⌊", "10", "1.0")

        runMaxTest(Complex(1.0, 4.0), "⌊", "2J3", "1J4")
        runMaxTest(Complex(4.0, 2.0), "⌊", "4J2", "4J6")

        parseAPLExpression("@a⌊@b").let { result ->
            val v = result.unwrapDeferredValue()
            assertTrue(v is APLChar)
            assertEquals('a'.toInt(), v.value)
        }
        parseAPLExpression("@C⌊@D").let { result ->
            val v = result.unwrapDeferredValue()
            assertTrue(v is APLChar)
            assertEquals('C'.toInt(), v.value)
        }
    }

    @Test
    fun minComparingIncompatibleTypes() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("@a⌈1").collapse()
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("1⌈@a").collapse()
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("@a⌊1").collapse()
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("1⌊@a").collapse()
        }
    }

    @Test
    fun testCeiling() {
        assertSimpleDouble(2.0, parseAPLExpression("⌈1.4"))
        assertSimpleDouble(3.0, parseAPLExpression("⌈2.9"))
        assertSimpleDouble(-3.0, parseAPLExpression("⌈¯3.1"))
        assertSimpleDouble(9.0, parseAPLExpression("⌈9"))
        assertSimpleComplex(Complex(4.0, 6.0), parseAPLExpression("⌈4.1J5.2"))
        assertSimpleComplex(Complex(91.0, 2.0), parseAPLExpression("⌈90.8J1.9"))
        assertSimpleComplex(Complex(-1.0, -5.0), parseAPLExpression("⌈¯1.8J¯4.82"))
        assertSimpleComplex(Complex(-10.0, -40.0), parseAPLExpression("⌈¯10.1J¯40.1"))
    }

    @Test
    fun testFloor() {
        assertSimpleDouble(5.0, parseAPLExpression("⌊5.9"))
        assertSimpleDouble(3.0, parseAPLExpression("⌊3.1"))
        assertSimpleDouble(-6.0, parseAPLExpression("⌊¯5.1"))
        assertSimpleDouble(-9.0, parseAPLExpression("⌊¯8.9"))
        assertSimpleComplex(Complex(1.0, 4.0), parseAPLExpression("⌊1.1J3.9"))
        assertSimpleComplex(Complex(2.0, 3.0), parseAPLExpression("⌊1.9J3.9"))
        assertSimpleComplex(Complex(-2.0, -7.0), parseAPLExpression("⌊¯1.3J¯7.0"))
        assertSimpleComplex(Complex(-4.0, -6.0), parseAPLExpression("⌊-4.1J5.2"))
        assertSimpleComplex(Complex(1.0, 9.0), parseAPLExpression("⌊1.01J9.9"))
    }

    @Test
    fun ceilingWithIllegalType() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⌈@a").collapse()
        }
    }

    @Test
    fun failWithWrongDimension() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("1 2 3 4 +[0] 5 6 7 ⍴ ⍳24")
        }
    }

    @Test
    fun floorWithIllegalType() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⌊@x").collapse()
        }
    }

    @Test

    fun failWithWrongAxis() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("1 2 3 4 +[3] 5 6 7 ⍴ ⍳24")
        }
    }

    @Test
    fun floorConvertsComplexToDouble() {
        val result = parseAPLExpression("⌊3.4J0.01")
        val v = result.unwrapDeferredValue()
        assertTrue(v is APLDouble, "expected APLDouble, actual type: ${v::class.simpleName}")
        assertSimpleDouble(3.0, v)
    }

    @Test
    fun scalarFunctionWithEnclosedArg() {
        parseAPLExpression("(⊂1 2 3) + 10 20").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            result.valueAt(0).let { v ->
                assertDimension(dimensionsOfSize(3), v)
                assertArrayContent(arrayOf(11, 12, 13), v)
            }
            result.valueAt(1).let { v ->
                assertDimension(dimensionsOfSize(3), v)
                assertArrayContent(arrayOf(21, 22, 23), v)
            }
        }
    }

    private fun runMaxTest(expected: Any, op: String, a: String, b: String) {
        assertAPLValue(expected, parseAPLExpression("${a}${op}${b}"))
        assertAPLValue(expected, parseAPLExpression("${b}${op}${a}"))
    }

    private fun runScalarTest1Arg(functionName: String, doubleFn: (Double) -> Double) {
        val result = parseAPLExpression("${functionName} ¯4.0+⍳10")
        assertDimension(dimensionsOfSize(10), result)
        for (i in 0 until result.dimensions[0]) {
            assertEquals(
                doubleFn((i - 4).toDouble()),
                result.valueAt(i).ensureNumber().asDouble(),
                "function: ${functionName}, arg: ${i - 4}"
            )
        }
    }

    private fun runScalarTest(functionName: String, doubleFn: (Double, Double) -> Double) {
        runScalarTestSD(functionName, doubleFn)
        runScalarTestDS(functionName, doubleFn)
    }

    private fun runScalarTestSD(functionName: String, doubleFn: (Double, Double) -> Double) {
        val result = parseAPLExpression("100 $functionName 100+3 4 ⍴ ⍳100")
        assertDimension(dimensionsOfSize(3, 4), result)
        for (i in 0 until result.size) {
            assertEquals(
                doubleFn(100.0, (100 + i).toDouble()),
                result.valueAt(i).ensureNumber().asDouble(),
                "function: ${functionName}}, index: ${i}"
            )
        }
    }

    private fun runScalarTestDS(functionName: String, doubleFn: (Double, Double) -> Double) {
        val result = parseAPLExpression("(100+3 4 ⍴ ⍳100) $functionName 10")
        assertDimension(dimensionsOfSize(3, 4), result)
        for (i in 0 until result.size) {
            assertEquals(
                doubleFn((100 + i).toDouble(), 10.0),
                result.valueAt(i).ensureNumber().asDouble(),
                "function: ${functionName}}. index: ${i}"
            )
        }
    }
}
