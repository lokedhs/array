package array

import kotlin.test.Test

class StandardLibSimpleFunctionsTest : APLTest() {
    @Test
    fun simplePick() {
        parseAPLExpression("2 ⊇ 100+⍳10", true).let { result ->
            assertSimpleNumber(102, result)
        }
    }

    @Test
    fun simplePickArrayResult() {
        parseAPLExpression("2 3 ⊇ 100+⍳10", true).let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(102, 103), result)
        }
    }
}
