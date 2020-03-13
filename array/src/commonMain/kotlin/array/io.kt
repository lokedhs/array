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

interface CharacterProvider {
    fun nextCodepoint(): Int?
    fun close()

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

class PushBackCharacterProvider(val source: CharacterProvider) : CharacterProvider {
    private val pushBackList = ArrayList<Int>()

    override fun nextCodepoint(): Int? {
        return if (pushBackList.isNotEmpty()) {
            pushBackList.removeAt(pushBackList.size - 1)
        } else {
            source.nextCodepoint()
        }
    }

    fun pushBack(ch: Int) {
        pushBackList.add(ch)
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
