package array

import kotlin.test.Test
import kotlin.test.assertEquals

class CharTypesTest {
    @Test
    fun breakIteratorTest() {
        val list = "abc test".asGraphemeList()
        assertEquals(8, list.size)
        assertEquals("a", list[0])
        assertEquals("b", list[1])
        assertEquals("c", list[2])
        assertEquals(" ", list[3])
        assertEquals("t", list[4])
        assertEquals("e", list[5])
        assertEquals("s", list[6])
        assertEquals("t", list[7])
    }

    @Test
    fun astralPlaneBreakIterator() {
        val list = "ab\uD835\uDC9Fc".asGraphemeList()
        assertEquals(4, list.size)
        assertEquals("a", list[0])
        assertEquals("b", list[1])
        assertEquals("\uD835\uDC9F", list[2])
        assertEquals("c", list[3])
    }
}
