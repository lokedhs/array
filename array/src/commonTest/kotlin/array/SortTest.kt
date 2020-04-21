package array

import kotlin.test.Test
import kotlin.test.assertFailsWith

class SortTest : APLTest() {
    @Test
    fun numberSort() {
        sortTest("10 30 20 5 11 21 3 1 12 2", arrayOf(7, 9, 6, 3, 0, 4, 8, 2, 5, 1))
    }

    @Test
    fun sortStrings() {
        sortTest("\"foo\" \"bar\" \"test\" \"abc\" \"xyz\" \"some\" \"strings\" \"withlongtext\" \"b\"",
            arrayOf(3, 8, 1, 0, 5, 6, 2, 7, 4))
    }

    @Test
    fun sortMultiDimensional() {
        sortTest("3 4 ⍴ 8 5 1 7 0 11 6 2 4 3 10 9",
            arrayOf(1, 2, 0))
    }

    @Test
    fun sortMixedTypes() {
        sortTest("1.2 2 0.1 ¯9 ¯9.9 4 7.1 8.3", arrayOf(4, 3, 2, 0, 1, 5, 6, 7))
    }

    @Test
    fun sortSingleElement() {
        sortTest(",1", arrayOf(0))
    }

    @Test
    fun sortingScalarsShouldFail() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍋1").collapse()
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍒1").collapse()
        }
    }

    @Test
    fun sortingArraysOfDifferentDimensions() {
        sortTest("(⊂4 3 ⍴ 1 2 3 4 5 6) (⊂3 4 ⍴ 1 2 3 4 5 6) (⊂3 2 ⍴ 1 2 3 4) (⊂2 5 ⍴ 1 2) (⊂2 5 3 ⍴ 1 2) (⊂4 3 ⍴ 1 2 2 4 5 6)",
            arrayOf(3, 2, 1, 5, 0, 4))
    }

    @Test
    fun compareNumbersAndCharsShouldFail() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍋ @a 1 2 @b").collapse()
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍒ @a 1 2 @b").collapse()
        }
    }

    @Test
    fun compareListsShouldFail() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍋ (1;2) (2;1)").collapse()
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍒ (1;2) (2;1)").collapse()
        }
    }

    @Test
    fun compareComplexShouldFail() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍋ 1J2 2J3").collapse()
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍒ 1J2 2J3").collapse()
        }
    }

    @Test
    fun compareSymbolsShouldFail() {
        // Perhaps symbols should compare lexicographically?
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍋ 'foo 'bar").collapse()
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍒ 'foo 'bar").collapse()
        }
    }

    private fun sortTest(content: String, expected: Array<Int>) {
        fun sortTestInner(s: String, exp: Array<Int>) {
            parseAPLExpression(s).let { result ->
                assertDimension(dimensionsOfSize(exp.size), result)
                assertArrayContent(exp, result)
            }
        }

        sortTestInner("⍋ ${content}", expected)
        sortTestInner("⍒ ${content}", expected.reversedArray())
    }
}
