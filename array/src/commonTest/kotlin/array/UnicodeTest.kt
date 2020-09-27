package array

import kotlin.test.Test

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
}
