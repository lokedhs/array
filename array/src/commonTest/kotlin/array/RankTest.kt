package array

import kotlin.test.Ignore
import kotlin.test.Test

@Ignore
class RankTest : APLTest() {
    @Test
    fun rank0Test() {
        parseAPLExpression("({100+⍵}⍤0) ⍳10").let { result ->
            assertDimension(dimensionsOfSize(10), result)
            assertArrayContent(arrayOf(100, 101, 102, 103, 104, 105, 106, 107, 108, 109), result)
        }
    }

    @Test
    fun rank0Test2() {
        parseAPLExpression("({100,9+⍵}⍤0) ⍳10").let { result ->
            assertDimension(dimensionsOfSize(10, 2), result)
            assertArrayContent(arrayOf(100, 9, 100, 10, 100, 11, 100, 12, 100, 13, 100, 14, 100, 15, 100, 16, 100, 17, 100, 18), result)
        }
    }

    @Test
    fun rank1Test() {
        parseAPLExpression("({100,9+⍵}⍤1) ⍳10").let { result ->
            assertDimension(dimensionsOfSize(11), result)
            assertArrayContent(arrayOf(100, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18), result)
        }
    }

    @Test
    fun rank1Test2Dimension() {
        parseAPLExpression("({100,9+⍵}⍤1) 2 3 ⍴ ⍳10").let { result ->
            assertDimension(dimensionsOfSize(2, 4), result)
            assertArrayContent(arrayOf(100, 9, 10, 11, 100, 12, 13, 14), result)
        }
    }

    @Test
    fun aplContribExample0() {
        assertSimpleNumber(1, parseAPLExpression("(1 2⍴(1 2 3)(4 5 6))≡(⊂⍤1) 1 2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample1() {
        assertSimpleNumber(1, parseAPLExpression("(1 2⍴(1 2 3)(4 5 6))≡(⊂⍤¯2)1 2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample2() {
        assertSimpleNumber(1, parseAPLExpression("(,⊂2 3⍴⍳6)≡(⊂⍤2) 1 2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample3() {
        assertSimpleNumber(1, parseAPLExpression("(,⊂2 3⍴⍳6)≡(⊂⍤¯1) 1 2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample4() {
        assertSimpleNumber(1, parseAPLExpression("(2 2⍴(⍳4)(⍳3)(4 5 6 7)(3 4 5))≡(2 4⍴⍳8) ({⍺⍵}⍤¯1) 2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample5() {
        assertSimpleNumber(1, parseAPLExpression("(2 2⍴(⍳4)0(4 5 6 7)1) ≡ (2 4⍴⍳8) ({⍺⍵}⍤¯1) ⍳2"))
    }

    @Test
    fun aplContribExample6() {
        assertSimpleNumber(1, parseAPLExpression("(2 2⍴0(⍳4)1(4 5 6 7)) ≡ (2 4⍴⍳8) ({⍺⍵}⍤¯1)⍨ ⍳2"))
    }

    @Test
    fun aplContribExample7() {
        assertSimpleNumber(1, parseAPLExpression("(2 2⍴0(⍳4)1(4 5 6 7)) ≡ (2 4⍴⍳8) ({⍺⍵}⍤   0 1)⍨ ⍳2"))
    }

    @Test
    fun aplContribExample8() {
        assertSimpleNumber(1, parseAPLExpression("(2 2⍴0(⍳4)1(4 5 6 7)) ≡ (2 4⍴⍳8) ({⍺⍵}⍤ 9 0 1)⍨ ⍳2"))
    }

    @Test
    fun aplContribExample9() {
        assertSimpleNumber(1, parseAPLExpression("(2 2⍴0(⍳4)1(4 5 6 7)) ≡ (2 4⍴⍳8) ({⍺⍵}⍤¯9 0 1)⍨ ⍳2"))
    }

    @Test
    fun aplContribExample10() {
//        val res = parseAPLExpression("(⊂⍤ 9 2) 1 2 3⍴⍳6")
        val res = parseAPLExpression("({99,⍵,99}⍤ 9 2) 1 2 3⍴⍳6")
        println("res:\n${res.formatted(FormatStyle.PRETTY)}")
        val res2 = res.collapse()
        println("res2:\n${res2.formatted(FormatStyle.PRETTY)}")
        println("dim=${res2.dimensions}")
        println("dim0=${res2.valueAt(0).dimensions}")
        println("dim1=${res2.valueAt(0).valueAt(0).dimensions}")
        if (false) {
            assertSimpleNumber(1, parseAPLExpression("(,⊂2 3⍴⍳6)≡(⊂⍤ 9 2) 1 2 3⍴⍳6"))
        }
    }

    @Test
    fun aplContribExample11() {
        assertSimpleNumber(1, parseAPLExpression("(,⊂2 3⍴⍳6)≡(⊂⍤¯9 2) 1 2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample12() {
        assertSimpleNumber(1, parseAPLExpression("(,⊂2 3⍴⍳6)≡(⊂⍤2  9  9) 1 2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample13() {
        assertSimpleNumber(1, parseAPLExpression("(,⊂2 3⍴⍳6)≡(⊂⍤2 ¯9 ¯9) 1 2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample14() {
        assertSimpleNumber(1, parseAPLExpression("(2 3⍴0 1 2 7 8 9)≡0 4(+⍤¯1)2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample15() {
        assertSimpleNumber(1, parseAPLExpression("(2 3⍴0 1 2 7 8 9)≡0 4(+⍤   0 1)2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample16() {
        assertSimpleNumber(1, parseAPLExpression("(2 3⍴0 1 2 7 8 9)≡0 4(+⍤¯9 0 1)2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample17() {
        assertSimpleNumber(1, parseAPLExpression("(2 3⍴0 1 2 7 8 9)≡0 4(+⍤ 9 0 1)2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample18() {
        assertSimpleNumber(1, parseAPLExpression("(2 3 2⍴(12⍴0 4)+2/⍳6)≡0 4(+⍤1 0)2 3⍴⍳6"))
    }

    /*
(1 2⍴(1 2 3)(4 5 6))≡⊂⍤1⊢1 2 3⍴⍳6
(1 2⍴(1 2 3)(4 5 6))≡⊂⍤¯2⊢1 2 3⍴⍳6
(,⊂2 3⍴⍳6)≡⊂⍤2⊢1 2 3⍴⍳6
(,⊂2 3⍴⍳6)≡⊂⍤¯1⊢1 2 3⍴⍳6

(2 2⍴(⍳4)(⍳3)(4 5 6 7)(3 4 5))≡(2 4⍴⍳8) ({⍺⍵}⍤¯1) 2 3⍴⍳6
(2 2⍴(⍳4)0(4 5 6 7)1) ≡ (2 4⍴⍳8) ({⍺⍵}⍤¯1) ⍳2
(2 2⍴0(⍳4)1(4 5 6 7)) ≡ (2 4⍴⍳8) ({⍺⍵}⍤¯1)⍨ ⍳2
(2 2⍴0(⍳4)1(4 5 6 7)) ≡ (2 4⍴⍳8) ({⍺⍵}⍤   0 1)⍨ ⍳2
(2 2⍴0(⍳4)1(4 5 6 7)) ≡ (2 4⍴⍳8) ({⍺⍵}⍤ 9 0 1)⍨ ⍳2
(2 2⍴0(⍳4)1(4 5 6 7)) ≡ (2 4⍴⍳8) ({⍺⍵}⍤¯9 0 1)⍨ ⍳2

(,⊂2 3⍴⍳6)≡⊂⍤ 9 2⊢1 2 3⍴⍳6
(,⊂2 3⍴⍳6)≡⊂⍤¯9 2⊢1 2 3⍴⍳6
(,⊂2 3⍴⍳6)≡⊂⍤2  9  9⊢1 2 3⍴⍳6
(,⊂2 3⍴⍳6)≡⊂⍤2 ¯9 ¯9⊢1 2 3⍴⍳6

(2 3⍴0 1 2 7 8 9)≡0 4(+⍤¯1)2 3⍴⍳6
(2 3⍴0 1 2 7 8 9)≡0 4(+⍤   0 1)2 3⍴⍳6
(2 3⍴0 1 2 7 8 9)≡0 4(+⍤¯9 0 1)2 3⍴⍳6
(2 3⍴0 1 2 7 8 9)≡0 4(+⍤ 9 0 1)2 3⍴⍳6
(2 3 2⍴(12⍴0 4)+2/⍳6)≡0 4(+⍤1 0)2 3⍴⍳6
     */
}
