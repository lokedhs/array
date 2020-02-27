package array

expect fun isLetter(codepoint: Int): Boolean
expect fun isDigit(codepoint: Int): Boolean
expect fun isWhitespace(codepoint: Int): Boolean
expect fun charToString(codepoint: Int): String

expect fun StringBuilder.addCodepoint(codepoint: Int): StringBuilder
expect fun String.asCodepointList(): List<Int>
expect fun String.asGraphemeList(): List<String>
