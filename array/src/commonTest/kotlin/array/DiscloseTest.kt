package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiscloseTest : APLTest() {
    @Test
    fun discloseArrayTest() {
        val result = parseAPLExpression("⊃1 2 3 4")
        assertFalse(result.isScalar())
        assertDimension(dimensionsOfSize(4), result)
        assertArrayContent(arrayOf(1, 2, 3, 4), result)
    }

    @Test
    fun discloseNumberTest() {
        val result = parseAPLExpression("⊃6")
        assertTrue(result.isScalar())
        assertDimension(emptyDimensions(), result)
        assertEquals(1, result.size())
        val number = result.ensureNumber().asLong()
        assertEquals(6L, number)
    }
}
