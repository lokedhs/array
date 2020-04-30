package array

import array.complex.Complex
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
    fun mixStringsAndSymbolsShouldFail() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍋ \"foo\" \"bar\" 'somename").collapse()
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍒ \"foo\" \"bar\" 'somename").collapse()
        }
    }

    @Test
    fun symbolsAndNumberShouldFail() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍋ 1 2 3 'somename").collapse()
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍒ 1 2 3 'somename").collapse()
        }
    }

    @Test
    fun numbersAndComplexShouldFail() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍋ 1 2 3 1J2").collapse()
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍒ 1 2 3 1J2").collapse()
        }
    }

    @Test
    fun sortSymbols() {
        sortTest("'foo 'bar 'a 'test 'abclongerstring", arrayOf(2, 4, 1, 0, 3))
    }

    @Test
    fun tokenSymbolComparison() {
        val engine = Engine()
        val sym = APLSymbol(engine.internSymbol("foo"))
        val str = makeAPLString("bar")
        assertFailsWith<APLEvalException> {
            sym.compare(str)
        }
        assertFailsWith<APLEvalException> {
            str.compare(sym)
        }
    }

    @Test
    fun numberSymbolComparison() {
        val engine = Engine()
        val num = 1.makeAPLNumber()
        val sym = APLSymbol(engine.internSymbol("foo"))
        assertFailsWith<APLEvalException> {
            num.compare(sym)
        }
        assertFailsWith<APLEvalException> {
            sym.compare(num)
        }
    }

    @Test
    fun numberComplexComparison() {
        val num = 1.makeAPLNumber()
        val complex = Complex(2.0, 3.0).makeAPLNumber()
        assertFailsWith<APLEvalException> {
            num.compare(complex)
        }
        assertFailsWith<APLEvalException> {
            complex.compare(num)
        }
    }

    @Test
    fun listComparison() {
        val list1 = APLList(listOf(1.makeAPLNumber(), 2.makeAPLNumber()))
        val list2 = APLList(listOf(2.makeAPLNumber(), 4.makeAPLNumber()))
        assertFailsWith<APLEvalException> {
            list1.compare(list2)
        }
        assertFailsWith<APLEvalException> {
            list2.compare(list1)
        }
    }

    @Test
    fun numberCharComparison() {
        val char1 = APLChar('a'.toInt())
        val num1 = 1.makeAPLNumber()
        assertFailsWith<APLEvalException> {
            char1.compare(num1)
        }
        assertFailsWith<APLEvalException> {
            num1.compare(char1)
        }
    }

    @Test
    fun symbolCharComparison() {
        val engine = Engine()
        val sym = APLSymbol(engine.internSymbol("foo"))
        val ch = APLChar('a'.toInt())
        assertFailsWith<APLEvalException> {
            sym.compare(ch)
        }
        assertFailsWith<APLEvalException> {
            ch.compare(sym)
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
