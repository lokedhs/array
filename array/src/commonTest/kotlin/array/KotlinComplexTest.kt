package array

import array.complex.Complex
import array.complex.minus
import array.complex.plus
import array.complex.times
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinComplexTest {
    @Test
    fun addSimple() {
        val result = Complex(1.0, 2.0) + Complex(100.0, 200.0)
        assertEquals(Complex(101.0, 202.0), result)
    }

    @Test
    fun addWithDouble() {
        val result1 = Complex(1.0, 2.0) + 100.0
        assertEquals(Complex(101.0, 2.0), result1)
        val result2 = 100.0 + Complex(1.0, 2.0)
        assertEquals(Complex(101.0, 2.0), result2)
    }

    @Test
    fun addWithZero() {
        val result = Complex(1.0, 2.0) + Complex.ZERO
        assertEquals(Complex(1.0, 2.0), result)
    }

    @Test
    fun multSimple() {
        val result = Complex(2.0, 3.0) * Complex(10.0, 11.0)
        assertEquals(Complex(-13.0, 52.0), result)
    }

    @Test
    fun mulWithDouble() {
        val result1 = Complex(2.0, 3.0) * 3.0
        assertEquals(Complex(6.0, 9.0), result1)
        val result2 = 3.0 * Complex(2.0, 3.0)
        assertEquals(Complex(6.0, 9.0), result2)
    }

    @Test
    fun subSimple() {
        val result = Complex(20.0, 21.0) - Complex(3.0, 1.0)
        assertEquals(Complex(17.0, 20.0), result)
    }

    @Test
    fun subWithDouble() {
        val result1 = Complex(3.0, 4.0) - 2.0
        assertEquals(Complex(1.0, 4.0), result1)
        val result2 = 2.0 - Complex(3.0, 4.0)
        assertEquals(Complex(-1.0, -4.0), result2)
    }

    @Test
    fun exponents() {
        //#C(-2.044201815541423d0 -3.0781516382992966d0)
        assertComplex(Pair(-2.044202, -2.0442), Pair(-3.0781523, -3.07815162), Complex(1.0, 2.0).pow(Complex(3.0, 1.0)))
        //#C(-27.436381991606034d0 -19.7893103650107d0)
        assertComplex(Pair(-27.436385, -27.43638), Pair(-19.789329, -19.789309), Complex(1.0, -2.0).pow(Complex(3.0, 1.0)))
        //#C(47.30424589281338d0 71.23056093896999d0)
        assertComplex(Pair(47.304244, 47.304246), Pair(71.230559, 71.230561), Complex(-1.0, -2.0).pow(Complex(3.0, 1.0)))
        //#C(5.7203802430975985d0 -16.116346655526538d0)
        assertComplex(Pair(5.720379, 5.720381), Pair(-16.116347, -16.116345), Complex(-1.0, -2.0).pow(Complex(1.0, 1.0)))
        //#C(23.628213273116497d0 13.608086987820702d0)
        assertComplex(Pair(23.628212, 23.628214), Pair(13.608085, 13.608087), Complex(5.0, 11.0).pow(Complex(5.0, 8.0)))
        assertComplex(Pair(0.999999, 1.000001), Pair(-0.000001, 0.000001), Complex(9.0, 10.0).pow(Complex(0.0, 0.0)))
    }

    private fun assertComplex(real: Pair<Double, Double>, imaginary: Pair<Double, Double>, result: Complex) {
        val message = "expected: ${real} ${imaginary}, actual: ${result}"
        assertTrue(real.first <= result.real && real.second >= result.real, message)
        assertTrue(imaginary.first <= result.imaginary && imaginary.second >= result.imaginary, message)
    }
}
