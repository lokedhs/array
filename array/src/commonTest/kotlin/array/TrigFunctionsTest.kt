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
            assertComplexWithRange(Pair(1.609437911, 1.609437913), Pair(3.141592653, 3.141592655), result.valueAt(4))
            assertDoubleWithRange(Pair(14.02623085, 14.02623087), result.valueAt(5))
            assertComplexWithRange(Pair(0.8047189561, 0.8047189563), Pair(1.107148717, 1.107148718), result.valueAt(6))
            assertComplexWithRange(Pair(1.609437911, 1.609437913), Pair(2.214297435, 2.214297437), result.valueAt(7))
            assertComplexWithRange(Pair(1.763180261, 1.763180263), Pair(-1.030376828, -1.030376826), result.valueAt(8))
            assertComplexWithRange(Pair(6.941136738, 6.941136740), Pair(-1.668636477, -1.668636475), result.valueAt(9))
        }
    }

    @Test
    fun sinRealArgument() {
        parseAPLExpression("math:sin 0 1 0.5 ¯0.5 0.2 ¯0.1 2 3 4 5 ¯1").let { result ->
            assertDoubleWithRange(Pair(0.0, 0.0), result.valueAt(0))
            assertDoubleWithRange(Pair(0.8414709847, 0.8414709849), result.valueAt(1))
            assertDoubleWithRange(Pair(0.4794255385, 0.4794255387), result.valueAt(2))
            assertDoubleWithRange(Pair(-0.4794255387, -0.4794255385), result.valueAt(3))
            assertDoubleWithRange(Pair(0.1986693307, 0.1986693309), result.valueAt(4))
            assertDoubleWithRange(Pair(-0.09983341666, -0.09983341664), result.valueAt(5))
            assertDoubleWithRange(Pair(0.9092974267, 0.9092974269), result.valueAt(6))
            assertDoubleWithRange(Pair(0.1411200080, 0.1411200082), result.valueAt(7))
            assertDoubleWithRange(Pair(-0.7568024954, -0.7568024952), result.valueAt(8))
            assertDoubleWithRange(Pair(-0.9589242748, -0.9589242746), result.valueAt(9))
            assertDoubleWithRange(Pair(-0.8414709849, -0.8414709847), result.valueAt(10))
        }
    }

    @Test
    fun sinComplexArgument() {
        parseAPLExpression("math:sin 1J2 0J1 1J0 10J12 0.5J¯1.2 ¯2.1J0.1 ¯1.1J¯0.1").let { result ->
            assertComplexWithRange(Pair(3.165778512, 3.165778514), Pair(1.959601040, 1.959601042), result.valueAt(0))
            assertComplexWithRange(Pair(0.0, 0.0), Pair(1.175201193, 1.175201195), result.valueAt(1))
            assertDoubleWithRange(Pair(0.8414709847, 0.8414709849), result.valueAt(2))
            assertComplexWithRange(Pair(-44271.02123, -44271.02121), Pair(-68281.45586, -68281.45584), result.valueAt(3))
            assertComplexWithRange(Pair(0.8680745205, 0.8680745207), Pair(-1.324676964, -1.324676962), result.valueAt(4))
            assertComplexWithRange(Pair(-0.8675290115, -0.8675290113), Pair(-0.05056879357, -0.05056879355), result.valueAt(5))
            assertComplexWithRange(Pair(-0.8956671116, -0.8956671114), Pair(-0.0454352494, -0.0454352492), result.valueAt(6))
        }
    }

    @Test
    fun cosRealArgument() {
        parseAPLExpression("math:cos 0 1 0.5 ¯0.5 0.2 ¯0.1 2 3 4 5 ¯1 ¯2 ¯5").let { result ->
            assertDoubleWithRange(Pair(1.0, 1.0), result.valueAt(0))
            assertDoubleWithRange(Pair(0.5403023058, 0.5403023060), result.valueAt(1))
            assertDoubleWithRange(Pair(0.8775825618, 0.8775825620), result.valueAt(2))
            assertDoubleWithRange(Pair(0.8775825618, 0.8775825620), result.valueAt(3))
            assertDoubleWithRange(Pair(0.9800665777, 0.9800665779), result.valueAt(4))
            assertDoubleWithRange(Pair(0.9950041652, 0.9950041654), result.valueAt(5))
            assertDoubleWithRange(Pair(-0.4161468366, -0.4161468364), result.valueAt(6))
            assertDoubleWithRange(Pair(-0.9899924967, -0.9899924965), result.valueAt(7))
            assertDoubleWithRange(Pair(-0.6536436210, -0.6536436208), result.valueAt(8))
            assertDoubleWithRange(Pair(0.2836621854, 0.2836621856), result.valueAt(9))
            assertDoubleWithRange(Pair(0.5403023058, 0.5403023060), result.valueAt(10))
        }
    }

    @Test
    fun cosComplexArgument() {
        parseAPLExpression("math:cos 2J1 0J1 1J0 10J12 0.5J¯1.2 ¯2.1J0.1 ¯1.1J¯0.1").let { result ->
            assertComplexWithRange(Pair(-0.6421481248, -0.6421481246), Pair(-1.068607422, -1.068607420), result.valueAt(0))
            assertDoubleWithRange(Pair(1.543080634, 1.543080636), result.valueAt(1))
            assertDoubleWithRange(Pair(0.5403023058, 0.5403023060), result.valueAt(2))
            assertComplexWithRange(Pair(-68281.45586, -68281.45584), Pair(44271.02120, 44271.02122), result.valueAt(3))
            assertComplexWithRange(Pair(1.588999750, 1.588999752), Pair(0.7236743232, 0.7236743234), result.valueAt(4))
            assertComplexWithRange(Pair(-0.5073724394, -0.5073724392), Pair(0.08646487683, 0.08646487685), result.valueAt(5))
            assertComplexWithRange(Pair(0.4558659925, 0.4558659927), Pair(-0.08926934486, -0.08926934482), result.valueAt(6))
        }
    }
}
