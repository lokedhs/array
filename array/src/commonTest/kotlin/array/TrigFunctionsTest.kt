package array

import kotlin.test.Test

class TrigFunctionsTest : APLTest() {
    @Test
    fun logFunctionTest() {
        parseAPLExpression("⍟1 2 10 1.1 ¯5 1234567 1J2 ¯3J4 3J¯5 ¯101J¯1029").let { result ->
            assertDimension(dimensionsOfSize(10), result)
            assertSimpleDouble(0.0, result.valueAt(0))
            assertDoubleWithRange(Pair(0.6931471805, 0.6931471807), result.valueAt(1))
            assertDoubleWithRange(Pair(2.302585092, 2.302585094), result.valueAt(2))
            assertDoubleWithRange(Pair(0.0953101797, 0.0953101799), result.valueAt(3))
            assertComplexWithRange(Pair(1.609437911, 1.609437913), Pair(3.141592653,3.141592655), result.valueAt(4))
            assertDoubleWithRange(Pair(14.02623085, 14.02623087), result.valueAt(5))
            assertComplexWithRange(Pair(0.8047189561,0.8047189563),Pair(1.107148717,1.107148718), result.valueAt(6))
            assertComplexWithRange(Pair(1.609437911,1.609437913), Pair(2.214297435,2.214297437), result.valueAt(7))
            assertComplexWithRange(Pair(1.763180261,1.763180263), Pair(-1.030376828,-1.030376826),result.valueAt(8))
            assertComplexWithRange(Pair(6.941136738,6.941136740), Pair(-1.668636477,-1.668636475), result.valueAt(9))
        }
    }
}
