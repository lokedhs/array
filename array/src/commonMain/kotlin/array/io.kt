package array

class MPFileException(message: String, cause: Exception? = null) : Exception(message, cause)

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
    fun readByte(): Byte?
    fun readBlock(buffer: ByteArray, start: Int? = null, length: Int? = null): Int?
}

interface CharacterProvider : NativeCloseable {
    fun sourceName(): String?
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
}

@OptIn(ExperimentalStdlibApi::class)
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

expect fun fileExists(path: String): Boolean

class PathUtils {
    companion object {
        fun cleanupPathName(path: String): String {
            var i = path.length
            while (i > 0 && path[i - 1] == '/') {
                i--
            }
            return path.substring(0, i)
        }

        fun isAbsolutePath(path: String) = path.length > 0 && path[0] == '/'
    }
}
