package array

import java.io.*

actual class StringCharacterProvider actual constructor(private val s: String) : CharacterProvider {

    private var pos = 0

    override fun sourceName() = "[inline code]"

    override fun nextCodepoint(): Int? {
        return if (pos >= s.length) {
            null
        } else {
            val result = s.codePointAt(pos)
            pos = s.offsetByCodePoints(pos, 1)
            result
        }
    }

    override fun close() {}
}

class KeyboardInputJvm : KeyboardInput {
    private val reader = BufferedReader(InputStreamReader(System.`in`))

    override fun readString(prompt: String): String? {
        print(prompt)
        return reader.readLine()
    }
}

actual fun makeKeyboardInput(): KeyboardInput {
    return KeyboardInputJvm()
}

class InvalidCharacter : Exception()

class ReaderCharacterProvider(private val reader: Reader, val sourceName: String? = null) : CharacterProvider {
    private var endOfFile = false

    override fun sourceName() = sourceName

    override fun nextCodepoint(): Int? {
        if (endOfFile) {
            return null
        }

        val v = reader.read()
        return when {
            v == -1 -> {
                endOfFile = true
                null
            }
            Character.isHighSurrogate(v.toChar()) -> {
                val v2 = reader.read()
                if (!Character.isLowSurrogate(v2.toChar())) {
                    throw InvalidCharacter()
                }
                Character.toCodePoint(v.toChar(), v2.toChar())
            }
            else -> v
        }
    }

    override fun close() {
        reader.close()
    }

}

actual fun openCharFile(name: String): CharacterProvider {
    return ReaderCharacterProvider(BufferedReader(FileReader(name, Charsets.UTF_8)))
}

class InputStreamByteProvider(private val input: InputStream) : ByteProvider {
    override fun readByte(): Byte? {
        val result = input.read()
        return if (result == -1) null else result.toByte()
    }

    override fun readBlock(buffer: ByteArray, start: Int?, length: Int?): Int? {
        val startPos = start ?: 0
        val result = input.read(buffer, startPos, length ?: buffer.size - startPos)
        return if (result == -1) null else result
    }

    override fun close() {
        input.close()
    }
}

class ByteProviderInputStream(private val input: ByteProvider) : InputStream() {
    override fun read(): Int {
        val result = input.readByte()
        return if (result == null) {
            -1
        } else {
            result.toInt() and 0xFF
        }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        return input.readBlock(b, off, len) ?: -1
    }
}

actual fun openFile(name: String): ByteProvider {
    return transformIOException {
        InputStreamByteProvider(FileInputStream(name))
    }
}

private fun <T> transformIOException(fn: () -> T): T {
    try {
        return fn()
    } catch (e: IOException) {
        throw MPFileException(e.toString(), e)
    }
}
