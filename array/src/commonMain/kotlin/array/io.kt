package array

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

expect fun readFile(name: String): CharacterProvider
