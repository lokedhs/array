package array

import kotlinx.cinterop.*
import platform.posix.*

class NativeFileException(message: String) : Exception(message)

actual class StringCharacterProvider actual constructor(val s: String) : CharacterProvider {
    private var pos = 0

    override fun nextCodepoint() = if (pos >= s.length) null else s[pos++].toInt()
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
    //return FileCharacterProvider(name)
    TODO("File reading not implemented in native mode yet")
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
