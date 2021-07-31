package array

import kotlin.test.Test
import kotlin.test.assertFailsWith

@Suppress("UNUSED_CHANGED_VALUE")
class WhereTest : APLTest() {
    @Test
    fun simpleWhere() {
        parseAPLExpression("⍸ 0 0 1 1 0 0 1 1 1 1").let { result ->
            assertArrayContent(arrayOf(2, 3, 6, 7, 8, 9), result)
        }
    }

    @Test
    fun multiDimensionalWhere() {
        fun assertNumbers(a: Long, b: Long, value: APLValue) {
            assertDimension(dimensionsOfSize(2), value)
            assertSimpleNumber(a, value.valueAt(0))
            assertSimpleNumber(b, value.valueAt(1))
        }

        parseAPLExpression("⍸4 5⍴0 0 1\n").let { result ->
            assertDimension(dimensionsOfSize(6), result)
            assertNumbers(0, 2, result.valueAt(0))
            assertNumbers(1, 0, result.valueAt(1))
            assertNumbers(1, 3, result.valueAt(2))
            assertNumbers(2, 1, result.valueAt(3))
            assertNumbers(2, 4, result.valueAt(4))
            assertNumbers(3, 2, result.valueAt(5))
        }
    }

    @Test
    fun nonBooleanValues() {
        fun assertNumbers(a: Long, b: Long, value: APLValue) {
            assertDimension(dimensionsOfSize(2), value)
            assertSimpleNumber(a, value.valueAt(0))
            assertSimpleNumber(b, value.valueAt(1))
        }

        // 0 0  0 0  0 0  1 0  1 0  1 0  2 0  2 0  2 0  3 0  3 0  3 0
        parseAPLExpression("⍸ 4 5 ⍴ 3 0 0 0 0").let { result ->
            assertDimension(dimensionsOfSize(12), result)
            var i = 0
            assertNumbers(0, 0, result.valueAt(i++))
            assertNumbers(0, 0, result.valueAt(i++))
            assertNumbers(0, 0, result.valueAt(i++))
            assertNumbers(1, 0, result.valueAt(i++))
            assertNumbers(1, 0, result.valueAt(i++))
            assertNumbers(1, 0, result.valueAt(i++))
            assertNumbers(2, 0, result.valueAt(i++))
            assertNumbers(2, 0, result.valueAt(i++))
            assertNumbers(2, 0, result.valueAt(i++))
            assertNumbers(3, 0, result.valueAt(i++))
            assertNumbers(3, 0, result.valueAt(i++))
            assertNumbers(3, 0, result.valueAt(i++))
        }
    }

    @Test
    fun invalidFormat() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍸ 4 5 ⍴ 1 1 (2 2 ⍴ 1 0)")
        }
    }

    @Test
    fun scalarArgument() {
        assertAPLNull(parseAPLExpression("⍸ 1"))
    }

    @Test
    fun scalarArgumentWrongType() {
        assertFailsWith<APLIncompatibleDomainsException> {
            parseAPLExpression("⍸@a")
        }
        assertFailsWith<APLIncompatibleDomainsException> {
            parseAPLExpression("⍸ map 2 2 ⍴ 1 2 3 4")
        }
    }
}
