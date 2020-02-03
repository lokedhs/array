package array

actual fun isLetter(codepoint: Int) : Boolean {
    assert(codepoint < 0x10000)
    val lowCodepoint = codepoint.toChar()
    return lowCodepoint.isLetter()
}

actual fun isDigit(codepoint: Int) : Boolean {
    assert(codepoint < 0x10000)
    val lowCodepoint = codepoint.toChar()
    return lowCodepoint.isDigit()
}

actual fun isWhitespace(codepoint: Int) : Boolean {
    assert(codepoint < 0x10000)
    val lowCodepoint = codepoint.toChar()
    return lowCodepoint.isWhitespace()
}

actual fun charToString(codepoint: Int): String {
    assert(codepoint < 0x10000)
    return codepoint.toChar().toString()
}
actual fun StringBuilder.addCodepoint(codepoint: Int): StringBuilder {
    assert(codepoint < 0x10000)
    return this.append(codepoint.toChar())
}
