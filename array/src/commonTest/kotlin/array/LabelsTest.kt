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
