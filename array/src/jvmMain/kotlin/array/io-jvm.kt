package array

import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.math.min

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
        transformIOException {
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
    }

    override fun close() {
        reader.close()
    }

}

class CharacterProviderReaderWrapper(val provider: CharacterProvider) : Reader() {
    private val buf = CharArray(10)
    private var bufPos = 0
    private var end = false

    override fun read(cbuf: CharArray, off: Int, len: Int): Int {
        if (end) {
            return -1
        }
        var i = 0
        if (bufPos > 0) {
            val length = min(len, bufPos)
            buf.copyInto(cbuf, 0, 0, length)
            i += length
            if (length < bufPos) {
                val newLength = bufPos - length
                buf.copyInto(buf, 0, length, newLength)
                bufPos = newLength
            } else {
                bufPos = 0
            }
        }
        while (i < len) {
            val value = provider.nextCodepoint() ?: break
            val valueString = charToString(value)
            val charsToCopy = min(valueString.length, len - i)
            var valueIndex = 0
            while (valueIndex < charsToCopy) {
                cbuf[i++] = valueString[valueIndex++]
            }
            if (charsToCopy < valueString.length) {
                var bufIndex = 0
                while (valueIndex < valueString.length) {
                    buf[bufIndex++] = valueString[valueIndex++]
                }
                bufPos = bufIndex
            }
        }
        if (i < len) {
            end = true
        }
        return if (i == 0) -1 else i
    }

    override fun close() {
        TODO("not implemented")
    }
}

actual fun openCharFile(name: String): CharacterProvider {
    transformIOException {
        return ReaderCharacterProvider(BufferedReader(FileReader(name, Charsets.UTF_8)))
    }
}

class InputStreamByteProvider(private val input: InputStream) : ByteProvider {
    override fun readByte(): Byte? {
        transformIOException {
            val result = input.read()
            return if (result == -1) null else result.toByte()
        }
    }

    override fun readBlock(buffer: ByteArray, start: Int?, length: Int?): Int? {
        transformIOException {
            val startPos = start ?: 0
            val result = input.read(buffer, startPos, length ?: buffer.size - startPos)
            return if (result == -1) null else result
        }
    }

    override fun close() {
        input.close()
    }
}

class ByteProviderInputStream(private val input: ByteProvider) : InputStream() {
    override fun read(): Int {
        transformIOException {
            val result = input.readByte()
            return if (result == null) {
                -1
            } else {
                result.toInt() and 0xFF
            }
        }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        transformIOException {
            return input.readBlock(b, off, len) ?: -1
        }
    }
}

actual fun openFile(name: String): ByteProvider {
    transformIOException {
        return InputStreamByteProvider(FileInputStream(name))
    }
}

private inline fun <T> transformIOException(fn: () -> T): T {
    try {
        return fn()
    } catch (e: IOException) {
        throw MPFileException(e.toString(), e)
    }
}

actual fun fileType(path: String): FileNameType? {
    val p = Path.of(path)
    return when {
        !Files.exists(p) -> null
        Files.isRegularFile(p) -> FileNameType.FILE
        Files.isDirectory(p) -> FileNameType.DIRECTORY
        else -> FileNameType.UNDEFINED
    }
}

actual fun readDirectoryContent(dirName: String): List<PathEntry> {
    val path = Paths.get(dirName)
    unless(Files.isDirectory(path)) {
        throw MPFileException("Argument is not a directory: ${dirName}")
    }
    val result = ArrayList<PathEntry>()
    Files.newDirectoryStream(path).forEach { p ->
        val fileNameType = when {
            Files.isDirectory(p) -> FileNameType.DIRECTORY
            Files.isRegularFile(p) -> FileNameType.FILE
            else -> FileNameType.UNDEFINED
        }
        result.add(
            PathEntry(
                p.fileName.toString(),
                if (fileNameType == FileNameType.FILE) Files.size(p) else 0,
                fileNameType))
    }
    return result
}
