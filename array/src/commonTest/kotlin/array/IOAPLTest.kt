package array

import kotlin.test.Test

class IOAPLTest : APLTest() {
    @Test
    fun plainReaddir() {
        parseAPLExpression("x ← readdir \"test-data/readdir-test/\" ◊ x[⍋x;]").let { result ->
            assertDimension(dimensionsOfSize(2, 1), result)
            assertString("a.txt", result.valueAt(0))
            assertString("file2.txt", result.valueAt(1))
        }
    }

    @Test
    fun readdirWithSize() {
        parseAPLExpression("x ← :size readdir \"test-data/readdir-test/\" ◊ x[⍋x;]").let { result ->
            assertDimension(dimensionsOfSize(2, 2), result)
            assertString("a.txt", result.valueAt(0))
            assertSimpleNumber(12, result.valueAt(1))
            assertString("file2.txt", result.valueAt(2))
            assertSimpleNumber(27, result.valueAt(3))
        }
    }
}
