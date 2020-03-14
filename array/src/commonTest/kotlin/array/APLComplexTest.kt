package array

import array.complex.Complex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class APLComplexTest : APLTest() {
    @Test
    fun addComplex() {
        val result = parseAPLExpression("2J3+3J4")
        assertEquals(Complex(5.0, 7.0), result.ensureNumber().asComplex())
    }

    @Test
    fun complexConjugate() {
        val result = parseAPLExpression("+5j¯2")
        assertEquals(Complex(5.0, 2.0), result.ensureNumber().asComplex())
    }

    @Test
    fun exptTest() {
        parseAPLExpression("⋆1J2").let { result ->
            //#C(-1.1312043837568135d0 2.4717266720048188d0)
            assertAPLComplex(Pair(-1.131205, -1.131204), Pair(2.471725, 2.471727), result)
        }
        parseAPLExpression("2j3⋆2j2").let { result ->
            //#C(-0.32932274563575575d0 -1.7909296731087332d0)
            assertAPLComplex(Pair(-0.329323, -0.329321), Pair(-1.790930, -1.790928), result)
        }
    }

    private fun assertAPLComplex(real: Pair<Double, Double>, imaginary: Pair<Double, Double>, result: APLValue) {
        val complex = result.ensureNumber().asComplex()
        val message = "expected: ${real} ${imaginary}, actual: ${complex}"
        assertTrue(real.first <= complex.real && real.second >= complex.real, message)
        assertTrue(imaginary.first <= complex.imaginary && imaginary.second >= complex.imaginary, message)
    }
}
