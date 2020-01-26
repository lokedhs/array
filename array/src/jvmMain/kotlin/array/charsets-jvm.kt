package array

actual fun isLetter(codepoint: Int): Boolean {
    return Character.isLetter(codepoint)
}

actual fun isDigit(codepoint: Int): Boolean {
    return Character.isDigit(codepoint)
}

actual fun isWhitespace(codepoint: Int): Boolean {
    return Character.isWhitespace(codepoint)
}

actual fun StringBuilder.addCodepoint(codepoint: Int): StringBuilder {
    return this.appendCodePoint(codepoint)
}
