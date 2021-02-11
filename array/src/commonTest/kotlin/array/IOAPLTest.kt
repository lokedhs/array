package array

import kotlin.test.Test
import kotlin.test.assertEquals

class IOAPLTest : APLTest() {
    @Test
    fun plainReaddir() {
        parseAPLExpression("x ← io:readdir \"test-data/readdir-test/\" ◊ x[⍋x;]").let { result ->
            assertDimension(dimensionsOfSize(2, 1), result)
            assertString("a.txt", result.valueAt(0))
            assertString("file2.txt", result.valueAt(1))
        }
    }

    @Test
    fun readdirWithSize() {
        parseAPLExpression("x ← :size io:readdir \"test-data/readdir-test/\" ◊ x[⍋x;]").let { result ->
            assertDimension(dimensionsOfSize(2, 2), result)
            assertString("a.txt", result.valueAt(0))
            assertSimpleNumber(12, result.valueAt(1))
            assertString("file2.txt", result.valueAt(2))
            assertSimpleNumber(27, result.valueAt(3))
        }
    }

    @Test
    fun readCharacterFile() {
        parseAPLExpression("io:read \"test-data/multi.txt\"").let { result ->
            val expected = listOf("foo", "bar", "test", "abcdef", "testtest", "  testline", "", "aa", "ab", "ac", "ad")
            assertEquals(1, result.dimensions.size)
            assertEquals(expected.size, result.dimensions[0])
            for (i in expected.indices) {
                assertEquals(expected[i], result.valueAt(i).toStringValue())
            }
        }
    }
}
