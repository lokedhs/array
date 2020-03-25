package array

import kotlin.test.Test

class CompareTest : APLTest() {
    @Test
    fun testComparators() {
        testFunction(arrayOf(1, 0, 0, 1, 0, 0), "<")
        testFunction(arrayOf(0, 1, 0, 0, 1, 0), ">")
        testFunction(arrayOf(1, 0, 1, 1, 0, 1), "≤")
        testFunction(arrayOf(0, 1, 1, 0, 1, 1), "≥")
        testFunction(arrayOf(0, 0, 1, 0, 0, 1), "=")
    }

    private fun testFunction(expected: Array<Long>, name: String) {
        assertSimpleNumber(expected[0], parseAPLExpression("1${name}2"))
        assertSimpleNumber(expected[1], parseAPLExpression("2${name}1"))
        assertSimpleNumber(expected[2], parseAPLExpression("2${name}2"))
        assertSimpleNumber(expected[3], parseAPLExpression("0${name}1"))
        assertSimpleNumber(expected[4], parseAPLExpression("1${name}0"))
        assertSimpleNumber(expected[5], parseAPLExpression("0${name}0"))
    }
}
