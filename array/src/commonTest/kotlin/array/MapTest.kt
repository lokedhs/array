package array

import kotlin.test.*

class MapTest {
    @Test
    fun simpleMapTest() {
        val map = ImmutableMap<String, Int>()
        assertEquals(0, map.size)

        val updated0 = map.copyAndPut("foo", 1)
        assertEquals(0, map.size)
        assertEquals(1, updated0.size)
        assertEquals(1, updated0["foo"])

        val updated1 = updated0.copyAndPut("bar", 2)
        assertEquals(0, map.size)
        assertEquals(1, updated0.size)
        assertEquals(2, updated1.size)
        assertEquals(1, updated0["foo"])
        assertEquals(1, updated1["foo"])
        assertEquals(2, updated1["bar"])
        assertTrue(updated1.containsValue(1))
        assertTrue(updated1.containsValue(2))
    }

    @Test
    fun removeTest() {
        val map = ImmutableMap<String, Int>()
        assertEquals(0, map.size)

        val updated0 = map.copyAndPutMultiple("foo" to 1, "bar" to 2)
        assertEquals(0, map.size)
        assertEquals(2, updated0.size)
        assertEquals(1, updated0["foo"])
        assertEquals(2, updated0["bar"])

        val updated1 = updated0.copyWithout("bar")
        assertEquals(0, map.size)
        assertEquals(2, updated0.size)
        assertEquals(1, updated1.size)
        assertEquals(1, updated1["foo"])
        assertFalse(updated1.containsKey("bar"))
        assertFalse(updated1.containsValue(2))

        // This test ensures that removing an element that does not exist in the map will return the same map
        val updated2 = updated1.copyWithout("bar")
        assertSame(updated1, updated2)
        assertEquals(1, updated2["foo"])
        assertFalse(updated1.containsKey("bar"))
    }
}
