package array

import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScalarTest : APLTest() {
    @Test
    fun testReshape() {
        val result = parseAPLExpression("3 4 ⍴ ⍳100")
        assertEquals(result.size(), 12)
        assertTrue(result.dimensions().compare(dimensionsOfSize(3, 4)))
        for (i in 0 until 12) {
            assertEquals(result.valueAt(i).ensureNumber().asLong(), i.toLong())
        }
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
    fun testCompareEquals() {
        runScalarTest("=") { a, b -> if (a == b) 1.0 else 0.0 }
    }

    @Test
    fun testCompareNotEquals() {
        runScalarTest("≠") { a, b -> if (a != b) 1.0 else 0.0 }
    }

    private fun runScalarTest(functionName: String, doubleFn: (Double, Double) -> Double) {
        runScalarTestSD(functionName, doubleFn)
        runScalarTestDS(functionName, doubleFn)
    }

    private fun runScalarTestSD(functionName: String, doubleFn: (Double, Double) -> Double) {
        val result = parseAPLExpression("100 ${functionName} 100+3 4 ⍴ ⍳100")
        for (i in 0 until 12) {
            assertEquals(result.valueAt(i).ensureNumber().asDouble(), doubleFn(100.0, (100 + i).toDouble()))
        }
    }

    private fun runScalarTestDS(functionName: String, doubleFn: (Double, Double) -> Double) {
        val result = parseAPLExpression("(100+3 4 ⍴ ⍳100) ${functionName} 10")
        for (i in 0 until 12) {
            assertEquals(result.valueAt(i).ensureNumber().asDouble(), doubleFn((100 + i).toDouble(), 10.0))
        }
    }
}
