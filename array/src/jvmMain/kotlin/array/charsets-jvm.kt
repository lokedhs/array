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

actual fun charToString(codepoint: Int): String {
    return Character.toString(codepoint)
}

actual fun StringBuilder.addCodepoint(codepoint: Int): StringBuilder {
    return this.appendCodePoint(codepoint)
}
