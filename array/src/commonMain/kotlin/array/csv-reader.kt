package array.csv

import array.*

class CsvParseException(msg: String) : Exception(msg)

fun readCsv(source: CharacterProvider): APLValue {
    val rows = ArrayList<List<APLValue>>()
    while (true) {
        val row = readRow(source) ?: break
        rows.add(row)
    }

    val width = rows.maxValueBy { it.size }
    return APLArrayImpl(intArrayOf(rows.size, width)) { index ->
        val rowIndex = index / width
        val colIndex = index % width
        val row = rows[rowIndex]
        if (colIndex >= row.size) {
            APLNullValue()
        } else {
            row[colIndex]
        }
    }
}

private fun readRow(source: CharacterProvider): List<APLValue>? {
    return null
}

//private fun readRow(source: CharacterProvider): List<APLValue>? {
//    val content = ArrayList<APLValue>()
//    while (true) {
//        val token = readNextToken(source)
//    }
//    return content
//}
//
//private sealed class CsvToken
//private data class StringToken(val value: String) : CsvToken()
//
//private fun readNextElement(source: CharacterProvider): String? {
//    val pushBackProvider = PushBackCharacterProvider(source)
//
//    // Skip initial whitespace
//    while (true) {
//        val ch = pushBackProvider.nextCodepoint() ?: return null
//        if (!isWhitespace(ch)) {
//            break
//        }
//        pushBackProvider.pushBack(ch)
//    }
//
//    val buf = StringBuilder()
//    val ch = pushBackProvider.nextCodepoint() ?: throw IllegalStateException("The push-back list should not be empty")
//    if (ch == '"'.toInt()) {
//        while (true) {
//            val v = pushBackProvider.nextCodepoint() ?: throw CsvParseException("End of file in string")
//            if (v == '\r'.toInt()) {
//                throw CsvParseException("Newline in string")
//            }
//            if (v == '\n'.toInt()) {
//                break
//            }
//            buf.addCodepoint(v)
//
//            // Skip to next comma
//            while(true) {
//                val ch2 = pushBackProvider.nextCodepoint() ?: break
//                if(ch2 == ','.toInt()) {
//                    break
//                }
//            }
//        }
//        return buf.toString()
//    }
//    else {
//        // Collect all characters until the next comma or end of line
//
//    }
//}
