package array.rendertext

import array.*
import kotlin.math.max

private class String2D {
    private val content: List<List<String>>
    private val width: Int

    constructor(value: String) {
        val list = ArrayList<List<String>>()
        value.split('\n').forEach { line ->
            val chars = line.asGraphemeList()
            list.add(chars)
        }
        content = list
        width = computeLongestRow(list)
    }

    constructor(initContent: List<List<String>>) {
        content = initContent
        width = computeLongestRow(initContent)
    }

    fun width() = width
    fun height() = content.size
    fun row(i: Int) = if (i < content.size) content[i] else emptyList()

    private fun computeLongestRow(rows: List<List<String>>): Int {
        return rows.map { it.size }.reduceWithInitial({ a, b -> max(a, b) }, 0)
    }

    fun encloseInBox(): String2D {
        val newContent = ArrayList<List<String>>()
        newContent.add(topBottomRow("┏", "━", "┓", width))
        content.forEach { row ->
            val newRow = ArrayList<String>()
            newRow.add("┃")
            newRow.addAll(row)
            (0 until (width - row.size)).forEach { newRow.add(" ") }
            newRow.add("┃")
            newContent.add(newRow)
        }
        newContent.add(topBottomRow("┗", "━", "┛", width))
        return String2D(newContent)
    }

    fun asString(): String {
        val buf = StringBuilder()
        content.forEachIndexed { i, row ->
            row.forEach { character ->
                buf.append(character)
            }
            if (i < content.size - 1) {
                buf.append("\n")
            }
        }
        return buf.toString()
    }
}

private fun encloseString(string: String): String {
    val s = String2D(string)
    val s2d = s.encloseInBox()
    return s2d.asString()
}

private fun enclose1D(value: APLValue): String {
    val elements = ArrayList<String2D>()
    var height = 0
    var width = 0
    for (i in 0 until value.size()) {
        val e = value.valueAt(i)
        val s2 = String2D(e.formatted())
        height = max(height, s2.height())
        width += s2.width()
        elements.add(s2)
    }

    val content = ArrayList<List<String>>()
    content.add(topBottomRow("┏", "━", "┓", width + elements.size - 1)) // account for one space between each element
    for (y in 0 until height) {
        val rowContent = ArrayList<String>()
        rowContent.add("┃")
        for (x in 0 until elements.size) {
            val s2 = elements[x]
            val elementRow = s2.row(y)
            rowContent.addAll(elementRow)
            (0 until s2.width() - elementRow.size).forEach { _ -> rowContent.add(" ") }
            if (x < elements.size - 1) {
                rowContent.add(" ")
            }
        }
        rowContent.add("┃")
        content.add(rowContent)
    }
    content.add(topBottomRow("┗", "━", "┛", width + elements.size - 1))
    return String2D(content).asString()
}

private fun enclose2D(value: APLValue): String {
    val (numRows, numCols) = value.dimensions()
    val colWidths = Array(numCols) { 0 }
    val rows = ArrayList<List<String2D>>()
    for (y in 0 until numRows) {
        val row = ArrayList<String2D>()
        for (x in 0 until numCols) {
            val s2 = String2D(value.valueAt(y * numCols + x).formatted())
            row.add(s2)
            colWidths[x] = max(colWidths[x], s2.width())
        }
        rows.add(row)
    }

    val content = ArrayList<List<String>>()
    for(y in 0 until numRows) {
        val row = rows[y]

        val numInternalRows = row.maxValueBy { it.height() }

        for(internalRowIndex in 0 until numInternalRows) {
            val rowContent = ArrayList<String>()
            for (x in 0 until numCols) {
                val v = row[x]
                if (x > 0) {
                    rowContent.add(" ")
                }

                if(internalRowIndex < v.height()) {
                    val inner = v.row(internalRowIndex)
                    for(i in 0 until colWidths[x] - v.width()) {
                        rowContent.add(" ")
                    }
                    rowContent.addAll(inner)
                }
                else {
                    for (i in 0 until colWidths[x]) {
                        rowContent.add(" ")
                    }
                }
            }
            content.add(rowContent)
        }
    }

    return String2D(content).asString()
}

private fun topBottomRow(left: String, middle: String, right: String, width: Int): List<String> {
    val row = ArrayList<String>()
    row.add(left)
    (0 until width).forEach { _ ->
        row.add(middle)
    }
    row.add(right)
    return row
}

fun encloseInBox(value: APLValue): String {
    return when {
        value is APLSingleValue -> value.formatted()
        value.rank() == 0 -> encloseString(value.valueAt(0).formatted())
        value.rank() == 1 -> enclose1D(value)
        value.rank() == 2 -> enclose2D(value)
        else -> TODO("not implemented")
    }
}
