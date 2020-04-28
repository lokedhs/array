package array

import kotlinx.cinterop.*
import platform.posix.*

class NativeFileException(message: String) : Exception(message)

actual class StringCharacterProvider actual constructor(private val s: String) : CharacterProvider {
    private var pos = 0

    override fun sourceName(): String? = null
    override fun nextCodepoint(): Int? {
        if (pos >= s.length) {
            return null
        }

        val ch = s[pos++]
        return when {
            ch.isHighSurrogate() -> {
                if (pos < s.length) {
                    val low = s[pos++]
                    if (low.isLowSurrogate()) {
                        Char.toCodePoint(ch, low)
                    } else {
                        throw IllegalStateException("A high surrogate should be followed by a low surrogate")
                    }
                } else {
                    throw IllegalStateException("End of string when low surrogate was expected")
                }
            }
            ch.isLowSurrogate() -> throw IllegalStateException("Unexpected low surrogate")
            else -> ch.toInt()
        }
    }

    override fun close() {}
}

class KeyboardInputNative : KeyboardInput {
    override fun readString(prompt: String): String? {
        print(prompt)
        memScoped {
            val bufSize = 10240
            val buf = allocArray<ByteVar>(bufSize)
            return fgets(buf, bufSize, stdin)?.toKString()
        }
    }
}

//class KeyboardInputLibedit : KeyboardInput {
//    override fun readString(prompt: String): String? {
//        val result = libedit.readline(prompt)
//        return if (result == null) {
//            null
//        } else {
//            val resultConverted = result.toKString()
//            free(result.rawValue)
//            resultConverted
//        }
//    }
//}

actual fun makeKeyboardInput(): KeyboardInput {
    return KeyboardInputNative()
}

actual fun openCharFile(name: String): CharacterProvider {
    val fd = open(name, O_RDONLY)
    if (fd == -1) {
        throw NativeFileException(nativeErrorString())
    }
    return FileCharacterProvider(LinuxByteProvider(fd), name)
}

@OptIn(ExperimentalUnsignedTypes::class)
class FileCharacterProvider(val backend: LinuxByteProvider, val sourceName: String) : CharacterProvider {
    private val buf = ByteArray(1024)
    private var pos = 0
    private var length = 0
    private var pushbackChar: Byte? = null

    override fun sourceName() = sourceName

    override fun nextCodepoint(): Int? {
        var utfCodepoint = 0
        var utfBytesSeen = 0
        var utfBytesNeeded = 0
        var utfLowerBoundary = 0x80
        var utfUpperBoundary = 0xBF

        while (true) {
            val nextByte = readNextByte()
            if (nextByte == null) {
                if (utfBytesNeeded != 0) {
                    throw NativeFileException("Truncated UTF-8 stream")
                } else {
                    return null
                }
            }
            val a = nextByte.toInt() and 0xFF
            if (utfBytesNeeded == 0) {
                when {
                    a <= 0x7F -> {
                        return a
                    }
                    a in (0xC2..0xDF) -> {
                        utfBytesNeeded = 1
                        utfCodepoint = (a and 0x1F)
                    }
                    a in (0xE0..0xEF) -> {
                        if (a == 0xE0) utfLowerBoundary = 0xA0
                        else if (a == 0xED) utfUpperBoundary = 0x9F
                        utfBytesNeeded = 2
                        utfCodepoint = (a and 0xF)
                    }
                    a in (0xF0..0xF4) -> {
                        if (a == 0xF0) utfLowerBoundary = 0x90
                        else if (a == 0xF4) utfUpperBoundary = 0x8F
                        utfBytesNeeded = 3
                        utfCodepoint = (a and 0x7)
                    }
                    else -> throw NativeFileException("Unexpected value in UTF-8 stream")
                }
            } else {
                if (a < utfLowerBoundary || a > utfUpperBoundary) {
                    pushbackChar = nextByte
                    throw NativeFileException("Invalid UTF-8 bytes")
                }
                utfLowerBoundary = 0x80
                utfUpperBoundary = 0xBF
                utfCodepoint = (utfCodepoint shl 6) or (a and 0x3F)
                utfBytesSeen++
                if (utfBytesSeen == utfBytesNeeded) {
                    return utfCodepoint
                }
            }
        }
    }

    private fun readNextByte(): Byte? {
        if (pushbackChar != null) {
            val old = pushbackChar
            pushbackChar = null
            return old
        }
        if (pos >= length) {
            val res = backend.readBlock(buf) ?: return null
            if (res == 0) {
                return null
            }
            length = res
            pos = 0
        }
        return buf[pos++]
    }

    override fun close() {
        backend.close()
    }

}

class LinuxByteProvider(val fd: Int) : ByteProvider {
    @ExperimentalUnsignedTypes
    override fun readByte(): Byte? {
        val buf = ByteArray(1)
        val result = readBlock(buf, 0, 1)
        return if (result == 0) {
            null
        } else {
            buf[0]
        }
    }

    @ExperimentalUnsignedTypes
    override fun readBlock(buffer: ByteArray, start: Int?, length: Int?): Int? {
        val startPos = start ?: 0
        val lengthInt = length ?: buffer.size - startPos
        memScoped {
            val buf = allocArray<ByteVar>(lengthInt)
            val result = read(fd, buf, lengthInt.toULong())
            if (result == -1L) {
                throw NativeFileException(nativeErrorString())
            }
            val resultLen = result.toInt()
            for (i in 0 until resultLen) {
                buffer[startPos + i] = buf[i].toByte()
            }
            return resultLen
        }
    }

    override fun close() {
        if (close(fd) == -1) {
            throw NativeFileException(nativeErrorString())
        }
    }
}

actual fun openFile(name: String): ByteProvider {
    val fd = open(name, O_RDONLY)
    if (fd == -1) {
        throw NativeFileException(nativeErrorString())
    }
    return LinuxByteProvider(fd)
}

private fun nativeErrorString(): String {
    return strerror(errno)?.toKString() ?: "unknown error"
}
