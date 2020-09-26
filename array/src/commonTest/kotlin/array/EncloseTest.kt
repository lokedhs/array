package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EncloseTest : APLTest() {
    @Test
    fun encloseArrayTest() {
        val result = parseAPLExpression("⊂1 2 3 4")
        assertTrue(result.isScalar())
        assertDimension(emptyDimensions(), result)
        assertEquals(1, result.size)
        val v = result.valueAt(0)
        assertDimension(dimensionsOfSize(4), v)
        assertArrayContent(arrayOf(1, 2, 3, 4), v)
    }

    @Test
    fun encloseNumberTest() {
        val result = parseAPLExpression("⊂6")
        assertTrue(result.isScalar())
        assertDimension(emptyDimensions(), result)
        assertEquals(1, result.size)
        val number = result.ensureNumber().asLong()
        assertEquals(6L, number)
    }

    @Test
    fun printEnclosedValue() {
        parseAPLExpression("print ⊂1 2 3 4").let { result ->
            assertTrue(result.isScalar())
            assertDimension(emptyDimensions(), result)
            assertEquals(1, result.size)
            val v = result.valueAt(0)
            assertDimension(dimensionsOfSize(4), v)
            assertArrayContent(arrayOf(1, 2, 3, 4), v)
        }
    }
}
