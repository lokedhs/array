package array

import kotlin.test.Test
import kotlin.test.assertFailsWith

class IotaTest : APLTest() {
    @Test
    fun testIotaOneArg() {
        val result = parseAPLExpression("⍳10")
        assertDimension(dimensionsOfSize(10), result)
        assertArrayContent(arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), result)
    }

    @Test
    fun testIotaSingleValue() {
        val result = parseAPLExpression("⍳1")
        assertDimension(dimensionsOfSize(1), result)
        assertArrayContent(arrayOf(0), result)
    }

    /**
     * This test verifies that the iota function can accept an expression as
     * argument and not just plain numbers.
     */
    @Test
    fun testIotaWithExpressionArg() {
        val result = parseAPLExpression("⍳1+10")
        assertDimension(dimensionsOfSize(11), result)
        assertArrayContent(arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10), result)
    }

    @Test
    fun failWithComplexArgument() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍳2J1").collapse()
        }
    }

    @Test
    fun failWithCharArgument() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍳@p").collapse()
        }
    }

    @Test
    fun iota2DArray() {
        fun assertNumbers(a: Long, b: Long, value: APLValue) {
            assertDimension(dimensionsOfSize(2), value)
            assertSimpleNumber(a, value.valueAt(0))
            assertSimpleNumber(b, value.valueAt(1))
        }

        parseAPLExpression("⍳ 4 5").let { result ->
            assertDimension(dimensionsOfSize(4, 5), result)
            assertNumbers(0, 0, result.valueAt(0))
            assertNumbers(0, 1, result.valueAt(1))
            assertNumbers(0, 2, result.valueAt(2))
            assertNumbers(0, 3, result.valueAt(3))
            assertNumbers(0, 4, result.valueAt(4))
            assertNumbers(1, 0, result.valueAt(5))
            assertNumbers(1, 1, result.valueAt(6))
            assertNumbers(1, 2, result.valueAt(7))
            assertNumbers(1, 3, result.valueAt(8))
            assertNumbers(1, 4, result.valueAt(9))
            assertNumbers(2, 0, result.valueAt(10))
            assertNumbers(2, 1, result.valueAt(11))
            assertNumbers(2, 2, result.valueAt(12))
            assertNumbers(2, 3, result.valueAt(13))
            assertNumbers(2, 4, result.valueAt(14))
            assertNumbers(3, 0, result.valueAt(15))
            assertNumbers(3, 1, result.valueAt(16))
            assertNumbers(3, 2, result.valueAt(17))
            assertNumbers(3, 3, result.valueAt(18))
            assertNumbers(3, 4, result.valueAt(19))
        }
    }

    @Test
    fun iota3DArray() {
        fun assertNumbers(a: Long, b: Long, c: Long, value: APLValue) {
            assertDimension(dimensionsOfSize(3), value)
            assertSimpleNumber(a, value.valueAt(0))
            assertSimpleNumber(b, value.valueAt(1))
            assertSimpleNumber(c, value.valueAt(2))
        }

        parseAPLExpression("⍳ 2 3 2").let { result ->
            assertNumbers(0, 0, 0, result.valueAt(0))
            assertNumbers(0, 0, 1, result.valueAt(1))
            assertNumbers(0, 1, 0, result.valueAt(2))
            assertNumbers(0, 1, 1, result.valueAt(3))
            assertNumbers(0, 2, 0, result.valueAt(4))
            assertNumbers(0, 2, 1, result.valueAt(5))
            assertNumbers(1, 0, 0, result.valueAt(6))
            assertNumbers(1, 0, 1, result.valueAt(7))
            assertNumbers(1, 1, 0, result.valueAt(8))
            assertNumbers(1, 1, 1, result.valueAt(9))
            assertNumbers(1, 2, 0, result.valueAt(10))
            assertNumbers(1, 2, 1, result.valueAt(11))
        }
    }

    @Test
    fun singleElementDimension() {
        parseAPLExpression("⍳,9").let { result ->
            fun assertElement(expected: Long, i: Int) {
                val v = result.valueAt(i)
                assertDimension(dimensionsOfSize(1), v)
                assertSimpleNumber(expected, v.valueAt(0))
            }
            assertDimension(dimensionsOfSize(9), result)
            repeat(9) { index ->
                assertElement(index.toLong(), index)
            }
        }
    }

    @Test
    fun iotaEmpty() {
        parseAPLExpression("⍳0").let { result ->
            assertAPLNull(result)
        }
    }

    @Test
    fun failWith2DArrayArgument() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍳ 2 2 ⍴ 1 2 3 4")
        }
    }

    @Test
    fun iotaWithNullArg() {
        parseAPLExpression("⍳⍬").let { result ->
            assertDimension(emptyDimensions(), result)
            val inner = result.valueAt(0)
            assertAPLNull(inner)
        }
    }
}
