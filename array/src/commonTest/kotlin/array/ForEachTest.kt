package array

import kotlin.test.Test
import kotlin.test.assertEquals

class ForEachTest : APLTest() {
    @Test
    fun simpleForEach() {
        parseAPLExpression("÷¨1 4 2 16").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertEquals(1.0, result.valueAt(0).ensureNumber().asDouble())
            assertEquals(0.25, result.valueAt(1).ensureNumber().asDouble())
            assertEquals(0.5, result.valueAt(2).ensureNumber().asDouble())
            assertEquals(0.0625, result.valueAt(3).ensureNumber().asDouble())
        }
    }

    @Test
    fun twoArgForEach() {
        parseAPLExpression("1 2 3 4+¨1").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(2, 3, 4, 5), result)
        }
    }

    @Test
    fun scalarForEach() {
        parseAPLExpression("1+¨11").let { result ->
            assertSimpleNumber(12, result)
        }
    }

    @Test
    fun scalarForEachOneArg() {
        parseAPLExpression("+¨1").let { result ->
            assertSimpleNumber(1, result)
        }
    }

    @Test
    fun scalarForEachOneArgCustomFunction() {
        val result = parseAPLExpression(
            """
            |∇ foo (X) {
            |  X + 1
            |}
            |foo¨ 1
            """.trimMargin()
        )
        assertSimpleNumber(2, result)
    }
}
