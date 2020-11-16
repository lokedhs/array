package array

import kotlin.test.Test
import kotlin.test.assertFailsWith

class UniqueTest : APLTest() {
    @Test
    fun simpleTest() {
        parseAPLExpression("∪ 0 3 1 1 0 0").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(0, 3, 1), result)
        }
    }

    @Test
    fun allDistinct() {
        parseAPLExpression("∪ 0 3 1 2").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(0, 3, 1, 2), result)
        }
    }

    @Test
    fun scalarValue() {
        parseAPLExpression("∪ 1").let { result ->
            assertDimension(dimensionsOfSize(1), result)
            assertArrayContent(arrayOf(1), result)
        }
    }

    @Test
    fun errorWhenMultiDimensional() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("∪ 2 2 ⍴ ⍳100")
        }
    }

    @Test
    fun emptyArray() {
        parseAPLExpression("∪⍬").let { result ->
            assertAPLNull(result)
        }
    }
}
