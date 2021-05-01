package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PowerTest : APLTest() {
    @Test
    fun simpleCount() {
        parseAPLExpressionWithOutput("({(⍵-1) ⊣ io:print ⍵}⍣{⍵=0}) 9").let { (result, out) ->
            assertSimpleNumber(-1, result)
            assertEquals("9876543210", out)
        }
    }

    @Test
    fun simpleCountCheckRightArg() {
        parseAPLExpressionWithOutput("({(⍵-1) ⊣ io:print ⍵}⍣{⍺=1}) 9").let { (result, out) ->
            assertSimpleNumber(1, result)
            assertEquals("98765432", out)
        }
    }

    @Test
    fun powerWithNumericArg() {
        parseAPLExpression("({⍵+10}⍣3) 5").let { result ->
            assertSimpleNumber(35, result)
        }
    }

    @Test
    fun powerWithZeroArg() {
        parseAPLExpression("({⍵+10}⍣0) 5").let { result ->
            assertSimpleNumber(5, result)
        }
    }

    @Test
    fun powerWithOneArg() {
        parseAPLExpression("({⍵+200}⍣1) 5").let { result ->
            assertSimpleNumber(205, result)
        }
    }

    @Test
    fun powerWithNegativeValueIsAnError() {
        assertFailsWith<APLIllegalArgumentException> {
            parseAPLExpression("(×⍣¯1)5")
        }
    }
}
