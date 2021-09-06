package array

import kotlin.test.Test

class CompareTest : APLTest() {
    @Test
    fun testComparators() {
        testFunction(arrayOf(1, 0, 0, 1, 0, 0), "<")
        testFunction(arrayOf(0, 1, 0, 0, 1, 0), ">")
        testFunction(arrayOf(1, 0, 1, 1, 0, 1), "≤")
        testFunction(arrayOf(0, 1, 1, 0, 1, 1), "≥")
        testFunction(arrayOf(0, 0, 1, 0, 0, 1), "=")
    }

    @Test
    fun testIdenticalSimple() {
        assertSimpleNumber(1, parseAPLExpression("10≡10"))
        assertSimpleNumber(0, parseAPLExpression("10≡100"))
        assertSimpleNumber(1, parseAPLExpression("10 11≡10 11"))
        assertSimpleNumber(1, parseAPLExpression("11 12 13 14 15 16 17≡10+1 2 3 4 5 6 7"))
        assertSimpleNumber(0, parseAPLExpression("1 2 3≡1 2"))
        assertSimpleNumber(1, parseAPLExpression("(1 2) (3 4 (5 6)) ≡ (1 2) (3 4 (5 6))"))
        assertSimpleNumber(0, parseAPLExpression("(1 2) (3 4 (5 6)) ≡ (1 2) (3 4 (@a 6))"))
        assertSimpleNumber(1, parseAPLExpression("@a ≡ @a"))
        assertSimpleNumber(1, parseAPLExpression("1.2 ≡ 1.2"))
        assertSimpleNumber(0, parseAPLExpression("2J4 ≡ 2J3"))
    }

    @Test
    fun testNotIdentical() {
        assertSimpleNumber(0, parseAPLExpression("10≢10"))
        assertSimpleNumber(1, parseAPLExpression("10≢100"))
        assertSimpleNumber(0, parseAPLExpression("10 11≢4+6 7"))
        assertSimpleNumber(0, parseAPLExpression("11 12 13 14 15 16 17≢10+1 2 3 4 5 6 7"))
        assertSimpleNumber(1, parseAPLExpression("1 2 3≢1 2"))
        assertSimpleNumber(0, parseAPLExpression("\"foo\"≢\"foo\""))
        assertSimpleNumber(1, parseAPLExpression("(2 4 ⍴ 1 2 3 4 5 6 7 8) ≢ (4 2 ⍴ 1 2 3 4 5 6 7 8)"))
        assertSimpleNumber(1, parseAPLExpression("2 ≢ 1⍴2"))
        assertSimpleNumber(0, parseAPLExpression("'foo ≢ 'foo"))
        assertSimpleNumber(1, parseAPLExpression("@a ≢ @b"))
        assertSimpleNumber(0, parseAPLExpression("(1;2;3) ≢ (1;2;3)"))
    }

    @Test
    fun compareEqualsNonNumeric() {
        assertSimpleNumber(1, parseAPLExpression("@a = @a"))
        assertSimpleNumber(0, parseAPLExpression("@a = @b"))
        assertSimpleNumber(1, parseAPLExpression("('foo) = 'foo"))
        assertSimpleNumber(0, parseAPLExpression("('foo) = 'bar"))
    }

    @Test
    fun compareNotEqualsNonNumeric() {
        assertSimpleNumber(1, parseAPLExpression("'foo ≠ 'foox"))
        assertSimpleNumber(0, parseAPLExpression("'foo ≠ 'foo"))
        assertSimpleNumber(0, parseAPLExpression("@b ≠ @b"))
        assertSimpleNumber(1, parseAPLExpression("@a ≠ @b"))
        assertSimpleNumber(0, parseAPLExpression("(1;2;3) ≠ (1;2;3)"))
        assertSimpleNumber(1, parseAPLExpression("(1;2;3) ≠ (1;2;3;4)"))
    }

    private fun testFunction(expected: Array<Long>, name: String) {
        assertSimpleNumber(expected[0], parseAPLExpression("1${name}2"))
        assertSimpleNumber(expected[1], parseAPLExpression("2${name}1"))
        assertSimpleNumber(expected[2], parseAPLExpression("2${name}2"))
        assertSimpleNumber(expected[3], parseAPLExpression("0${name}1"))
        assertSimpleNumber(expected[4], parseAPLExpression("1${name}0"))
        assertSimpleNumber(expected[5], parseAPLExpression("0${name}0"))
    }

    @Test
    fun oneArgumentNotIdenticalTest() {
        assertSimpleNumber(0, parseAPLExpression("≢0⍴0"))
        assertSimpleNumber(0, parseAPLExpression("≢4"))
        assertSimpleNumber(4, parseAPLExpression("≢1 2 3 4"))
        assertSimpleNumber(1, parseAPLExpression("≢,4"))
        assertSimpleNumber(2, parseAPLExpression("≢2 3 ⍴ ⍳100"))
        assertSimpleNumber(8, parseAPLExpression("≢8 3 4 ⍴ ⍳100"))
        assertSimpleNumber(2, parseAPLExpression("≢(2 2 ⍴ ⍳4) (2 2 ⍴ ⍳4)"))
        assertSimpleNumber(5, parseAPLExpression("≢(1 2) (3 4) (5 6) (7 8) (9 10)"))
    }

    @Test
    fun oneArgumentIdenticalTest0() {
        assertSimpleNumber(1, parseAPLExpression("≡1 2 3"))
        assertSimpleNumber(0, parseAPLExpression("≡1"))
        assertSimpleNumber(2, parseAPLExpression("≡(1 2 3) (4 5 6)"))
        assertSimpleNumber(2, parseAPLExpression("≡(1 2 3) (10 11)"))
        assertSimpleNumber(2, parseAPLExpression("≡(1 2 3) (4 5 6) (100 200) (3000 4000)"))
        assertSimpleNumber(3, parseAPLExpression("≡(1 2 3) (4 5 6 (10 11))"))
        assertSimpleNumber(3, parseAPLExpression("≡((1 2) (3 4)) ((10 20) (30 40))"))
        assertSimpleNumber(1, parseAPLExpression("≡\"foo\""))
        assertSimpleNumber(2, parseAPLExpression("≡(1 2 ⍬)"))
    }

    @Test
    fun oneArgumentIdenticalWithHashMap() {
        val result = parseAPLExpression(
            """
            |a ← map 2 2 ⍴ 1 2 3 4
            |≡a
            """.trimMargin())
        assertSimpleNumber(0, result)
    }

    @Test
    fun oneArgumentIdenticalWithComplex() {
        assertSimpleNumber(0, parseAPLExpression("≡1J3"))
    }
}
