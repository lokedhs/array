package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IoTest {
    @Test
    fun testBinaryFile() {
        openFile("test-data/plain.txt").use { input ->
            val buf = ByteArray(3)
            val result = input.readBlock(buf)
            assertEquals(3, result)
            assertByteArrayContent("abc".encodeToByteArray(), buf)
        }
    }

    @Test
    fun testPartialBlock() {
        openFile("test-data/plain.txt").use { input ->
            val buf = ByteArray(10) { 0 }
            val result = input.readBlock(buf, 2, 3)
            assertEquals(3, result)
            assertByteArrayContent("abc".encodeToByteArray(), buf, 2)
        }
    }

    @Test
    fun testMultipleReads() {
        openFile("test-data/plain.txt").use { input ->
            val buf = ByteArray(5) { 0 }
            val result1 = input.readBlock(buf, 2, 3)
            assertEquals(3, result1)
            assertByteArrayContent("abc".encodeToByteArray(), buf, 2)
            val result2 = input.readBlock(buf, 0, 3)
            assertEquals(3, result2)
            assertByteArrayContent("bar".encodeToByteArray(), buf)
        }
    }

    @Test
    fun testCharacterContent() {
        openCharFile("test-data/char-tests.txt").use { input ->
            assertEquals(0x61, input.nextCodepoint())
            assertEquals(0x62, input.nextCodepoint())
            assertEquals(0x2283, input.nextCodepoint())
            assertEquals(0x22C6, input.nextCodepoint())
            assertEquals(0x1D49F, input.nextCodepoint())
            assertEquals(0xE01, input.nextCodepoint())
            assertEquals(0xA, input.nextCodepoint())
            assertNull(input.nextCodepoint())
        }
    }

    @Test
    fun testReadline() {
        openCharFile("test-data/plain.txt").use { input ->
            assertEquals("abcbar", input.nextLine())
            assertNull(input.nextCodepoint())
        }
    }

    private fun assertByteArrayContent(expected: ByteArray, content: ByteArray, start: Int? = null) {
        val startPos = start ?: 0
        for (i in expected.indices) {
            assertEquals(expected[i], content[startPos + i])
        }
    }
}
