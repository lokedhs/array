package array

import kotlin.test.Ignore
import kotlin.test.Test

class StandardLibMath : APLTest() {
    @Test
    fun testSimpleMatrixInverse() {
        parseAPLExpression("⌹ 5 5 ⍴ 1 0 0 0 0 0", true).let { result ->
            assertDimension(dimensionsOfSize(5, 5), result)
            assertArrayContent(
                arrayOf(
                    1, 0, 0, 0, 0,
                    0, 1, 0, 0, 0,
                    0, 0, 1, 0, 0,
                    0, 0, 0, 1, 0,
                    0, 0, 0, 0, 1
                ), result)
        }
    }

    @Ignore
    @Test
    fun testMatrixDivision() {
        parseAPLExpression("(4 4⍴12 1 4 10 ¯6 ¯5 4 7 ¯4 9 3 4 ¯2 ¯6 7 7)⌹93 81 93.5 120.5", true).let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertDoubleWithRange(Pair(0.00038988887, 0.00038988889), result.valueAt(0))
            assertDoubleWithRange(Pair(-0.0050295666, -0.0050295664), result.valueAt(1))
            assertDoubleWithRange(Pair(0.047306516, 0.047306518), result.valueAt(2))
            assertDoubleWithRange(Pair(0.07055688, 0.07055690), result.valueAt(3))
        }
    }
}
