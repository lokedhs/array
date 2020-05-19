package array

import kotlin.test.Test
import kotlin.test.assertEquals

class PowerTest : APLTest() {
    @Test
    fun simpleCount() {
        parseAPLExpressionWithOutput("{(⍵-1) ⊣ print ⍵}⍣{⍵=0} 9").let { (result, out) ->
            assertSimpleNumber(-1, result)
            assertEquals("9876543210", out)
        }
    }

    @Test
    fun simpleCountCheckRightArg() {
        parseAPLExpressionWithOutput("{(⍵-1) ⊣ print ⍵}⍣{⍺=1} 9").let { (result, out) ->
            assertSimpleNumber(1, result)
            assertEquals("98765432", out)
        }
    }
}
