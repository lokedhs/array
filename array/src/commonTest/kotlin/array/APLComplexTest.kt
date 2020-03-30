package array

import array.complex.Complex
import kotlin.test.Test
import kotlin.test.assertEquals

class APLComplexTest : APLTest() {
    @Test
    fun addComplex() {
        val result = parseAPLExpression("2J3+3J4")
        assertEquals(Complex(5.0, 7.0), result.ensureNumber().asComplex())
    }

    @Test
    fun addIntAndComplex() {
        val result = parseAPLExpression("6+3J4")
        assertEquals(Complex(9.0, 4.0), result.ensureNumber().asComplex())
    }

    @Test
    fun addComplexAndInt() {
        val result = parseAPLExpression("3J4+6")
        assertEquals(Complex(9.0, 4.0), result.ensureNumber().asComplex())
    }

    @Test
    fun convertIntToComplex() {
        val result = parseAPLExpression("1+3")
        assertEquals(Complex(4.0, 0.0), result.ensureNumber().asComplex())
    }

    @Test
    fun convertFloatToComplex() {
        val result = parseAPLExpression("1.0+3.0")
        assertEquals(Complex(4.0, 0.0), result.ensureNumber().asComplex())
    }

    @Test
    fun complexConjugate() {
        val result = parseAPLExpression("+5j¯2")
        assertEquals(Complex(5.0, 2.0), result.ensureNumber().asComplex())
    }

    @Test
    fun convertComplexToIntSuccess() {
        val result = parseAPLExpression("2J3-1J3")
        assertEquals(1, result.ensureNumber().asInt())
    }

    @Test
    fun convertComplexToDoubleSuccess() {
        val result = parseAPLExpression("2J3-1J3")
        assertEquals(1.0, result.ensureNumber().asDouble())
    }

    @Test
    fun negateComplexTest() {
        val result = parseAPLExpression("-2J3")
        assertEquals(Complex(-2.0, -3.0), result.ensureNumber().asComplex())
    }

    @Test
    fun exptTest() {
        parseAPLExpression("⋆1J2").let { result ->
            //#C(-1.1312043837568135d0 2.4717266720048188d0)
            assertComplexWithRange(Pair(-1.131205, -1.131204), Pair(2.471725, 2.471727), result)
        }
        parseAPLExpression("2j3⋆2j2").let { result ->
            //#C(-0.32932274563575575d0 -1.7909296731087332d0)
            assertComplexWithRange(Pair(-0.329323, -0.329321), Pair(-1.790930, -1.790928), result)
        }
    }
}
