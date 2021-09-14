package array

import kotlin.test.Test
import kotlin.test.assertFailsWith

class ArrayLookupTest : APLTest() {
    @Test
    fun testSimpleArrayLookup() {
        val result = parseAPLExpression("2 ⌷ 1 2 3 4")
        assertSimpleNumber(3, result)
    }

    @Test
    fun testSimpleArrayLookupFromFunctionInvocation() {
        val result = parseAPLExpression("2 ⌷ 10 + 10 11 12 13")
        assertSimpleNumber(22, result)
    }

    @Test
    fun testIllegalIndex() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("3 ⌷ 1 2 3").collapse()
        }
    }

    @Test
    fun illegalDimension() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("2 3 ⌷ 1 2 3 4").collapse()
        }
    }

    @Test
    fun multiDimensionLookup() {
        parseAPLExpression("3 4 ⌷ 4 5 ⍴ 100+⍳100").let { result ->
            assertSimpleNumber(119, result)
        }
    }

    @Test
    fun multiValueLookup0() {
        parseAPLExpression("(⊂ 5 10) ⌷ 100+⍳100").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(105, 110), result)
        }
    }

    @Test
    fun multiValueLookup1() {
        parseAPLExpression("(3 0) (3 2) ⌷ 4 5⍴100+⍳100").let { result ->
            assertDimension(dimensionsOfSize(2, 2), result)
            assertArrayContent(arrayOf(118, 117, 103, 102), result)
        }
    }

    @Test
    fun lookupMajorAxis() {
        parseAPLExpression("0 ⌷ 4 4⍴100+⍳100").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(100, 101, 102, 103), result)
        }
    }

    @Test
    fun lookupMajorAxisExplicit() {
        parseAPLExpression("0 ⌷[0] 4 4⍴100+⍳100").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(100, 101, 102, 103), result)
        }
    }

    @Test
    fun lookupOtherAxisExplicit() {
        parseAPLExpression("0 ⌷[1] 4 4⍴100+⍳100").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(100, 104, 108, 112), result)
        }
    }

    @Test
    fun multiValueLookupMajorAxis() {
        parseAPLExpression("(⊂ 3 0) ⌷ 4 6 ⍴ 100+⍳100").let { result ->
            assertDimension(dimensionsOfSize(2, 6), result)
            assertArrayContent(arrayOf(118, 119, 120, 121, 122, 123, 100, 101, 102, 103, 104, 105), result)
        }
    }

    @Test
    fun lookupMultiAxes() {
        parseAPLExpression("0 1 ⌷[0 1] 4 4 4 ⍴ 100+⍳100").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(104,105,106,107), result)
        }
    }

    @Test
    fun lookupMultiAxesIllegalAxis() {
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("(0 1) (3 1) ⌷[0 3] 4 4 4 ⍴ 100+⍳100")
        }
    }

    @Test
    fun lookupMultiAxesNotEnoughArguments() {
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("(0 1) (3 1) ⌷[0] 4 4 4 ⍴ 100+⍳100")
        }
    }

    @Test
    fun lookupMultiAxesTooManyAxes() {
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("(0 1) (2 3) ⌷[0 1 2 3] 10 4 4 4 ⍴ 100+⍳100")
        }
    }

    @Test
    fun multiValueLookupWithIndexOutOfRange() {
        assertFailsWith<APLIndexOutOfBoundsException> {
            parseAPLExpression("(⊂ 11 100) ⌷ 100+⍳100")
        }
    }


    @Test
    fun duplicatedAxes() {
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("1 2 ⌷[1 1] 4 4⍴100+⍳100")
        }
    }
}
