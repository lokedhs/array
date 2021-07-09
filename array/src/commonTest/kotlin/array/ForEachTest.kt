package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
            """.trimMargin())
        assertSimpleNumber(2, result)
    }

    @Test
    fun scalarForEachTwoArgsWithVectorResult() {
        parseAPLExpression("1,¨1").let { result ->
            assertDimension(emptyDimensions(), result)
            val inner = result.valueAt(0)
            assertDimension(dimensionsOfSize(2), inner)
            assertArrayContent(arrayOf(1, 1), inner)
        }
    }

    @Test
    fun scalarForEachOneArgWithVectorResult() {
        parseAPLExpression(",¨1").let { result ->
            assertDimension(emptyDimensions(), result)
            val inner = result.valueAt(0)
            assertDimension(dimensionsOfSize(1), inner)
            assertArrayContent(arrayOf(1), inner)
        }
    }

    @Test
    fun forEachEnclosedOneArg() {
        parseAPLExpression("{3+⍵}¨ ⊂1 2 3").let { result ->
            assertTrue(result.isScalar())
            val v = result.valueAt(0)
            assertDimension(dimensionsOfSize(3), v)
            assertArrayContent(arrayOf(4, 5, 6), v)
        }
    }

    @Test
    fun forEachEnclosedTwoArg() {
        parseAPLExpression("(⊂10 20 30) {10000+⍵+⍺}¨ ⊂1 2 3").let { result ->
            assertTrue(result.isScalar())
            val v = result.valueAt(0)
            assertDimension(dimensionsOfSize(3), v)
            assertArrayContent(arrayOf(10011, 10022, 10033), v)
        }

    }

    @Test
    fun forEachRightEnclosedTwoArg() {
        parseAPLExpression("10 20 30 {10000+⍵+⍺}¨ ⊂1 2 3").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            result.valueAt(0).let { v ->
                assertDimension(dimensionsOfSize(3), v)
                assertArrayContent(arrayOf(10011, 10012, 10013), v)
            }
            result.valueAt(1).let { v ->
                assertDimension(dimensionsOfSize(3), v)
                assertArrayContent(arrayOf(10021, 10022, 10023), v)
            }
            result.valueAt(2).let { v ->
                assertDimension(dimensionsOfSize(3), v)
                assertArrayContent(arrayOf(10031, 10032, 10033), v)
            }
        }

    }

    @Test
    fun forEachLeftEnclosedTwoArg() {
        parseAPLExpression("(⊂10 20 30) {10000+⍵+⍺}¨ 1 2 3").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            result.valueAt(0).let { v ->
                assertDimension(dimensionsOfSize(3), v)
                assertArrayContent(arrayOf(10011, 10021, 10031), v)
            }
            result.valueAt(1).let { v ->
                assertDimension(dimensionsOfSize(3), v)
                assertArrayContent(arrayOf(10012, 10022, 10032), v)
            }
            result.valueAt(2).let { v ->
                assertDimension(dimensionsOfSize(3), v)
                assertArrayContent(arrayOf(10013, 10023, 10033), v)
            }
        }

    }
}
