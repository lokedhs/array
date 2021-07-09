package array

import array.complex.Complex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GammaTest {
    @Test
    fun gammaInteger() {
        assertEquals(Double.POSITIVE_INFINITY, doubleGamma(0.0))
        assertEquals(1.0, doubleGamma(1.0))
        assertEquals(1.0, doubleGamma(2.0))
        assertEquals(24.0, doubleGamma(5.0))
        assertEquals(120.0, doubleGamma(6.0))
        assertEquals(40320.0, doubleGamma(9.0))
        assertEquals(121645100408832000.0, doubleGamma(20.0))
    }

    @Test
    fun gammaIntegerNeg() {
        assertTrue(doubleGamma(-1.0).isNaN())
        NearDouble(-3.722981, 4).assertNear(doubleGamma(-0.4))
    }

    @Test
    fun gammaDouble() {
        assertRangeVal(Pair(0.918168, 0.918170), doubleGamma(1.2))
        assertRangeVal(Pair(0.887263, 0.887265), doubleGamma(1.4))
        NearDouble(95809.457688, 4).assertNear(doubleGamma(9.4))
        NearDouble(297246107523557247.0, -2).assertNear(doubleGamma(20.3))
    }

    @Test
    fun gammaComplex() {
        NearComplex(Complex(-0.6141502978, -0.6878060058), 4, 4).assertNear(complexGamma(Complex(5.0, 6.0)))
    }

    private fun assertRangeVal(expected: Pair<Double, Double>, result: Double) {
        assertTrue(expected.first <= result && expected.second >= result, "Expected: ${expected}, result: ${result}")
    }
}
