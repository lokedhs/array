package array

import java.text.BreakIterator

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

actual fun String.asCodepointList(): List<Int> {
    val result = ArrayList<Int>()
    var pos = 0
    while(pos < this.length) {
        result.add(this.codePointAt(pos))
        pos = this.offsetByCodePoints(pos, 1)
    }
    return result
}

actual fun String.asGraphemeList(): List<String> {
    val result = ArrayList<String>()
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(this)
    var start = iterator.first()
    while(true) {
        val end = iterator.next()
        if(end == BreakIterator.DONE) {
            break
        }
        result.add(this.substring(start, end))
        start = end
    }
    return result
}
