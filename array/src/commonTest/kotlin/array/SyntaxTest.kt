package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SyntaxText : APLTest() {
    @Test
    fun simpleCustomSyntax() {
        val result = parseAPLExpression(
            """
            |defsyntax foo (:value x) { x + 10 }
            |200+foo (1)
        """.trimMargin())
        assertSimpleNumber(211, result)
    }

    @Test
    fun constants() {
        val result = parseAPLExpression(
            """
            |defsyntax foo (:constant x) { 10 }
            |foo x
            """.trimMargin())
        assertSimpleNumber(10, result)
    }

    @Test
    fun nonMatchedConstants() {
        assertFailsWith<ParseException> {
            parseAPLExpression(
                """
                |defsyntax foo (:constant x) { 10 }
                |foo xyz
                """.trimMargin())
        }
    }

    @Test
    fun valueArg() {
        val result = parseAPLExpression(
            """
            |defsyntax foo (:value x) { x + 1 }
            |foo (100)
            """.trimIndent())
        assertSimpleNumber(101, result)
    }

    @Test
    fun doubleValueArg() {
        val result = parseAPLExpression(
            """
            |defsyntax foo (:value x :value y) { x + y }
            |foo (200) (10)
            """.trimIndent())
        assertSimpleNumber(210, result)
    }

    @Test
    fun ifTestWithOptionalElse() {
        val result = parseAPLExpression(
            """
            |defsyntax xif (:value cond :function thenStatement :optional (:constant xelse :function elseStatement)) {
            |  ⍞((cond ≡ 1) ⌷ (⍞((isLocallyBound 'elseStatement) ⌷ λ{λ{⍬}} λ{elseStatement}) ⍬) thenStatement) cond
            |}
            |(xif (1) { 10 }) (xif (0) { 10 })
            """.trimMargin())
        assertDimension(dimensionsOfSize(2), result)
        assertSimpleNumber(10, result.valueAt(0))
        val secondRes = result.valueAt(1)
        assertDimension(dimensionsOfSize(0), secondRes)
    }

    @Test
    fun ifTestWithElse() {
        val result = parseAPLExpression(
            """
            |defsyntax xif (:value cond :function thenStatement :optional (:constant xelse :function elseStatement)) {
            |  ⍞((cond ≡ 1) ⌷ (⍞((isLocallyBound 'elseStatement) ⌷ λ{λ{⍬}} λ{elseStatement}) ⍬) thenStatement) cond
            |}
            |(xif (1) { 10 } xelse { 20 }) (xif (0) { 11 } xelse { 22 })
            """.trimMargin())
        assertDimension(dimensionsOfSize(2), result)
        assertArrayContent(arrayOf(10, 22), result)
    }

    @Test
    fun ifTest() {
        val result = parseAPLExpression(
            """
            |defsyntax xif (:value cond :function thenStatement :constant xelse :function elseStatement) {
            |  ⍞((cond ≡ 1) ⌷ elseStatement thenStatement) cond
            |}
            |(xif (1) { 10 } xelse { 11 }) (xif (0) { 100 } xelse { 101 })  
            """.trimMargin())
        assertDimension(dimensionsOfSize(2), result)
        assertArrayContent(arrayOf(10, 101), result)
    }

    @Test
    fun xifWithSideEffect() {
        val (result, output) = parseAPLExpressionWithOutput(
            """
            |defsyntax xif (:value cond :function thenStatement :constant xelse :function elseStatement) {
            |  ⍞((cond ≡ 1) ⌷ elseStatement thenStatement) cond
            |}
            |xif (1) { print "aa" ◊ 10 } xelse { print "bb" ◊ 11 }  
            """.trimMargin())
        assertSimpleNumber(10, result)
        assertEquals("aa", output)
    }

    @Test
    fun nonBindingDefinedFunction() {
        val result = parseAPLExpression(
            """
            |defsyntax foo (:nfunction a) { ⍞a 2 }
            |{ x←1 ◊ foo { x } } 0
            """.trimMargin())
        assertSimpleNumber(1, result)
    }

    @Test
    fun nonBindingDefinedFunctionWithArg() {
        val result = parseAPLExpression(
            """
            |defsyntax foo (:nfunction a) { ⍞a 2 }
            |{ x←1+⍵ ◊ foo { x+⍵ } } 3
            """.trimMargin())
        assertSimpleNumber(7, result)
    }

    @Test
    fun bindingDefinedFunctionWithArg() {
        val result = parseAPLExpression(
            """
            |defsyntax foo (:function a) { ⍞a 2 }
            |{ x←1+⍵ ◊ foo { x+⍵ } } 3
            """.trimMargin())
        assertSimpleNumber(6, result)
    }

    @Test
    fun exprFunction() {
        val result = parseAPLExpression(
            """
            |defsyntax foo (:exprfunction a) { 1+⍞a 0 }
            |foo (2)
            """.trimMargin())
        assertSimpleNumber(3, result)
    }

    @Test
    fun optionalExprFunction() {
        val (result, out) = parseAPLExpressionWithOutput(
            """
            |defsyntax foo (:value x :optional (:exprfunction a)) { x+(⍞a 0)+(⍞a 0) }
            |foo (10) (print 9)
            """.trimMargin())
        assertSimpleNumber(28, result)
        assertEquals("99", out)
    }
}
