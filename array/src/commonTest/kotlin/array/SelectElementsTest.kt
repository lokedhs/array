package array

import kotlin.test.Test

class SelectElementsTest : APLTest() {
    @Test
    fun plainSelect() {
        parseAPLExpression("1 0 1 / 1 2 3").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(1, 3), result)
        }
    }

    @Test
    fun multiSelect() {
        parseAPLExpression("3 0 2 / 1 2 3").let { result ->
            assertDimension(dimensionsOfSize(5), result)
            assertArrayContent(arrayOf(1, 1, 1, 3, 3), result)
        }
    }

    @Test
    fun selectNone() {
        parseAPLExpression("0 0 0 0 0/ 1 2 3 4 5").let { result ->
            assertDimension(dimensionsOfSize(0), result)
        }
    }

    @Test
    fun select0FromScalar() {
        parseAPLExpression("0 / 10").let { result ->
            assertDimension(dimensionsOfSize(0), result)
        }
    }

    @Test
    fun select1FromScalar() {
        parseAPLExpression("1 / 10").let { result ->
            assertDimension(dimensionsOfSize(1), result)
            assertArrayContent(arrayOf(10), result)
        }
    }
}
