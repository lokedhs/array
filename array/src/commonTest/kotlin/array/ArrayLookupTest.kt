package array

import kotlin.test.Test
import kotlin.test.assertFailsWith

class ArrayLookupTest : APLTest() {
    @Test
    fun testSimpleArrayLookup() {
        val result = parseAPLExpression("2 ⌷ 1 2 3 4")
        assertSimpleNumber(3, result)
    }

    @Test
    fun testSimpleArrayLookupFromFunctionInvocation() {
        val result = parseAPLExpression("2 ⌷ 10 + 10 11 12 13")
        assertSimpleNumber(22, result)
    }

    @Test
    fun testIllegalIndex() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("3 ⌷ 1 2 3").collapse()
        }
    }

    @Test
    fun illegalDimension() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("2 3 ⌷ 1 2 3 4").collapse()
        }
    }
}
