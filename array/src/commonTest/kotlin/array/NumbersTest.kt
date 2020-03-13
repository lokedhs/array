package array

import kotlin.test.Test

class NumbersTest : APLTest() {
    @Test
    fun testMathsOperations() {
        assertMathsOperation({ a, b -> a + b }, "+")
        assertMathsOperation({ a, b -> a - b }, "-")
        assertMathsOperation({ a, b -> a * b }, "×")
    }

    @Test
    fun testDivision() {
        assertSimpleNumber(0, parseAPLExpression("1÷0"))
        assertSimpleNumber(0, parseAPLExpression("100÷0"))
        assertSimpleNumber(0, parseAPLExpression("¯100÷0"))
        assertSimpleNumber(0, parseAPLExpression("12.2÷0"))
        assertSimpleNumber(0, parseAPLExpression("2÷0.0"))
        assertSimpleNumber(0, parseAPLExpression("2.0÷0.0"))
        assertSimpleNumber(2, parseAPLExpression("4÷2"))
        assertSimpleNumber(20, parseAPLExpression("40÷2"))
        assertSimpleDouble(Pair(3.33332, 3.33334), parseAPLExpression("10÷3"))
    }

    private fun assertMathsOperation(op: (Long, Long) -> Long, name: String) {
        val args: Array<Long> =
            arrayOf(0, 1, -1, 2, 3, 10, 100, 123456, -12345, Int.MAX_VALUE.toLong(), Int.MIN_VALUE.toLong(), Long.MAX_VALUE, Long.MIN_VALUE)
        args.forEach { left ->
            args.forEach { right ->
                val expr = "${formatLongAsAPL(left)}${name}${formatLongAsAPL(right)}"
                val result = parseAPLExpression(expr)
                val expect = op(left, right)
                assertSimpleNumber(expect, result, expr)
            }
        }
    }

    private fun formatLongAsAPL(value: Long): String {
        return if (value < 0) {
            // This is a hack to deal with Long.MIN_VALUE
            "¯${value.toString().substring(1)}"
        } else {
            value.toString()
        }
    }
}
