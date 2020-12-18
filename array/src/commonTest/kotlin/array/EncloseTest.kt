package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
        parseAPLExpression("io:print ⊂1 2 3 4").let { result ->
            assertTrue(result.isScalar())
            assertDimension(emptyDimensions(), result)
            assertEquals(1, result.size)
            val v = result.valueAt(0)
            assertDimension(dimensionsOfSize(4), v)
            assertArrayContent(arrayOf(1, 2, 3, 4), v)
        }
    }

    @Suppress("UNUSED_CHANGED_VALUE")
    @Test
    fun encloseWithAxis0() {
        parseAPLExpression("⊂[0] 2 3 2 ⍴ ⍳1000").let { result ->
            assertDimension(dimensionsOfSize(3, 2), result)

            fun assertValue(index: Int, vararg args: Int) {
                result.valueAt(index).let { v ->
                    assertDimension(dimensionsOfSize(v.size), v)
                    assertArrayContent(args.toTypedArray(), v)
                }
            }

            var i = 0
            assertValue(i++, 0, 6)
            assertValue(i++, 1, 7)
            assertValue(i++, 2, 8)
            assertValue(i++, 3, 9)
            assertValue(i++, 4, 10)
            assertValue(i++, 5, 11)
        }
    }

    @Suppress("UNUSED_CHANGED_VALUE")
    @Test
    fun encloseWithAxis1() {
        parseAPLExpression("⊂[1] 2 3 2 ⍴ 300+⍳1000").let { result ->
            assertDimension(dimensionsOfSize(2, 2), result)

            fun assertValue(index: Int, vararg args: Int) {
                result.valueAt(index).let { v ->
                    assertDimension(dimensionsOfSize(v.size), v)
                    assertArrayContent(args.toTypedArray(), v)
                }
            }

            var i = 0
            assertValue(i++, 300, 302, 304)
            assertValue(i++, 301, 303, 305)
            assertValue(i++, 306, 308, 310)
            assertValue(i++, 307, 309, 311)
        }
    }

    @Suppress("UNUSED_CHANGED_VALUE")
    @Test
    fun encloseWithAxis2() {
        parseAPLExpression("⊂[2] 2 3 2 ⍴ 700+⍳1000").let { result ->
            assertDimension(dimensionsOfSize(2, 3), result)

            fun assertValue(index: Int, vararg args: Int) {
                result.valueAt(index).let { v ->
                    assertDimension(dimensionsOfSize(v.size), v)
                    assertArrayContent(args.toTypedArray(), v)
                }
            }

            var i = 0
            assertValue(i++, 700, 701)
            assertValue(i++, 702, 703)
            assertValue(i++, 704, 705)
            assertValue(i++, 706, 707)
            assertValue(i++, 708, 709)
            assertValue(i++, 710, 711)
        }
    }

    @Test
    fun illegalAxis() {
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("⊂[3] 2 3 2 ⍴ ⍳1000")
        }
    }

    @Test
    fun illegalAxis2() {
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("⊂[0] 1")
        }
    }
}
