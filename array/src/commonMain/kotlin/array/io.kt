package array

open class MPFileException(message: String, cause: Exception? = null) : Exception(message, cause)
open class MPFileNotFoundException(message: String, cause: Exception? = null) : MPFileException(message, cause)

interface NativeCloseable {
    fun close()
}

fun <T : NativeCloseable, R> T.use(fn: (T) -> R): R {
    try {
        return fn(this)
    } finally {
        close()
    }
}

interface ByteProvider : NativeCloseable {
    fun sourceName(): String? = null
    fun readByte(): Byte?

    fun readBlock(buffer: ByteArray, start: Int? = null, length: Int? = null): Int? {
        val startPos = start ?: 0
        val n = length ?: buffer.size - startPos
        var i = 0
        while (i < n) {
            val value = readByte() ?: break
            buffer[startPos + i] = value
            i++
        }
        return if (i == 0) null else i
    }
}

interface CharacterProvider : NativeCloseable {
    fun sourceName(): String? = null
    fun nextCodepoint(): Int?

    fun nextLine(): String? {
        val buf = StringBuilder()
        while (true) {
            val ch = nextCodepoint()
            if (ch == null) {
                val s = buf.toString()
                return if (s.isEmpty()) null else s
            } else if (ch == '\n'.toInt()) {
                return buf.toString()
            }
            buf.addCodepoint(ch)
        }
    }

    fun lines() = generateSequence { nextLine() }
}

class PushBackCharacterProvider(val sourceLocation: SourceLocation) : CharacterProvider {
    private class CharWithPosition(val character: Int, val line: Int, val col: Int)

    private val source = sourceLocation.open()

    private val pushBackList = ArrayList<CharWithPosition>()
    private val pushBackHistory = ArrayDeque<CharWithPosition>()

    private var line = 0
    private var col = 0

    fun nextCodepointWithPos() = Pair(nextCodepoint(), pos())

    override fun nextCodepoint(): Int? {
        val ch = if (pushBackList.isNotEmpty()) {
            pushBackList.removeAt(pushBackList.size - 1)
        } else {
            val ch = source.nextCodepoint() ?: return null
            val chWithPos = CharWithPosition(ch, line, col)
            pushBackHistory.addLast(chWithPos)
            // Keep the pushback list at 5 elements at most
            while (pushBackHistory.size > 5) {
                pushBackHistory.removeFirst()
            }
            chWithPos
        }
        if (ch.character == '\n'.toInt()) {
            line++
            col = 0
        } else {
            col++
        }
        return ch.character
    }

    fun pushBack() {
        val ch = pushBackHistory.removeLast()
        pushBackList.add(ch)
        line = ch.line
        col = ch.col
    }

    fun pos() = Position(sourceLocation, line, col)

    override fun close() {
        source.close()
    }

    override fun sourceName() = source.sourceName()
}

class ByteToCharacterProvider(val source: ByteProvider) : CharacterProvider {
    override fun sourceName(): String? = source.sourceName()

    private var pushbackChar: Byte? = null

    override fun nextCodepoint(): Int? {
        var utfCodepoint = 0
        var utfBytesSeen = 0
        var utfBytesNeeded = 0
        var utfLowerBoundary = 0x80
        var utfUpperBoundary = 0xBF

        while (true) {
            val nextByte = source.readByte()
            if (nextByte == null) {
                if (utfBytesNeeded != 0) {
                    throw MPFileException("Truncated UTF-8 stream")
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
                    else -> throw MPFileException("Unexpected value in UTF-8 stream")
                }
            } else {
                if (a < utfLowerBoundary || a > utfUpperBoundary) {
                    pushbackChar = nextByte
                    throw MPFileException("Invalid UTF-8 bytes")
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

    override fun close() {
        source.close()
    }
}

expect class StringCharacterProvider(s: String) : CharacterProvider

interface KeyboardInput {
    fun readString(prompt: String): String?
}

expect fun makeKeyboardInput(): KeyboardInput

expect fun openFile(name: String): ByteProvider
expect fun openCharFile(name: String): CharacterProvider

interface CharacterOutput {
    fun writeString(s: String)
}

class NullCharacterOutput : CharacterOutput {
    override fun writeString(s: String) = Unit
}

class StringBuilderOutput : CharacterOutput {
    val buf = StringBuilder()
    var curr = ""

    override fun writeString(s: String) {
        buf.append(s)
        curr = curr + s
    }
}

fun fileExists(path: String) = fileType(path) != null

expect fun fileType(path: String): FileNameType?

class PathUtils {
    companion object {
        fun cleanupPathName(path: String): String {
            var i = path.length
            while (i > 0 && path[i - 1] == '/') {
                i--
            }
            return path.substring(0, i)
        }

        fun isAbsolutePath(path: String) = path.isNotEmpty() && path[0] == '/'
    }
}

enum class FileNameType {
    FILE,
    DIRECTORY,
    UNDEFINED
}

data class PathEntry(val name: String, val size: Long, val type: FileNameType)

expect fun readDirectoryContent(dirName: String): List<PathEntry>
