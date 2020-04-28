package array

actual fun isLetter(codepoint: Int): Boolean {
    return if (codepoint < 0x10000) {
        codepoint.toChar().isLetter()
    } else {
        false
    }
}

actual fun isDigit(codepoint: Int): Boolean {
    return if (codepoint < 0x10000) {
        codepoint.toChar().isDigit()
    } else {
        false
    }
}

actual fun isWhitespace(codepoint: Int): Boolean {
    return if (codepoint < 0x10000) {
        codepoint.toChar().isWhitespace()
    } else {
        false
    }
}

actual fun charToString(codepoint: Int): String {
    try {
        return String(Char.toChars(codepoint))
    } catch (e: IllegalArgumentException) {
        println("Failed to convert ${codepoint} to string")
        throw e
    }
}

actual fun StringBuilder.addCodepoint(codepoint: Int): StringBuilder {
    val v = Char.toChars(codepoint)
    v.forEach {
        this.append(it)
    }
    return this
}

actual fun String.asCodepointList(): List<Int> {
    val result = ArrayList<Int>()
    var i = 0
    while (i < this.length) {
        val ch = this[i++]
        val v = when {
            ch.isHighSurrogate() -> {
                val low = this[i++]
                if (low.isLowSurrogate()) {
                    Char.toCodePoint(ch, low)
                } else {
                    throw IllegalStateException("Expected low surrogate, got: ${low.toInt()}")
                }
            }
            ch.isLowSurrogate() -> throw IllegalStateException("Standalone low surrogate found: ${ch.toInt()}")
            else -> ch.toInt()
        }
        result.add(v)
    }
    return result
}

actual fun String.asGraphemeList(): List<String> {
    // TODO: Need ICU for this
    return this.asCodepointList().map(::charToString)
}
