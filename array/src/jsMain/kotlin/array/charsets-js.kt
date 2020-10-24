package array

val XRegExp = js("require('xregexp')")
val letterRegexp = XRegExp("^\\p{L}$")
val digitRegexp = XRegExp("^\\p{N}$")
val whitespaceRegexp = XRegExp("^\\p{Zs}$")

actual fun isLetter(codepoint: Int): Boolean {
    return letterRegexp.test(charToString(codepoint)) as Boolean
}

actual fun isDigit(codepoint: Int): Boolean {
    return digitRegexp.test(codepoint.toChar().toString()) as Boolean
}

actual fun isWhitespace(codepoint: Int): Boolean {
    return whitespaceRegexp.test(codepoint.toChar().toString()) as Boolean
}

actual fun charToString(codepoint: Int): String {
    return if (codepoint < 0x10000) {
        codepoint.toChar().toString()
    } else {
        val off = 0xD800 - (0x10000 shr 10)
        val high = off + (codepoint shr 10)
        val low = 0xDC00 + (codepoint and 0x3FF)
        "${high.toChar()}${low.toChar()}"
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
    val off = 0x10000 - (0xD800 shl 10) - 0xDC00
    return (highInt shl 10) + lowInt + off
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

//@JsModule("grapheme-breaker-mjs")
//@JsNonModule
//external object GraphemeBreaker {
//    fun `break`(s: String): Array<String>
//}
val GraphemeSplitter = js("require('grapheme-splitter')")
val graphemeSplitter = GraphemeSplitter()

actual fun String.asGraphemeList(): List<String> {
    val result = graphemeSplitter.splitGraphemes(this) as Array<String>
    return result.asList()
}
