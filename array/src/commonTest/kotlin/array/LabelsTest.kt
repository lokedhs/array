package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LabelsTest : APLTest() {
    @Test
    fun simpleLabelsTest() {
        parseAPLExpression("\"foo\" \"bar\" labels[1] 2 2 ⍴ 1 2 3 4").let { result ->
            assertDimension(dimensionsOfSize(2, 2), result)
            assertArrayContent(arrayOf(1, 2, 3, 4), result)
            val labels = result.labels
            assertNotNull(labels)
            val a = labels.labels
            assertEquals(2, a.size)
            assertNull(a[0])
            val labelsList = a[1]
            assertLabelList(listOf("foo", "bar"), labelsList)
        }
    }

    @Test
    fun preserveLabelsForTranspose() {
        parseAPLExpression("⍉ \"a\" \"b\" labels[0] \"q\" \"w\" \"e\" labels[1] 2 3 ⍴ ⍳100").let { result ->
            assertDimension(dimensionsOfSize(3, 2), result)
            assertArrayContent(arrayOf(0, 3, 1, 4, 2, 5), result)
            val labels = result.labels
            assertNotNull(labels)
            val a = labels.labels
            assertEquals(2, a.size)
            val axis0Labels = a[0]
            assertNotNull(axis0Labels)
            assertEquals(3, axis0Labels.size)
            assertLabelList(listOf("q", "w", "e"), axis0Labels)
            val axis1Labels = a[1]
            assertNotNull(axis1Labels)
            assertEquals(2, axis1Labels.size)
            assertLabelList(listOf("a", "b"), axis1Labels)
        }
    }

    private fun assertLabelList(expected: List<String>, actual: List<AxisLabel?>?) {
        assertNotNull(actual)
        assertEquals(expected.size, actual.size)
        expected.forEachIndexed { i, v ->
            val axisLabel = actual[i]
            assertNotNull(axisLabel)
            assertEquals(v, axisLabel.title)
        }
    }
}
