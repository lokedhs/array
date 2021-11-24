package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ParallelTest : APLTest() {
    @Test
    fun singletonForeach() {
        parseAPLExpression("{1+⍵}¨parallel 10", numTasks = 10).let { result ->
            assertSimpleNumber(11, result)
        }
    }

    @Test
    fun singletonForeachIsCollapsed() {
        val (result, out) = parseAPLExpressionWithOutput("{io:print ⍵ ⋄ 1+⍵}¨parallel 10", numTasks = 1)
        assertSimpleNumber(11, result)
        assertEquals("10", out)
    }

    @Test
    fun singletonForeachWithEnclosedIsCollapsed() {
        val (result, out) = parseAPLExpressionWithOutput("{10+⍵}¨parallel ⊂{io:print ⍵ ⋄ 1+⍵}¨10 20 30", numTasks = 10)
        assertTrue(result.isScalar())
        val inner = result.valueAt(0)
        assertDimension(dimensionsOfSize(3), inner)
        assertArrayContent(arrayOf(21, 31, 41), inner)
        assertEquals("102030", out)
    }

    @Test
    fun arraySmallerThanTasks() {
        parseAPLExpression("{1+⍵}¨parallel 10 11 12 13 14 15", numTasks = 10).let { result ->
            assertDimension(dimensionsOfSize(6), result)
            assertArrayContent(arrayOf(11, 12, 13, 14, 15, 16), result)
        }
    }

    @Test
    fun largeArray() {
        parseAPLExpression("{1+⍵}¨parallel ⍳10000", numTasks = 10).let { result ->
            assertDimension(dimensionsOfSize(10000), result)
            repeat(10000) { i ->
                assertSimpleNumber((i + 1).toLong(), result.valueAt(i))
            }
        }
    }

    @Test
    fun exceptionInParallelTask() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("{if(⍵=30) { io:print (1 2)[5] } ⋄ 1+⍵}¨parallel ⍳100", withStandardLib = true, numTasks = 10)
        }
    }

    @Test
    fun exceptionInSingletonForeach() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("{io:print (1 2)[5] ⋄ 1+⍵}¨parallel 10", withStandardLib = true, numTasks = 10)
        }
    }

    @Test
    fun singletonForeachTwoArg() {
        parseAPLExpression("11 {⍺+⍵}¨parallel 10", numTasks = 10).let { result ->
            assertSimpleNumber(21, result)
        }
    }

    @Test
    fun singletonForeachTwoArgIsCollapsed() {
        val (result, out) = parseAPLExpressionWithOutput("11 {io:print ⍺ ⋄ io:print ⍵ ⋄ ⍺+⍵}¨parallel 10", numTasks = 1)
        assertSimpleNumber(21, result)
        assertEquals("1110", out)
    }

    @Test
    fun singletonForeachWithEnclosedTwoArgIsCollapsed() {
        val (result, out) = parseAPLExpressionWithOutput("100 {⍺+⍵}¨parallel ⊂{io:print ⍵ ⋄ 1+⍵}¨10 20 30", numTasks = 10)
        assertTrue(result.isScalar())
        val inner = result.valueAt(0)
        assertDimension(dimensionsOfSize(3), inner)
        assertArrayContent(arrayOf(111, 121, 131), inner)
        assertEquals("102030", out)
    }

    @Test
    fun arraySmallerThanTasksTwoArg() {
        parseAPLExpression("100 200 300 400 500 600 {⍺+⍵}¨parallel 10 20 30 40 50 60", numTasks = 10).let { result ->
            assertDimension(dimensionsOfSize(6), result)
            assertArrayContent(arrayOf(110, 220, 330, 440, 550, 660), result)
        }
    }

    @Test
    fun largeArrayTwoArg() {
        parseAPLExpression("(1+⍳10000) {⍺+⍵}¨parallel ⍳10000", numTasks = 10).let { result ->
            assertDimension(dimensionsOfSize(10000), result)
            repeat(10000) { i ->
                assertSimpleNumber((i * 2 + 1).toLong(), result.valueAt(i))
            }
        }
    }
}
