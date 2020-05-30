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

private fun encloseNDim(value: APLValue, renderLabels: Boolean = true): String {
    val dimensions = value.dimensions
    val multipliers = dimensions.multipliers()
    val renderedValues = (0 until value.size).map { index -> String2D(value.valueAt(index).formatted(FormatStyle.PRETTY)) }
    val labelsArray = if (renderLabels) value.labels?.labels else null

    // 4-dimensional rendering may seem a bit backwards, where each 2D block is rendered in row-major style,
    // while the grid of blocks are rendered column-major. This is because for 3D rendering, we want each
    // block to be rendered vertically, and we don't want to transpose this grid just because we add another
    // dimension.

    abstract class LookupActions(
        val s1: Int,
        val s2: Int,
        val s3: Int,
        val s4: Int
    ) {
        abstract fun lookupByCoords(a: Int, b: Int, c: Int, d: Int): Int
    }

    val lookupActions = when (dimensions.size) {
        1 -> object : LookupActions(1, 1, 1, dimensions[0]) {
            override fun lookupByCoords(a: Int, b: Int, c: Int, d: Int) = d
        }
        2 -> object : LookupActions(1, 1, dimensions[0], dimensions[1]) {
            override fun lookupByCoords(a: Int, b: Int, c: Int, d: Int) = c * multipliers[0] + d
        }
        3 -> object : LookupActions(dimensions[0], 1, dimensions[1], dimensions[2]) {
            override fun lookupByCoords(a: Int, b: Int, c: Int, d: Int) = multipliers[0] + c * multipliers[1] + d
        }
        4 -> object : LookupActions(dimensions[1], dimensions[0], dimensions[2], dimensions[3]) {
            override fun lookupByCoords(a: Int, b: Int, c: Int, d: Int): Int =
                b * multipliers[0] + a * multipliers[1] + c * multipliers[2] + d
        }
        else -> TODO("No support for printing higher-dimension arrays")
    }

    // Find the max width of each column
    val colWidths = IntArray(lookupActions.s2 * lookupActions.s4) { 0 }
    for (outerY in 0 until lookupActions.s1) {
        for (innerY in 0 until lookupActions.s3) {
            for (outerX in 0 until lookupActions.s2) {
                for (innerX in 0 until lookupActions.s4) {
                    val index = outerX * lookupActions.s4 + innerX
                    val p = lookupActions.lookupByCoords(outerY, outerX, innerY, innerX)
                    val cell = renderedValues[p]
                    colWidths[index] = max(cell.width(), colWidths[index])
                }
            }
        }
    }

    // Adjust the max widths based on the labels
    val xLabelsArray = ArrayList<String2D?>()
    var maxXLabelsHeight = 0
    if (labelsArray != null) {
        for (innerX in 0 until lookupActions.s4) {
            val text = labelsArray[labelsArray.size - 1]?.get(innerX)
            if (text != null) {
                val s = String2D(text.title)
                xLabelsArray.add(s)
                maxXLabelsHeight = max(s.height(), maxXLabelsHeight)
            } else {
                xLabelsArray.add(null)
            }
        }
        // For now, let's just print the innermost label, repeated s2 times
        for (outerX in 0 until lookupActions.s2) {
            for (innerX in 0 until lookupActions.s4) {
                val index = outerX * lookupActions.s4 + innerX
                val s = xLabelsArray[innerX]
                if (s != null) {
                    colWidths[index] = max(s.width(), colWidths[index])
                }
            }
        }
    }

    val doubleBoxed = dimensions.size > 2
    val allColsWidth = colWidths.sum() + lookupActions.s2 * lookupActions.s4 - 1
    val content = ArrayList<List<String>>()
    content.add(if (doubleBoxed) topBottomRow("╔", "═", "╗", allColsWidth) else topBottomRow("┏", "━", "┓", allColsWidth))
    // Render column labels, if needed
    if (labelsArray != null && maxXLabelsHeight > 0) {
        for (internalRow in 0 until maxXLabelsHeight) {
            val row = ArrayList<String>()
            row.add(if (doubleBoxed) "║" else "┃")
            for (outerX in 0 until lookupActions.s2) {
                for (innerX in 0 until lookupActions.s4) {
                    if (innerX > 0) {
                        row.add(" ")
                    }
                    val text = xLabelsArray[innerX]
                    if (text != null) {
                        val rowText = text.row(internalRow)
                        repeat(colWidths[innerX] - rowText.size) {
                            row.add(" ")
                        }
                        row.addAll(rowText)
                    } else {
                        repeat(colWidths[innerX]) {
                            row.add(" ")
                        }
                    }
                }
            }
            row.add(if (doubleBoxed) "║" else "┃")
            content.add(row)
        }
        content.add(if (doubleBoxed) topBottomRow("╠", "═", "╣", allColsWidth) else topBottomRow("┣", "━", "┫", allColsWidth))
    }

    // Render the actual array content
    for (outerY in 0 until lookupActions.s1) {
        if (outerY > 0) {
            val row = ArrayList<String>()
            row.add(if (doubleBoxed) "║" else "┃")
            for (i in 0 until allColsWidth) {
                row.add(" ")
            }
            row.add(if (doubleBoxed) "║" else "┃")

            content.add(row)
        }
        for (innerY in 0 until lookupActions.s3) {
            // Find the highest cell, and make all cells this size
            var numInternalRows = 0
            for (outerX in 0 until lookupActions.s2) {
                for (innerX in 0 until lookupActions.s4) {
                    val cell = renderedValues[lookupActions.lookupByCoords(outerY, outerX, innerY, innerX)]
                    numInternalRows = max(cell.height(), numInternalRows)
                }
            }

            // Iterate over the internal rows and render each cell
            for (internalRow in 0 until numInternalRows) {
                val row = ArrayList<String>()
                row.add(if (doubleBoxed) "║" else "┃")
                for (outerX in 0 until lookupActions.s2) {
                    if (outerX > 0) {
                        row.add("│")
                    }
                    for (innerX in 0 until lookupActions.s4) {
                        if (innerX > 0) {
                            row.add(" ")
                        }
                        val cell = renderedValues[lookupActions.lookupByCoords(outerY, outerX, innerY, innerX)]
                        val index = outerX * lookupActions.s4 + innerX
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

fun renderStringValue(value: APLValue, style: FormatStyle): String {
    return when (style) {
        FormatStyle.PLAIN -> renderStringValueOptionalQuotes(value, false)
        FormatStyle.PRETTY -> renderStringValueOptionalQuotes(value, true)
        FormatStyle.READABLE -> renderStringValueOptionalQuotes(value, true)
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
        value is APLSingleValue -> value.formatted(FormatStyle.PRETTY)
        value.rank == 0 -> encloseString(String2D(value.valueAt(0).formatted(FormatStyle.PRETTY)))
        else -> encloseNDim(value)
    }
}
