package array.rendertext

import array.*
import kotlin.math.max

class String2D {
    private val content: List<List<String>>
    private val width: Int

    constructor(value: String) {
        content = value.split('\n').map { line -> line.asGraphemeList() }
        width = computeLongestRow(content)
    }

    constructor(initContent: List<List<String>>) {
        content = initContent
        width = computeLongestRow(initContent)
    }

    fun width() = width
    fun height() = content.size
    fun row(i: Int) = if (i < content.size) content[i] else emptyList()

    private fun computeLongestRow(rows: List<List<String>>): Int {
        return rows.map { it.size }.reduceWithInitial(0) { a, b -> max(a, b) }
    }

    fun encloseInBox(): String2D {
        val newContent = ArrayList<List<String>>()
        ArrayList<String>().let { row ->
            row.add("┏")
            repeat(width) { row.add("━") }
            row.add("┓")
            newContent.add(row)
        }
        content.forEach { row ->
            val newRow = ArrayList<String>()
            newRow.add("┃")
            newRow.addAll(row)
            repeat(width - row.size) { newRow.add(" ") }
            newRow.add("┃")
            newContent.add(newRow)
        }
        ArrayList<String>().let { row ->
            row.add("┗")
            repeat(width) { row.add("━") }
            row.add("┛")
            newContent.add(row)
        }
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

    val indexes = when (dimensions.size) {
        1 -> arrayOf(null, null, null, 0)
        2 -> arrayOf(null, null, 0, 1)
        3 -> arrayOf(0, null, 1, 2)
        4 -> arrayOf(1, 0, 2, 3)
        else -> TODO("No support for printing higher-dimension arrays")
    }
    val s0 = if (indexes[0] != null) dimensions[indexes[0]!!] else 1
    val s1 = if (indexes[1] != null) dimensions[indexes[1]!!] else 1
    val s2 = if (indexes[2] != null) dimensions[indexes[2]!!] else 1
    val s3 = if (indexes[3] != null) dimensions[indexes[3]!!] else 1

    fun lookupByCoords(a: Int, b: Int, c: Int, d: Int): Int {
        return (if (indexes[0] != null) multipliers[indexes[0]!!] * a else 0) +
                (if (indexes[1] != null) multipliers[indexes[1]!!] * b else 0) +
                (if (indexes[2] != null) multipliers[indexes[2]!!] * c else 0) +
                d
    }

    // Compute labels by the Y axis
    var yLabelsWidth = 0
    val yLabelsArray = ArrayList<String2D?>()
    if (labelsArray != null && indexes[2] != null) {
        repeat(s2) { innerY ->
            val text = labelsArray[indexes[2]!!]?.get(innerY)
            if (text != null) {
                val s = String2D(text.title)
                yLabelsArray.add(s)
                yLabelsWidth = max(s.width(), yLabelsWidth)
            } else {
                yLabelsArray.add(null)
            }
        }
    }

    // Find the max width of each column
    val colWidths = IntArray(s1 * s3) { 0 }
    for (outerY in 0 until s0) {
        for (innerY in 0 until s2) {
            for (outerX in 0 until s1) {
                for (innerX in 0 until s3) {
                    val index = outerX * s3 + innerX
                    val p = lookupByCoords(outerY, outerX, innerY, innerX)
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
        for (innerX in 0 until s3) {
            val text = labelsArray[indexes[3]!!]?.get(innerX)
            if (text != null) {
                val s = String2D(text.title)
                xLabelsArray.add(s)
                maxXLabelsHeight = max(s.height(), maxXLabelsHeight)
            } else {
                xLabelsArray.add(null)
            }
        }
        // For now, let's just print the innermost label, repeated s2 times
        for (outerX in 0 until s1) {
            for (innerX in 0 until s3) {
                val index = outerX * s3 + innerX
                val s = xLabelsArray[innerX]
                if (s != null) {
                    colWidths[index] = max(s.width(), colWidths[index])
                }
            }
        }
    }

    fun widthOfOuterCol(colIndex: Int): Int {
        var n = 0
        repeat(s3) { innerX ->
            val index = colIndex * s3 + innerX
            n += colWidths[index]
        }
        return n + s3 - 1
    }

    val doubleBoxed = dimensions.size > 2
    val allColsWidth = colWidths.sum() + s1 * s3 - 1
    val content = ArrayList<List<String>>()

    // Render column labels, if needed
    if (labelsArray != null && maxXLabelsHeight > 0) {
        ArrayList<String>().let { row ->
            row.add(if (doubleBoxed) "╔" else "┏")
            if (yLabelsWidth > 0) {
                repeat(yLabelsWidth) { row.add(if (doubleBoxed) "═" else "━") }
                row.add(if (doubleBoxed) "╦" else "┳")
            }
            repeat(s1) { outerX ->
                if (outerX > 0) {
                    row.add(if (doubleBoxed) "╤" else "┳")
                }
                repeat(widthOfOuterCol(outerX)) { row.add(if (doubleBoxed) "═" else "━") }
            }
            row.add(if (doubleBoxed) "╗" else "┓")
            content.add(row)
        }
        for (internalRow in 0 until maxXLabelsHeight) {
            val row = ArrayList<String>()
            row.add(if (doubleBoxed) "║" else "┃")
            if (yLabelsWidth > 0) {
                repeat(yLabelsWidth) { row.add(" ") }
                row.add(if (doubleBoxed) "║" else "┃")
            }
            repeat(s1) { outerX ->
                if (outerX > 0) {
                    row.add("│")
                }
                repeat(s3) { innerX ->
                    if (innerX > 0) {
                        row.add(" ")
                    }
                    val text = xLabelsArray[innerX]
                    val index = outerX * s3 + innerX
                    if (text != null) {
                        val rowText = text.row(internalRow)
                        repeat(colWidths[index] - rowText.size) { row.add(" ") }
                        row.addAll(rowText)
                    } else {
                        repeat(colWidths[index]) { row.add(" ") }
                    }
                }
            }
            row.add(if (doubleBoxed) "║" else "┃")
            content.add(row)
        }
        ArrayList<String>().let { row ->
            row.add(if (doubleBoxed) "╠" else "┣")
            if (yLabelsWidth > 0) {
                repeat(yLabelsWidth) { row.add(if (doubleBoxed) "═" else "━") }
                row.add(if (doubleBoxed) "╬" else "╋")
            }
            repeat(s1) { outerX ->
                if (outerX > 0) {
                    row.add(if (doubleBoxed) "╧" else "┻")
                }
                repeat(widthOfOuterCol(outerX)) { row.add(if (doubleBoxed) "═" else "━") }
            }
            row.add(if (doubleBoxed) "╣" else "┫")
            content.add(row)
        }
    } else {
        ArrayList<String>().let { row ->
            // No top labels, but has left labels
            row.add(if (doubleBoxed) "╔" else "┏")
            if (yLabelsWidth > 0) {
                repeat(yLabelsWidth) { row.add(if (doubleBoxed) "═" else "━") }
                row.add(if (doubleBoxed) "╦" else "┳")
            }
            repeat(allColsWidth) { row.add(if (doubleBoxed) "═" else "━") }
            row.add(if (doubleBoxed) "╗" else "┓")
            content.add(row)
        }
    }
    // "left0" "left1" labels[2] 2 2 2 3 ⍴ ⍳1000
    // "left0" "left1" labels[2] "foo" "bar" "abc" labels[3] 2 2 2 3 ⍴ ⍳1000

    // Render the actual array content
    for (outerY in 0 until s0) {
        if (outerY > 0) {
            val row = ArrayList<String>()
            row.add(if (doubleBoxed) "║" else "┃")
            if (yLabelsWidth > 0) {
                repeat(yLabelsWidth) { row.add(" ") }
                row.add(if (doubleBoxed) "║" else "┃")
            }
            repeat(allColsWidth) { row.add(" ") }
            row.add(if (doubleBoxed) "║" else "┃")

            content.add(row)
        }
        for (innerY in 0 until s2) {
            // Find the highest cell, and make all cells this size
            var numInternalRows = 0
            for (outerX in 0 until s1) {
                for (innerX in 0 until s3) {
                    val cell = renderedValues[lookupByCoords(outerY, outerX, innerY, innerX)]
                    numInternalRows = max(cell.height(), numInternalRows)
                }
            }
            if (yLabelsWidth > 0) {
                numInternalRows = max(yLabelsArray[innerY]?.height() ?: 0, numInternalRows)
            }

            // Iterate over the internal rows and render each cell
            for (internalRow in 0 until numInternalRows) {
                val row = ArrayList<String>()
                row.add(if (doubleBoxed) "║" else "┃")
                if (yLabelsWidth > 0) {
                    val text = yLabelsArray[innerY]
                    if (text != null) {
                        val rowText = text.row(internalRow)
                        repeat(yLabelsWidth - rowText.size) { row.add(" ") }
                        row.addAll(rowText)
                    } else {
                        repeat(yLabelsWidth) { row.add(" ") }
                    }
                    row.add(if (doubleBoxed) "║" else "┃")
                }
                for (outerX in 0 until s1) {
                    if (outerX > 0) {
                        row.add("│")
                    }
                    for (innerX in 0 until s3) {
                        if (innerX > 0) {
                            row.add(" ")
                        }
                        val cell = renderedValues[lookupByCoords(outerY, outerX, innerY, innerX)]
                        val index = outerX * s3 + innerX
                        rightJustified(row, cell.row(internalRow), colWidths[index])
                    }
                }
                row.add(if (doubleBoxed) "║" else "┃")
                content.add(row)
            }
        }
    }
    ArrayList<String>().let { row ->
        row.add(if (doubleBoxed) "╚" else "┗")
        if (yLabelsWidth > 0) {
            repeat(yLabelsWidth) { row.add(if (doubleBoxed) "═" else "━") }
            row.add(if (doubleBoxed) "╩" else "┻")
        }
        repeat(allColsWidth) { row.add(if (doubleBoxed) "═" else "━") }
        row.add(if (doubleBoxed) "╝" else "┛")
        content.add(row)
    }
    return String2D(content).asString()
}

fun rightJustified(dest: MutableList<String>, s: List<String>, width: Int) {
    for (i in 0 until width - s.size) {
        dest.add(" ")
    }
    dest.addAll(s)
}

fun maybeWrapInParens(buf: Appendable, value: APLValue) {
    val shouldWrap = value.formattedAsCodeRequiresParens()
    if (shouldWrap) {
        buf.append("(")
    }
    buf.append(value.formatted(FormatStyle.READABLE))
    if (shouldWrap) {
        buf.append(")")
    }
}

fun renderStringValue(value: APLValue, style: FormatStyle): String {
    return when (style) {
        FormatStyle.PLAIN -> renderStringValueOptionalQuotes(value, false)
        FormatStyle.PRETTY -> renderStringValueOptionalQuotes(value, true)
        FormatStyle.READABLE -> renderStringValueOptionalQuotes(value, true)
    }
}

fun renderStringValueOptionalQuotes(value: APLValue, showQuotes: Boolean): String {
    val buf = StringBuilder()
    if (showQuotes) {
        buf.append("\"")
    }
    for (i in 0 until value.size) {
        val v = value.valueAt(i)
        if (v !is APLChar) {
            throw IllegalStateException("String contain non-chars")
        }
        val ch = v.value
        when {
            ch == '"'.code && showQuotes -> buf.append("\\\"")
            else -> buf.addCodepoint(ch)
        }
    }
    if (showQuotes) {
        buf.append("\"")
    }
    return buf.toString()
}

fun renderNullValue(style: FormatStyle): String {
    return when (style) {
        FormatStyle.PLAIN -> ""
        FormatStyle.PRETTY -> "⍬"
        FormatStyle.READABLE -> "⍬"
    }
}

fun encloseInBox(value: APLValue, style: FormatStyle): String {
    return when {
        value is APLSingleValue -> value.formatted(style)
        value.isScalar() -> encloseString(String2D(value.valueAt(0).formatted(style)))
        isNullValue(value) -> renderNullValue(style)
        value.isStringValue() -> renderStringValue(value, style)
        else -> encloseNDim(value)
    }
}
