package array

import kotlin.test.Test
import kotlin.test.assertFailsWith

class ArrayLookupBracketsTest : APLTest() {
    @Test
    fun lookupSingleElement() {
        val result = parseAPLExpression("(10 20 30 40)[2]")
        assertSimpleNumber(30, result)
    }

    @Test
    fun lookupList() {
        val result = parseAPLExpression("(10 20 30 40)[0 2]")
        assertArrayContent(arrayOf(10, 30), result)
    }

    @Test
    fun lookup2DimensionalArrayValue() {
        val result = parseAPLExpression("(3 4 ⍴ 10+⍳12)[1;2]")
        assertSimpleNumber(16, result)
    }

    @Test
    fun lookupWithFunction() {
        val result = parseAPLExpression("(10000+⍳100)[90+⍳4]")
        assertArrayContent(arrayOf(10090, 10091, 10092, 10093), result)
    }

    @Test
    fun lookupWithRightSide() {
        val result = parseAPLExpression("3 4 (3 4 5)[0] 10 11 12")
        assertArrayContent(arrayOf(3, 4, 3, 10, 11, 12), result)
    }

    @Test
    fun selectAxis() {
        parseAPLExpression("(2 3 ⍴ ⍳6)[1;⍳3]").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(3, 4, 5), result)
        }
    }

    @Test
    fun lookupWithDefaultSelectionOneRow() {
        parseAPLExpression("(3 4 ⍴ 10×⍳100)[2;]").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(80, 90, 100, 110), result)
        }
    }

    @Test
    fun lookupWithDefaultSelectionTwoRows() {
        parseAPLExpression("(1000 4 ⍴ 10×⍳100)[992 997 1;]").let { result ->
            assertDimension(dimensionsOfSize(3, 4), result)
            assertArrayContent(arrayOf(680, 690, 700, 710, 880, 890, 900, 910, 40, 50, 60, 70), result)
        }
    }

    @Test
    fun lookupWithDefaultSelectionOneCol() {
        parseAPLExpression("(3 4 ⍴ 10×⍳100)[;3]").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(30, 70, 110), result)
        }
    }

    @Test
    fun lookupWithDefaultSelectionTwoCols() {
        parseAPLExpression("(3 4 ⍴ 10×⍳100)[;0 3]").let { result ->
            assertDimension(dimensionsOfSize(3, 2), result)
            assertArrayContent(arrayOf(0, 30, 40, 70, 80, 110), result)
        }
    }

    @Test
    fun lookupWithInvalidArgument() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("(1 2 3 4)[\"foo\"]").collapse()
        }
    }

    @Test
    fun lookupWithInvalidDimension() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("(1 2 3 4)[0;1]").collapse()
        }
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("(2 3 ⍴ 1 2 3 4 5 6)[0]").collapse()
        }
    }

    @Test
    fun indexLookupParseError() {
        assertFailsWith<ParseException> {
            parseAPLExpression("(2 3 4)[0")
        }
        assertFailsWith<ParseException> {
            parseAPLExpression("(2 3 4)]")
        }
    }

    @Test
    fun lookupIllegalIndex() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("(1 2 3 4)[5]").collapse()
        }
    }
}
