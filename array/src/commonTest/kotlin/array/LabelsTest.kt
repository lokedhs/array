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
            assertLabelList(listOf("q", "w", "e"), a[0])
            assertLabelList(listOf("a", "b"), a[1])
        }
    }

    @Test
    fun rotateLabels() {
        parseAPLExpression("⌽ \"a\" \"b\" \"c\" \"d\" \"e\" \"f\" labels[0] ⍳6").let { result ->
            assertDimension(dimensionsOfSize(6), result)
            assertArrayContent(arrayOf(5, 4, 3, 2, 1, 0), result)
            val labels = result.labels
            assertNotNull(labels)
            val a = labels.labels
            assertEquals(1, a.size)
            assertLabelList(listOf("f", "e", "d", "c", "b", "a"), a[0])
        }
    }

    @Test
    fun rotate2D() {
        parseAPLExpression("⌽ \"aa\" \"bb\" \"cc\" labels[1] \"a\" \"b\" \"c\" labels[0] 3 3 ⍴ ⍳9").let { result ->
            assertDimension(dimensionsOfSize(3, 3), result)
            assertArrayContent(arrayOf(2, 1, 0, 5, 4, 3, 8, 7, 6), result)
            val labels = result.labels
            assertNotNull(labels)
            val a = labels.labels
            assertEquals(2, a.size)
            assertLabelList(listOf("a", "b", "c"), a[0])
            assertLabelList(listOf("cc", "bb", "aa"), a[1])
        }
    }

    @Test
    fun concatenationTwoLabels() {
        parseAPLExpression("(\"a\" \"b\" labels[1] 2 2 ⍴ ⍳4) ,[1] (\"aa\" \"bb\" labels[1] 2 2 ⍴ 100+⍳4)").let { result ->
            assertDimension(dimensionsOfSize(2, 4), result)
            assertArrayContent(arrayOf(0, 1, 100, 101, 2, 3, 102, 103), result)
            val labels = result.labels
            assertNotNull(labels)
            val a = labels.labels
            assertEquals(2, a.size)
            assertNull(a[0])
            assertLabelList(listOf("a", "b", "aa", "bb"), a[1])
        }
    }

    /**
     * Labels on the non-concatenated axis should be dropped
     */
    @Test
    fun concatenationConflictingAxisNames() {
        parseAPLExpression("(\"a\" \"b\" labels[1] 2 2 ⍴ ⍳4) ,[0] (\"aa\" \"bb\" labels[1] 2 2 ⍴ 100+⍳4)").let { result ->
            assertDimension(dimensionsOfSize(4, 2), result)
            assertArrayContent(arrayOf(0, 1, 2, 3, 100, 101, 102, 103), result)
            val labels = result.labels
            assertNull(labels)
        }
    }

    @Test
    fun concatenation1D() {
        parseAPLExpression("(\"a\" \"b\" labels[0] 1 2) , (\"aa\" \"bb\" labels[0] 100 101)").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1, 2, 100, 101), result)
            val labels = result.labels
            assertNotNull(labels)
            val a = labels.labels
            assertEquals(1, a.size)
            assertLabelList(listOf("a", "b", "aa", "bb"), a[0])
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
