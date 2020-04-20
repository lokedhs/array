package array

actual fun isLetter(codepoint: Int): Boolean {
    val regex = "^\\p{L}$".toRegex()
    return regex.matches(codepoint.toChar().toString())
}

actual fun isDigit(codepoint: Int): Boolean {
    val regex = "^\\p{N}$".toRegex()
    return regex.matches(codepoint.toChar().toString())
}

actual fun isWhitespace(codepoint: Int): Boolean {
    val regex = "^\\p{Zs}$".toRegex()
    return regex.matches(codepoint.toChar().toString())
}

actual fun charToString(codepoint: Int): String {
    if (codepoint < 0x10000) {
        return codepoint.toChar().toString()
    } else {
        throw RuntimeException("JS implementation only supports BMP")
    }
}

actual fun StringBuilder.addCodepoint(codepoint: Int): StringBuilder {
    this.append(charToString(codepoint))
    return this
}

fun makeCharFromSurrogatePair(high: Char, low: Char): Int {
    val highInt = high.toInt()
    val lowInt = low.toInt()
    assertx(highInt in 0xD800..0xDBFF)
    assertx(lowInt in 0xDC00..0xDFFF)
    return ((highInt - 0xD800) * 0xD400) + (lowInt - 0xDC00)
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
                    makeCharFromSurrogatePair(ch, low)
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
