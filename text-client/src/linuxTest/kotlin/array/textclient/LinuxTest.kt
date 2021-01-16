package array.textclient

import array.Engine
import array.StringSourceLocation
import kotlin.test.Test
import kotlin.test.assertEquals

class LinuxTest {
    @Test
    fun testCompilation() {
        val engine = Engine()
        val result = engine.parseAndEval(StringSourceLocation("10000 + ⍳3"), true).collapse()
        val d = result.dimensions
        assertEquals(1, d.size)
        assertEquals(3, d[0])
        assertEquals(10000L, result.valueAt(0).ensureNumber().asLong())
        assertEquals(10001L, result.valueAt(1).ensureNumber().asLong())
        assertEquals(10002L, result.valueAt(2).ensureNumber().asLong())
    }
}
