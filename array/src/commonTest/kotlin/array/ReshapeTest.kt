package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ReshapeTest : APLTest() {
    @Test
    fun simpleReshape() {
        val result = parseAPLExpression("3 4 ⍴ ⍳12")
        assertDimension(dimensionsOfSize(3, 4), result)
        assertArrayContent(arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), result)
    }

    @Test
    fun reshapeDecreaseSize() {
        val result = parseAPLExpression("3 ⍴ 1 2 3 4 5 6 7 8")
        assertDimension(dimensionsOfSize(3), result)
        assertArrayContent(arrayOf(1, 2, 3), result)
    }

    @Test
    fun reshapeIncreaseSize() {
        val result = parseAPLExpression("14 ⍴ 1 2 3 4")
        assertDimension(dimensionsOfSize(14), result)
        assertArrayContent(arrayOf(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2), result)
    }

    @Test
    fun reshapeScalarToSingleDimension() {
        val result = parseAPLExpression("4 ⍴ 1")
        assertDimension(dimensionsOfSize(4), result)
        assertArrayContent(arrayOf(1, 1, 1, 1), result)
    }

    @Test
    fun reshapeScalarToMultiDimension() {
        val result = parseAPLExpression("2 4 ⍴ 1")
        assertDimension(dimensionsOfSize(2, 4), result)
        assertArrayContent(arrayOf(1, 1, 1, 1, 1, 1, 1, 1), result)
    }

    @Test
    fun reshapeCalculatedDimension0() {
        parseAPLExpression("¯1 2 ⍴ ⍳4").let { result ->
            assertDimension(dimensionsOfSize(2, 2), result)
            assertArrayContent(arrayOf(0, 1, 2, 3), result)
        }
    }

    @Test
    fun reshapeCalculatedDimension1() {
        parseAPLExpression("2 ¯1 ⍴ ⍳4").let { result ->
            assertDimension(dimensionsOfSize(2, 2), result)
            assertArrayContent(arrayOf(0, 1, 2, 3), result)
        }
    }

    @Test
    fun reshapeCalculatedFailsWithMismatchedSource() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("2 ¯1 ⍴ ⍳5")
        }
    }

    @Test
    fun reshapeCalculatedFailsWithMultipleUndefinedDimensions() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("¯1 ¯1 ⍴ ⍳4")
        }
    }

    @Test
    fun reshapeSpecialisedLong() {
        parseAPLExpression("2 3 ⍴ 10 11 12 13").let { result ->
            assertDimension(dimensionsOfSize(2, 3), result)
            assertSame(ArrayMemberType.LONG, result.specialisedType)
            assertEquals(10L, result.valueAtLong(0, null))
            assertEquals(11L, result.valueAtLong(1, null))
            assertEquals(12L, result.valueAtLong(2, null))
            assertEquals(13L, result.valueAtLong(3, null))
            assertEquals(10L, result.valueAtLong(4, null))
            assertEquals(11L, result.valueAtLong(5, null))
        }
    }

    @Test
    fun reshapeSpecialisedDouble() {
        parseAPLExpression("2 3 ⍴ 1.1 1.2 1.3 1.4").let { result ->
            assertDimension(dimensionsOfSize(2, 3), result)
            assertSame(ArrayMemberType.DOUBLE, result.specialisedType)
            assertEquals(1.1, result.valueAtDouble(0, null))
            assertEquals(1.2, result.valueAtDouble(1, null))
            assertEquals(1.3, result.valueAtDouble(2, null))
            assertEquals(1.4, result.valueAtDouble(3, null))
            assertEquals(1.1, result.valueAtDouble(4, null))
            assertEquals(1.2, result.valueAtDouble(5, null))
        }
    }

    @Test
    fun reshapeSpecialisedLongSingleValue() {
        parseAPLExpression("2 3 ⍴ 1").let { result ->
            assertDimension(dimensionsOfSize(2, 3), result)
            repeat(6) { i ->
                assertEquals(1, result.valueAtLong(i, null))
            }
        }
    }

    @Test
    fun reshapeSpecialisedDoubleSingleValue() {
        parseAPLExpression("2 3 ⍴ 1.2").let { result ->
            assertDimension(dimensionsOfSize(2, 3), result)
            repeat(6) { i ->
                assertEquals(1.2, result.valueAtDouble(i, null))
            }
        }
    }
}
