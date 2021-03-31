package array

import kotlin.test.Test
import kotlin.test.assertEquals

class SpecialisedArrayTest : APLTest() {
    @Test
    fun readSpecialisedArrayLong0() {
        parseAPLExpression("1 2 3 4", collapse = false).let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1, 2, 3, 4), result)
            assertEquals(ArrayMemberType.LONG, result.specialisedType)
        }
    }

    @Test
    fun readSpecialisedArrayLong1() {
        parseAPLExpression("1 2 3 4", collapse = true).let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1, 2, 3, 4), result)
            assertEquals(ArrayMemberType.LONG, result.specialisedType)
        }
    }

    @Test
    fun readSpecialisedArrayDouble0() {
        parseAPLExpression("1.1 2.1 3.1 4.1", collapse = false).let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1.1, 2.1, 3.1, 4.1), result)
            assertEquals(ArrayMemberType.DOUBLE, result.specialisedType)
        }
    }

    @Test
    fun readSpecialisedArrayDouble1() {
        parseAPLExpression("1.1 2.1 3.1 4.1", collapse = true).let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1.1, 2.1, 3.1, 4.1), result)
            assertEquals(ArrayMemberType.DOUBLE, result.specialisedType)
        }
    }

    @Test
    fun readSpecialisedArrayDoubleWithZeroDecimals0() {
        parseAPLExpression("1.0 2.0 3.0 4.0", collapse = false).let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1.0, 2.0, 3.0, 4.0), result)
            assertEquals(ArrayMemberType.DOUBLE, result.specialisedType)
        }
    }

    @Test
    fun readSpecialisedArrayDoubleWithZeroDecimals1() {
        parseAPLExpression("1.0 2.0 3.0 4.0", collapse = true).let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1.0, 2.0, 3.0, 4.0), result)
            assertEquals(ArrayMemberType.DOUBLE, result.specialisedType)
        }
    }

    @Test
    fun readSpecialisedArrayMixedLongAndDouble0() {
        parseAPLExpression("1 2 3 4.5", collapse = false).let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1, 2, 3, 4.5), result)
            assertEquals(ArrayMemberType.GENERIC, result.specialisedType)
        }
    }

    @Test
    fun readSpecialisedArrayMixedLongAndDouble1() {
        parseAPLExpression("1 2 3 4.5", collapse = true).let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1, 2, 3, 4.5), result)
            assertEquals(ArrayMemberType.GENERIC, result.specialisedType)
        }
    }

    @Test
    fun iotaIsSpecialised0() {
        parseAPLExpression("⍳3", collapse = false).let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(0, 1, 2), result)
            assertEquals(ArrayMemberType.LONG, result.specialisedType)
        }
    }

    @Test
    fun iotaIsSpecialised1() {
        parseAPLExpression("⍳3", collapse = true).let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(0, 1, 2), result)
            assertEquals(ArrayMemberType.LONG, result.specialisedType)
        }
    }

    @Test
    fun iotaWithAdditionScalarLeft0() {
        parseAPLExpression("1000+⍳3", collapse = false).let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(1000, 1001, 1002), result)
            assertEquals(ArrayMemberType.LONG, result.specialisedType)
        }
    }

    @Test
    fun iotaWithAdditionScalarLeft1() {
        parseAPLExpression("1000+⍳3", collapse = true).let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(1000, 1001, 1002), result)
            assertEquals(ArrayMemberType.LONG, result.specialisedType)
        }
    }

    @Test
    fun iotaWithAdditionScalarRight0() {
        parseAPLExpression("(⍳3)+1000", collapse = false).let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(1000, 1001, 1002), result)
            assertEquals(ArrayMemberType.LONG, result.specialisedType)
        }
    }

    @Test
    fun iotaWithAdditionScalarRight1() {
        parseAPLExpression("(⍳3)+1000").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(1000, 1001, 1002), result)
            assertEquals(ArrayMemberType.LONG, result.specialisedType)
        }
    }

    @Test
    fun iotaWithAdditionPlusArray0() {
        parseAPLExpression("(⍳3) + 10 11 12", collapse = false).let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(10, 12, 14), result)
            assertEquals(ArrayMemberType.LONG, result.specialisedType)
        }
    }

    @Test
    fun iotaWithAdditionPlusArray1() {
        parseAPLExpression("(⍳3) + 10 11 12").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(10, 12, 14), result)
            assertEquals(ArrayMemberType.LONG, result.specialisedType)
        }
    }

    @Test
    fun transposeHorizontalLong() {
        parseAPLExpression("⌽ 1 2 3 4 5 6", collapse = false).let { result ->
            assertDimension(dimensionsOfSize(6), result)
            assertArrayContent(arrayOf(6, 5, 4, 3, 2, 1), result)
            assertEquals(ArrayMemberType.LONG, result.specialisedType)
        }
    }

    @Test
    fun transposeHorizontalDouble() {
        parseAPLExpression("⌽ 1.1 2.1 3.1 4.1 5.1 6.1", collapse = false).let { result ->
            assertDimension(dimensionsOfSize(6), result)
            assertArrayContent(arrayOf(6.1, 5.1, 4.1, 3.1, 2.1, 1.1), result)
            assertEquals(ArrayMemberType.DOUBLE, result.specialisedType)
        }
    }

    @Test
    fun transposeVerticalLong() {
        parseAPLExpression("⊖ 3 4 ⍴ ⍳12", collapse = false).let { result ->
            assertDimension(dimensionsOfSize(3, 4), result)
            assertArrayContent(arrayOf(8, 9, 10, 11, 4, 5, 6, 7, 0, 1, 2, 3), result)
            assertEquals(ArrayMemberType.LONG, result.specialisedType)
        }
    }

    @Test
    fun transposeVerticalDouble() {
        parseAPLExpression("⊖ 3 4 ⍴ internal:ensureDouble 0.7+⍳12", collapse = false).let { result ->
            assertDimension(dimensionsOfSize(3, 4), result)
            assertArrayContent(arrayOf(8.7, 9.7, 10.7, 11.7, 4.7, 5.7, 6.7, 7.7, 0.7, 1.7, 2.7, 3.7), result)
            assertEquals(ArrayMemberType.DOUBLE, result.specialisedType)
        }
    }

    @Test
    fun transposeArrayLong() {
        parseAPLExpression("⍉ 2 3 ⍴ ⍳12").let { result ->
            assertDimension(dimensionsOfSize(3, 2), result)
            assertArrayContent(arrayOf(0, 3, 1, 4, 2, 5), result)
            assertEquals(ArrayMemberType.LONG, result.specialisedType)
        }
    }

    @Test
    fun transposeArrayDouble() {
        parseAPLExpression("⍉ 2 3 ⍴ internal:ensureDouble 0.1+⍳12").let { result ->
            assertDimension(dimensionsOfSize(3, 2), result)
            assertArrayContent(arrayOf(0.1, 3.1, 1.1, 4.1, 2.1, 5.1), result)
            assertEquals(ArrayMemberType.DOUBLE, result.specialisedType)
        }
    }
}
