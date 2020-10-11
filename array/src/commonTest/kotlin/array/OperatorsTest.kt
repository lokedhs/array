package array

import kotlin.test.Test
import kotlin.test.assertTrue

class OperatorsTest : APLTest() {
    @Test
    fun commuteTwoArgs() {
        val result = parseAPLExpression("4÷⍨160")
        assertSimpleNumber(40, result)
    }

    @Test
    fun commuteOneArg() {
        val result = parseAPLExpression("+⍨3")
        assertSimpleNumber(6, result)
    }

    @Test
    fun commuteWithArrays() {
        val result = parseAPLExpression("2÷⍨8×⍳10")
        assertDimension(dimensionsOfSize(10), result)
        assertArrayContent(arrayOf(0, 4, 8, 12, 16, 20, 24, 28, 32, 36), result)
    }

    @Test
    fun multiOperators() {
        val result = parseAPLExpression("+⌺⍨1 2 3")
        assertDimension(dimensionsOfSize(3, 3), result)
        assertArrayContent(arrayOf(2, 3, 4, 3, 4, 5, 4, 5, 6), result)
    }

    @Test
    fun reduceWithoutAxisOnFunction() {
        parseAPLExpression(",/ (2 3 ⍴ 10+⍳6) (2 3 ⍴ 20+⍳6) (2 3 ⍴ 30+⍳6)").let { result ->
            assertTrue(result.isScalar())
            assertTrue(result !is APLSingleValue)
            val inner = result.valueAt(0)
            assertDimension(dimensionsOfSize(2, 9), inner)
            assertArrayContent(arrayOf(10, 11, 12, 20, 21, 22, 30, 31, 32, 13, 14, 15, 23, 24, 25, 33, 34, 35), inner)
        }
    }

    @Test
    fun reduceWithFunctionAxis() {
        parseAPLExpression(",[0]/ (2 3 ⍴ 10+⍳6) (2 3 ⍴ 20+⍳6) (2 3 ⍴ 30+⍳6)").let { result ->
            assertTrue(result.isScalar())
            assertTrue(result !is APLSingleValue)
            val inner = result.valueAt(0)
            assertDimension(dimensionsOfSize(6, 3), inner)
            assertArrayContent(arrayOf(10, 11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 25, 30, 31, 32, 33, 34, 35), inner)
        }
    }

    @Test
    fun reduceWithAxis() {
        parseAPLExpression(
            ",/[0] 3 3 ⍴ (2 3 ⍴ 10+⍳6) (2 3 ⍴ 20+⍳6) (2 3 ⍴ 30+⍳6) " +
                    "(2 3 ⍴ 40+⍳6) (2 3 ⍴ 50+⍳6) (2 3 ⍴ 60+⍳6) (2 3 ⍴ 70+⍳6) (2 3 ⍴ 80+⍳6) (2 3 ⍴ 90+⍳6)").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            fun verifySingle(n: Int, expected: Array<Int>) {
                val inner = result.valueAt(n)
                assertDimension(dimensionsOfSize(2, 9), inner)
                assertArrayContent(expected, inner)
            }
            verifySingle(0, arrayOf(10, 11, 12, 40, 41, 42, 70, 71, 72, 13, 14, 15, 43, 44, 45, 73, 74, 75))
            verifySingle(1, arrayOf(20, 21, 22, 50, 51, 52, 80, 81, 82, 23, 24, 25, 53, 54, 55, 83, 84, 85))
            verifySingle(2, arrayOf(30, 31, 32, 60, 61, 62, 90, 91, 92, 33, 34, 35, 63, 64, 65, 93, 94, 95))
        }
    }
}
