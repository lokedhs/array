package array

import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalStdlibApi
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
    fun testEndOfFile() {
        openFile("test-data/plain.txt").use { input ->
            val buf = ByteArray(10)
            val result = input.readBlock(buf)
            assertEquals(7, result)
            assertByteArrayContent("abcbar\n".encodeToByteArray(), buf)
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

    private fun assertByteArrayContent(expected: ByteArray, content: ByteArray, start: Int? = null) {
        val startPos = start ?: 0
        for (i in expected.indices) {
            assertEquals(expected[i], content[startPos + i])
        }
    }
}
