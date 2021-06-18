package array.gui

import array.*
import array.gui.styledarea.EditorContent
import array.gui.styledarea.StringEditorContentEntry
import array.gui.styledarea.TextStyle
import array.rendertext.renderStringValueOptionalQuotes
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Text
import java.util.*

class Array2ContentEntry(val value: APLValue) : EditorContent {
    private val textContent = value.formatted(FormatStyle.PLAIN)

    override fun length() = textContent.length
    override fun createNode(renderContext: ClientRenderContext, style: TextStyle) = makeArrayNode(value)
    override fun joinSegment(nextSeg: EditorContent): Optional<EditorContent> = Optional.empty()
    override fun realGetText() = textContent
    override fun realCharAt(index: Int) = throw IllegalStateException("Can't get character array element")

    override fun realSubsequence(start: Int, end: Int): EditorContent {
        return if (start == 0 && end == textContent.length) {
            this
        } else {
            StringEditorContentEntry(textContent.substring(start, end))
        }
    }
}

fun makeArrayNode(value: APLValue): Node {
    val d = value.dimensions
    return when {
        value.isStringValue() -> makeStringDisp(value)
        d.size == 1 -> makeArray1(value)
        d.size == 2 -> makeArray2(value)
        else -> Text(value.formatted(FormatStyle.PRETTY)).apply { styleClass.add("kapresult") }
    }
}

private fun makeArray1(value: APLValue): Node {
    val grid = GridPane()
    grid.border = Border(BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths(1.0)))
    value.iterateMembersWithPosition { v, i ->
        val label = makeArrayNode(v)
        GridPane.setRowIndex(label, 0)
        GridPane.setColumnIndex(label, i)
        GridPane.setMargin(label, Insets(3.0, 3.0, 3.0, 3.0))
        GridPane.setHalignment(label, HPos.RIGHT)
        grid.children.add(label)
    }
    return grid
}

private fun makeArray2(value: APLValue): Node {
    val dimensions = value.dimensions
    val grid = GridPane()
    grid.border = Border(BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths(1.0)))

    val labelState = DimensionLabels.computeLabelledAxis(value)

    val numRows = dimensions[0]
    val numCols = dimensions[1]
    val rowOffset = if (labelState[1]) 1 else 0
    val colOffset = if (labelState[0]) 1 else 0
    val multipliers = dimensions.multipliers()

    if (labelState[1]) {
        value.labels?.let { labels ->
            labels.labels[1]?.let { axis1Labels ->
                axis1Labels.forEachIndexed { i, axisLabel ->
                    if (axisLabel != null) {
                        val label = Text(axisLabel.title).apply {
                            styleClass.add("kapresult-header")
                        }
                        GridPane.setRowIndex(label, 0)
                        GridPane.setColumnIndex(label, i + colOffset)
                        GridPane.setMargin(label, Insets(3.0, 3.0, 3.0, 3.0))
                        GridPane.setHalignment(label, HPos.CENTER)
                        grid.children.add(label)
                    }
                }
            }
        }
    }

    repeat(numRows) { rowIndex ->
        if (labelState[0]) {
            value.labels?.let { labels ->
                labels.labels[0]?.let { axis0Labels ->
                    val axisLabel = axis0Labels[rowIndex]
                    if (axisLabel != null) {
                        val rowLabel = Text(axisLabel.title).apply {
                            styleClass.add("kapresult-header")
                        }
                        GridPane.setRowIndex(rowLabel, rowIndex + rowOffset)
                        GridPane.setColumnIndex(rowLabel, 0)
                        GridPane.setMargin(rowLabel, Insets(3.0, 3.0, 3.0, 3.0))
                        GridPane.setHalignment(rowLabel, HPos.LEFT)
                        grid.children.add(rowLabel)
                    }
                }
            }
        }
        repeat(numCols) { colIndex ->
            val label = makeArrayNode(value.valueAt(dimensions.indexFromPosition(intArrayOf(rowIndex, colIndex), multipliers)))
            GridPane.setRowIndex(label, rowIndex + rowOffset)
            GridPane.setColumnIndex(label, colIndex + colOffset)
            GridPane.setMargin(label, Insets(3.0, 3.0, 3.0, 3.0))
            GridPane.setHalignment(label, HPos.RIGHT)
            grid.children.add(label)
        }
    }

    return grid
}

private fun makeStringDisp(value: APLValue): Node {
    return Text(renderStringValueOptionalQuotes(value, true)).apply { styleClass.add("kapresult") }
}
