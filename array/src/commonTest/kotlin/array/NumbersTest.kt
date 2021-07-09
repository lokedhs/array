package array

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertFailsWith

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
        assertDoubleWithRange(Pair(3.33332, 3.33334), parseAPLExpression("10÷3"))
    }

    @Test
    fun testAbs() {
        // Plain integers
        assertSimpleNumber(2, parseAPLExpression("|2"))
        assertSimpleNumber(10, parseAPLExpression("|¯10"))
        // Floating point
        assertDoubleWithRange(Pair(10.79999, 10.80001), parseAPLExpression("|10.8"))
        assertDoubleWithRange(Pair(4.89999, 4.90001), parseAPLExpression("|¯4.9"))
        // Complex numbers
        assertDoubleWithRange(Pair(9.219543, 9.219545), parseAPLExpression("|6J7"))
        assertDoubleWithRange(Pair(4.472134, 4.472136), parseAPLExpression("|¯4J2"))
        assertDoubleWithRange(Pair(4.472134, 4.472136), parseAPLExpression("|4J¯2"))
        assertDoubleWithRange(Pair(342.285, 342.287), parseAPLExpression("|¯194J¯282"))
    }

    @Test
    fun testMod() {
        assertSimpleNumber(1, parseAPLExpression("2|3"))
        assertDoubleWithRange(Pair(0.66669, 0.700001), parseAPLExpression("1|1.7"))
        assertSimpleNumber(-1, parseAPLExpression("¯2|11"))
        assertSimpleNumber(0, parseAPLExpression("3|3"))
        assertSimpleNumber(2, parseAPLExpression("100|2"))
        assertSimpleNumber(-5, parseAPLExpression("10000|¯20005"))
        assertSimpleNumber(0, parseAPLExpression("5|0"))
        assertSimpleNumber(0, parseAPLExpression("0|0"))
        assertSimpleNumber(3, parseAPLExpression("0|3"))
    }

    @Test
    fun testNegation() {
        assertSimpleNumber(0, parseAPLExpression("-0"))
        assertSimpleNumber(1, parseAPLExpression("-(1-2)"))
        assertSimpleNumber(-3, parseAPLExpression("-3"))
        assertSimpleNumber(-6, parseAPLExpression("-2+4"))
    }

    @Test
    fun testExponential() {
        assertSimpleNumber(1024, parseAPLExpression("2⋆10"))
        assertDoubleWithRange(Pair(0.0009, 0.0011), parseAPLExpression("10⋆¯3"))
        assertSimpleNumber(0, parseAPLExpression("0⋆10"))
        assertSimpleNumber(1, parseAPLExpression("10⋆0"))
        assertComplexWithRange(Pair(-0.0000001, 0.0000001), Pair(1.732050807, 1.732050809), parseAPLExpression("¯3⋆0.5"))
        assertComplexWithRange(Pair(0.01342669136, 0.01342669138), Pair(0.04132310697, 0.04132310699), parseAPLExpression("¯7.1*¯1.6"))
        assertSimpleNumber(-27, parseAPLExpression("¯3⋆3"))
    }

    @Test
    fun invalidExpressions() {
        assertFailsWith<ParseException> {
            parseAPLExpression("1+")
        }
        assertFailsWith<ParseException> {
            parseAPLExpression("1++")
        }
        assertFailsWith<ParseException> {
            parseAPLExpression("-")
        }
        assertFailsWith<ParseException> {
            parseAPLExpression("1 2 3 4+")
        }
    }

    @Test
    fun mathOperationsWithCharacters() {
        testFailedOpWithChar("+")
        testFailedOpWithChar("-")
        testFailedOpWithChar("×")
        testFailedOpWithChar("÷")
        testFailedOpWithChar("⋆")
        testFailedOpWithChar("|")
    }

    private fun testFailedOpWithChar(name: String) {
        assertFailsWith<APLEvalException> { parseAPLExpression("1${name}@a").collapse() }
        assertFailsWith<APLEvalException> { parseAPLExpression("@a${name}1").collapse() }
        assertFailsWith<APLEvalException> { parseAPLExpression("@a${name}@b").collapse() }
    }

    @Test
    fun functionAliases() {
        val result = parseAPLExpression("2*4")
        assertSimpleNumber(16, result)
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

    @Test
    fun optimisedAdd() {
        runExprTest("(internal:ensureLong 1 2 3 4) + (internal:ensureLong 11 12 13 14)") { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(12, 14, 16, 18), result)
        }
    }

    fun runExprTest(expr: String, withStandardLib: Boolean = false, fn: (APLValue) -> Unit) {
        parseAPLExpression(expr, withStandardLib).let { result ->
            fn(result)
        }
    }

    @Test
    fun positiveIntegerGamma() {
        parseAPLExpression("!1 2 10 11").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1, 2, 3628800, 39916800), result)
        }
    }

    @Test
    fun positiveDoubleGamme() {
        parseAPLExpression("!1.2 1.3 1.4 10.1 2.2 4.2").let { result ->
            assertDimension(dimensionsOfSize(6), result)
            assertArrayContent(
                arrayOf(
                    NearDouble(1.101802491, 4),
                    NearDouble(1.166711905, 4),
                    NearDouble(1.242169345, 4),
                    NearDouble(4593083.59, 2),
                    NearDouble(2.42396548, 4),
                    NearDouble(32.57809605, 4)
                ), result)
        }
    }

    @Test
    fun negativeDoubleGamma() {
        parseAPLExpression("!¯1.2 ¯3.4 ¯5.1 ¯5.2 ¯2.2 ¯2.3").let { result ->
            assertDimension(dimensionsOfSize(6), result)
            assertArrayContent(
                arrayOf(
                    NearDouble(-5.821148569, 4),
                    NearDouble(-1.108029947, 4),
                    NearDouble(-0.3639731139, 4),
                    NearDouble(-0.1640610505, 4),
                    NearDouble(4.850957141, 4),
                    NearDouble(3.328347007, 4)
                ), result)
        }
    }

    @Ignore
    @Test
    fun positiveIntegerBinomial() {
        parseAPLExpression("10!32").let { result ->
            assertSimpleNumber(64512240, result)
        }
    }

    @Ignore
    @Test
    fun positiveDoubleBinominal() {
        parseAPLExpression("10.2!32.2").let { result ->
            assertNearDouble(NearDouble(80760102.76, 2), result)
        }
    }

}
