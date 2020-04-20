package array

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
                        makeCharFromSurrogatePair(ch, low)
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

actual fun makeKeyboardInput(): KeyboardInput {
    TODO("Not implemented")
}

actual fun openFile(name: String): ByteProvider {
    TODO("Not implemented")
}

actual fun openCharFile(name: String): CharacterProvider {
    TODO("Not implemented")
}
