package array

import array.complex.Complex
import array.complex.minus
import array.complex.plus
import array.complex.times
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
