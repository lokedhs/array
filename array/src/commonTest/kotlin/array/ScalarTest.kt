package array

import kotlin.math.pow
import kotlin.math.sign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScalarTest : APLTest() {
    @Test
    fun testReshape() {
        val result = parseAPLExpression("3 4 ⍴ ⍳100")
        assertDimension(dimensionsOfSize(3,4), result)
        assertArrayContent(arrayOf(0,1,2,3,4,5,6,7,8,9,10,11), result)
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

    private fun runScalarTest1Arg(functionName: String, doubleFn: (Double) -> Double) {
        val result = parseAPLExpression("${functionName} ¯4.0+⍳10")
        assertDimension(dimensionsOfSize(10), result)
        for (i in 0 until result.dimensions()[0]) {
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
        for (i in 0 until result.size()) {
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
        for (i in 0 until result.size()) {
            assertEquals(
                doubleFn((100 + i).toDouble(), 10.0),
                result.valueAt(i).ensureNumber().asDouble(),
                "function: ${functionName}}. index: ${i}"
            )
        }
    }
}
