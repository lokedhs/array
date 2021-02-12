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

    @Suppress("UNUSED_CHANGED_VALUE")
    @Test
    fun encloseWithAxis3() {
        parseAPLExpression("⊂[,1] 2 3 2 ⍴ 300+⍳1000").let { result ->
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

    //////////////////////////////////////////////////
    // Enclose with axis
    //////////////////////////////////////////////////

    @Test
    fun encloseWithMultiArgAxis0() {
        parseAPLExpression("⊂[0 1] 2 2 2 ⍴ ⍳100").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            result.valueAt(0).let { v ->
                assertDimension(dimensionsOfSize(2, 2), v)
                assertArrayContent(arrayOf(0, 2, 4, 6), v)
            }
            result.valueAt(1).let { v ->
                assertDimension(dimensionsOfSize(2, 2), v)
                assertArrayContent(arrayOf(1, 3, 5, 7), v)
            }
        }
    }

    @Test
    fun encloseWithMultiArgAxis1() {
        parseAPLExpression("⊂[1 0] 2 2 2 ⍴ ⍳100").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            result.valueAt(0).let { v ->
                assertDimension(dimensionsOfSize(2, 2), v)
                assertArrayContent(arrayOf(0, 4, 2, 6), v)
            }
            result.valueAt(1).let { v ->
                assertDimension(dimensionsOfSize(2, 2), v)
                assertArrayContent(arrayOf(1, 5, 3, 7), v)
            }
        }
    }

    @Test
    fun encloseWithMultiArgAxis2() {
        parseAPLExpression("⊂[2 0] 2 2 2 ⍴ ⍳100").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            result.valueAt(0).let { v ->
                assertDimension(dimensionsOfSize(2, 2), v)
                assertArrayContent(arrayOf(0, 4, 1, 5), v)
            }
            result.valueAt(1).let { v ->
                assertDimension(dimensionsOfSize(2, 2), v)
                assertArrayContent(arrayOf(2, 6, 3, 7), v)
            }
        }
    }

    @Test
    fun encloseWithMultiArgAxis3() {
        parseAPLExpression("⊂[0 1 2] 2 2 2 ⍴ ⍳100").let { result ->
            assertDimension(emptyDimensions(), result)
            result.valueAt(0).let { v ->
                assertDimension(dimensionsOfSize(2, 2, 2), v)
                assertArrayContent(arrayOf(0, 1, 2, 3, 4, 5, 6, 7), v)
            }
        }
    }

    @Test
    fun encloseWithMultiArgAxis4() {
        parseAPLExpression("⊂[2 1 0] 2 2 2 ⍴ ⍳100").let { result ->
            assertDimension(emptyDimensions(), result)
            result.valueAt(0).let { v ->
                assertDimension(dimensionsOfSize(2, 2, 2), v)
                assertArrayContent(arrayOf(0, 4, 2, 6, 1, 5, 3, 7), v)
            }
        }
    }

    @Test
    fun encloseWithMultiArgAxis5() {
        parseAPLExpression("⊂[0 1 2] 2 2 2 ⍴ ⍳100").let { result ->
            assertDimension(emptyDimensions(), result)
            result.valueAt(0).let { v ->
                assertDimension(dimensionsOfSize(2, 2, 2), v)
                assertArrayContent(arrayOf(0, 1, 2, 3, 4, 5, 6, 7), v)
            }
        }
    }

    @Test
    fun encloseWithMultiArgAxis6() {
        parseAPLExpression("⊂[1 2] 2 2 2 ⍴ ⍳100").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            result.valueAt(0).let { v ->
                assertDimension(dimensionsOfSize(2, 2), v)
                assertArrayContent(arrayOf(0, 1, 2, 3), v)
            }
            result.valueAt(1).let { v ->
                assertDimension(dimensionsOfSize(2, 2), v)
                assertArrayContent(arrayOf(4, 5, 6, 7), v)
            }
        }
    }

    @Test
    fun encloseWithMultiArgAxisOutOfRange1() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⊂[3 0]2 2 2⍴⍳100")
        }
    }

    @Test
    fun encloseWithMultiArgAxisDuplicated() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⊂[0 0]2 2 2⍴⍳100")
        }
    }

    // Two-arg enclose

    @Test
    fun simpleTwoArgEnclose() {
        parseAPLExpression("s ← \"foo,bar,test,abcd\" ◊ (∼s=@,) ⊂ s").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertString("foo", result.valueAt(0))
            assertString("bar", result.valueAt(1))
            assertString("test", result.valueAt(2))
            assertString("abcd", result.valueAt(3))
        }
    }

    @Test
    fun twoArgEncloseWithOneResult() {
        parseAPLExpression("1 1 1 1 ⊂ \"abcd\"").let { result ->
            assertDimension(dimensionsOfSize(1), result)
            assertString("abcd", result.valueAt(0))
        }
    }

    @Test
    fun twoArgEncloseWithIncreasngAndDecreasingIndex() {
        parseAPLExpression("1 3 2 1 ⊂ \"abcd\"").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertString("a", result.valueAt(0))
            assertString("bcd", result.valueAt(1))
        }
    }

    @Test
    fun twoArgEncloseWithMultipleZeroes() {
        parseAPLExpression("1 0 0 1 1 1 1 ⊂ \"abcdefg\"").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertString("a", result.valueAt(0))
            assertString("defg", result.valueAt(1))
        }
    }

    @Test
    fun twoArgEncloseWithMultiDimensionalArgument() {
        parseAPLExpression("99 88 0 0 0 2 1 2 2 ⊂ 3 9 ⍴ \"abcdefghijk\"").let { result ->
            assertDimension(dimensionsOfSize(3, 3), result)
            assertString("ab", result.valueAt(0))
            assertString("fg", result.valueAt(1))
            assertString("hi", result.valueAt(2))
            assertString("jk", result.valueAt(3))
            assertString("de", result.valueAt(4))
            assertString("fg", result.valueAt(5))
            assertString("hi", result.valueAt(6))
            assertString("bc", result.valueAt(7))
            assertString("de", result.valueAt(8))
        }
    }

    @Test
    fun twoArgEncloseWithAxis() {
        parseAPLExpression("1 1 0 1 2 3 1 ⊂[0] 7 3 ⍴ \"abcdefghijk\"").let { result ->
            assertDimension(dimensionsOfSize(4, 3), result)
            val expected = listOf("ad", "be", "cf", "j", "k", "a", "b", "c", "d", "eh", "fi", "gj")
            for (i in expected.indices) {
                assertString(expected[i], result.valueAt(i))
            }
        }
    }

    @Test
    fun twoArgEncloseWithIllegalAxis() {
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("1 1 0 1 2 3 1 ⊂[4] 7 3 ⍴ \"abcdefghijk\"")
        }
    }

    @Test
    fun twoArgEncloseWithInitialZeroes() {
        parseAPLExpression("0 0 1 1 1⊂\"Hello\"").let { result ->
            assertDimension(dimensionsOfSize(1), result)
            assertString("llo", result.valueAt(0))
        }
    }
}
