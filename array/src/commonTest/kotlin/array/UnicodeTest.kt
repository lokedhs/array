package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnicodeTest : APLTest() {
    @Test
    fun stringToCodepoints() {
        parseAPLExpression("unicode:toCodepoints \"ab\"").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(97, 98), result)
        }
    }

    @Test
    fun highCharsToCodepoints() {
        parseAPLExpression("unicode:toCodepoints \"öä\"").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(246, 228), result)
        }
    }

    @Test
    fun multilingualToCodepoints() {
        parseAPLExpression("unicode:toCodepoints \"öäζた\"").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(246, 228, 950, 12383), result)
        }
    }

    @Test
    fun surrogatesToCodepoints() {
        parseAPLExpression("unicode:toCodepoints \"\ud835\udca2fooqwe\"").let { result ->
            assertDimension(dimensionsOfSize(7), result)
            assertArrayContent(arrayOf(119970, 102, 111, 111, 113, 119, 101), result)
        }
    }

    @Test
    fun recursiveToCodepoints() {
        parseAPLExpression("unicode:toCodepoints \"foo\" \"zxcvb\"").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            result.valueAt(0).let { v ->
                assertDimension(dimensionsOfSize(3), v)
                assertArrayContent(arrayOf(102, 111, 111), v)
            }
            result.valueAt(1).let { v ->
                assertDimension(dimensionsOfSize(5), v)
                assertArrayContent(arrayOf(122, 120, 99, 118, 98), v)
            }
        }
    }

    @Test
    fun singleCharToCodepoint() {
        parseAPLExpression("unicode:toCodepoints @a").let { result ->
            assertSimpleNumber(97, result)
        }
    }

    @Test
    fun mixedDatatypesToCodepoints() {
        parseAPLExpression("unicode:toCodepoints @a @b 2 1.2 (1 3)").let { result ->
            assertDimension(dimensionsOfSize(5), result)
            assertSimpleNumber(97, result.valueAt(0))
            assertSimpleNumber(98, result.valueAt(1))
            assertSimpleNumber(2, result.valueAt(2))
            assertDoubleWithRange(Pair(1.19, 1.21), result.valueAt(3))
            result.valueAt(4).let { v ->
                assertDimension(dimensionsOfSize(2), v)
                assertArrayContent(arrayOf(1, 3), v)
            }
        }
    }

    @Test
    fun numberToChar() {
        parseAPLExpression("unicode:fromCodepoints 120 121").let { result ->
            assertString("xy", result)
        }
    }

    @Test
    fun convertSingleIntegerToChar() {
        parseAPLExpression("unicode:fromCodepoints 120").let { result ->
            assertChar('x'.code, result)
        }
    }

    @Test
    fun convertCodepointsToCharsNonAscii() {
        parseAPLExpression("unicode:fromCodepoints 97 955 12493 119979 119990 8833").let { result ->
            assertDimension(dimensionsOfSize(6), result)
            assertChar('a'.code, result.valueAt(0))
            assertChar(955, result.valueAt(1))
            assertChar(12493, result.valueAt(2))
            assertChar(119979, result.valueAt(3))
            assertChar(119990, result.valueAt(4))
            assertChar(8833, result.valueAt(5))
        }
    }

    @Test
    fun convertCodepointsToCharsNested() {
        parseAPLExpression("unicode:fromCodepoints 99 100 101 (102 103)").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertChar('c'.code, result.valueAt(0))
            assertChar('d'.code, result.valueAt(1))
            assertChar('e'.code, result.valueAt(2))
            result.valueAt(3).let { v ->
                assertDimension(dimensionsOfSize(2), v)
                assertChar('f'.code, v.valueAt(0))
                assertChar('g'.code, v.valueAt(1))
            }
        }
    }

    @Test
    fun graphemesTest() {
        parseAPLExpression("unicode:toGraphemes \"abca⃞\"").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertString("a", result.valueAt(0))
            assertString("b", result.valueAt(1))
            assertString("c", result.valueAt(2))
            assertString("a⃞", result.valueAt(3))
        }
    }

    private fun assertChar(codepoint: Int, result: APLValue) {
        assertTrue(result is APLChar)
        assertEquals(codepoint, result.value)
    }
}
