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
            repeat(width - row.size) { newRow.add(" ") }
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

private fun encloseString(s: String2D): String {
    return s.encloseInBox().asString()
}

private fun encloseNDim(value: APLValue): String {
    val dimensions = value.dimensions
    val multipliers = dimensions.multipliers()
    val renderedValues = (0 until value.size).map { index -> String2D(value.valueAt(index).formatted(APLValue.FormatStyle.PRETTY)) }

    // 4-dimensional rendering may seem a bit backwards, where each 2D block is rendered in row-major style,
    // while the grid of blocks are rendered column-major. This is because for 3D rendering, we want each
    // block to be rendered vertically, and we don't want to transpose this grid just because we add another
    // dimension.

    val lookupByCoords: (Int, Int, Int, Int) -> Int = when (dimensions.size) {
        1 -> { a, b, c, d -> d }
        2 -> { a, b, c, d -> c * multipliers[0] + d }
        3 -> { a, b, c, d -> a * multipliers[0] + c * multipliers[1] + d }
        4 -> { a, b, c, d -> b * multipliers[0] + a * multipliers[1] + c * multipliers[2] + d }
        else -> TODO("No support for printing higher-dimension arrays")
    }
    val (s1, s2, s3, s4) = when (dimensions.size) {
        1 -> listOf(1, 1, 1, dimensions[0])
        2 -> listOf(1, 1, dimensions[0], dimensions[1])
        3 -> listOf(dimensions[0], 1, dimensions[1], dimensions[2])
        4 -> listOf(dimensions[1], dimensions[0], dimensions[2], dimensions[3])
        else -> TODO("No support for printing higher-dimension arrays")
    }

    val colWidths = IntArray(s2 * s4) { 0 }
    for (outerY in 0 until s1) {
        for (innerY in 0 until s3) {
            for (outerX in 0 until s2) {
                for (innerX in 0 until s4) {
                    val index = outerX * s4 + innerX
                    val p = lookupByCoords(outerY, outerX, innerY, innerX)
                    val cell = renderedValues[p]
                    colWidths[index] = max(cell.width(), colWidths[index])
                }
            }
        }
    }

    val doubleBoxed = dimensions.size > 2
    val allColsWidth = colWidths.sum() + s2 * s4 - 1
    val content = ArrayList<List<String>>()
    content.add(if (doubleBoxed) topBottomRow("╔", "═", "╗", allColsWidth) else topBottomRow("┏", "━", "┓", allColsWidth))
    for (outerY in 0 until s1) {
        if (outerY > 0) {
            val row = ArrayList<String>()
            row.add(if (doubleBoxed) "║" else "┃")
            for (i in 0 until allColsWidth) {
                row.add(" ")
            }
            row.add(if (doubleBoxed) "║" else "┃")

            content.add(row)
        }
        for (innerY in 0 until s3) {
            // Find the highest cell, and make all cells this size
            var numInternalRows = 0
            for (outerX in 0 until s2) {
                for (innerX in 0 until s4) {
                    val cell = renderedValues[lookupByCoords(outerY, outerX, innerY, innerX)]
                    numInternalRows = max(cell.height(), numInternalRows)
                }
            }

            // Iterate over the internal rows and render each cell
            for (internalRow in 0 until numInternalRows) {
                val row = ArrayList<String>()
                row.add(if (doubleBoxed) "║" else "┃")
                for (outerX in 0 until s2) {
                    if (outerX > 0) {
                        row.add("│")
                    }
                    for (innerX in 0 until s4) {
                        if (innerX > 0) {
                            row.add(" ")
                        }
                        val cell = renderedValues[lookupByCoords(outerY, outerX, innerY, innerX)]
                        val index = outerX * s4 + innerX
                        rightJustified(row, cell.row(internalRow), colWidths[index])
                    }
                }
                row.add(if (doubleBoxed) "║" else "┃")
                content.add(row)
            }
        }
    }
    content.add(if (doubleBoxed) topBottomRow("╚", "═", "╝", allColsWidth) else topBottomRow("┗", "━", "┛", allColsWidth))
    return String2D(content).asString()
}

fun rightJustified(dest: MutableList<String>, s: List<String>, width: Int) {
    for (i in 0 until width - s.size) {
        dest.add(" ")
    }
    dest.addAll(s)
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

fun renderStringValue(value: APLValue, style: APLValue.FormatStyle): String {
    return when (style) {
        APLValue.FormatStyle.PLAIN -> renderStringValueOptionalQuotes(value, false)
        APLValue.FormatStyle.PRETTY -> renderStringValueOptionalQuotes(value, true)
        APLValue.FormatStyle.READABLE -> renderStringValueOptionalQuotes(value, true)
    }
}

private fun renderStringValueOptionalQuotes(value: APLValue, showQuotes: Boolean): String {
    val buf = StringBuilder()
    if (showQuotes) {
        buf.append("\"")
    }
    for (i in 0 until value.size) {
        val v = value.valueAt(i)
        if (v is APLChar) {
            buf.addCodepoint(v.value)
        } else {
            throw IllegalStateException("String contain non-chars")
        }
    }
    if (showQuotes) {
        buf.append("\"")
    }
    return buf.toString()
}

fun renderNullValue(): String {
    return "⍬"
}

fun encloseInBox(value: APLValue): String {
    return when {
        value is APLSingleValue -> value.formatted(APLValue.FormatStyle.PRETTY)
        value.rank == 0 -> encloseString(String2D(value.valueAt(0).formatted(APLValue.FormatStyle.PRETTY)))
        else -> encloseNDim(value)
    }
}
