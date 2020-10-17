package array

import array.complex.Complex
import kotlin.test.Test
import kotlin.test.assertFailsWith

class LogicTest : APLTest() {
    @Test
    fun andTest() {
        assertSimpleNumber(0, parseAPLExpression("0∧0"))
        assertSimpleNumber(0, parseAPLExpression("0∧1"))
        assertSimpleNumber(0, parseAPLExpression("1∧0"))
        assertSimpleNumber(1, parseAPLExpression("1∧1"))
    }

    @Test
    fun andTestWithArray() {
        parseAPLExpression("1 1 0 0 ∧ 0 1 1 0").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(0, 1, 0, 0), result)
        }
    }

    @Test
    fun leastCommonMultipleIntegers() {
        assertSimpleNumber(0, parseAPLExpression("0∧0"))
        assertSimpleNumber(6, parseAPLExpression("2∧3"))
        assertSimpleNumber(6, parseAPLExpression("2∧3"))
        assertSimpleNumber(12, parseAPLExpression("4∧6"))
        assertSimpleNumber(-6, parseAPLExpression("2∧¯3"))
        assertSimpleNumber(-6, parseAPLExpression("¯2∧3"))
        assertSimpleNumber(6, parseAPLExpression("¯2∧¯3"))
    }

    @Test
    fun leastCommonMultipleComplex() {
        assertSimpleComplex(Complex(123.0, 192.0), parseAPLExpression("6J21∧9J30"))
        assertSimpleComplex(Complex(38.0, 43.0), parseAPLExpression("5J8∧1J6"))
        assertSimpleComplex(Complex(-495.0, 312.0), parseAPLExpression("9J30∧5J18"))
        assertSimpleComplex(Complex(-5.0, -14.0), parseAPLExpression("2J3∧4J1"))
        assertSimpleComplex(Complex(25.0, -19.0), parseAPLExpression("5J3∧5J2"))
        assertSimpleComplex(Complex(31.0, -5.0), parseAPLExpression("¯5J3∧5J2"))
        assertSimpleComplex(Complex(-159.0, -15.0), parseAPLExpression("9J30∧1J5"))
    }

    @Test
    fun orTest() {
        assertSimpleNumber(0, parseAPLExpression("0∨0"))
        assertSimpleNumber(1, parseAPLExpression("0∨1"))
        assertSimpleNumber(1, parseAPLExpression("1∨0"))
        assertSimpleNumber(1, parseAPLExpression("1∨1"))
    }

    @Test
    fun greatestCommonDenominatorComplex() {
        assertSimpleComplex(Complex(123.0, 192.0), parseAPLExpression("6J21∧9J30"))        
    }

    @Test
    fun orTestWithArray() {
        parseAPLExpression("1 1 0 0 ∨ 0 1 1 0").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1, 1, 1, 0), result)
        }
    }

    @Test
    fun testNotWorking() {
        parseAPLExpression("~0 1").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(1, 0), result)
        }
    }

    @Test
    fun testNotFailing() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("~10")
        }
    }
}
