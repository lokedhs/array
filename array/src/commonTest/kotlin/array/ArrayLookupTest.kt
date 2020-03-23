package array

import kotlin.test.Test

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
}
