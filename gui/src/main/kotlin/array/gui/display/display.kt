package array.gui.display

import array.*
import array.gui.Client
import array.gui.ClientRenderContext
import array.gui.arrayedit.ArrayEditor
import array.gui.styledarea.EditorContent
import array.gui.styledarea.StringEditorContentEntry
import array.gui.styledarea.TextStyle
import array.rendertext.renderStringValueOptionalQuotes
import javafx.event.EventHandler
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Side
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.input.Clipboard
import javafx.scene.input.DataFormat
import javafx.scene.input.MouseButton
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Text
import java.util.*
import kotlin.reflect.KClass

interface ValueRenderer {
    val value: APLValue
    val text: String
    fun renderValue(): Region
    fun addToMenu(contextMenu: ContextMenu) {}

    companion object {
        private val renderers = hashMapOf<KClass<out APLValue>, (APLValue) -> ValueRenderer>(
            Pair(APLMap::class, { APLMapRenderer(it as APLMap) }))

        fun makeValueRenderer(client: Client, value: APLValue): ValueRenderer {
            val renderer = renderers[value::class]
            return if (renderer != null) {
                renderer(value)
            } else {
                Array2ValueRenderer(client, value)
            }
        }

        fun makeContent(client: Client, value: APLValue): EditorContent {
            return Array2ContentEntry(makeValueRenderer(client, value))
        }
    }
}

class Array2ValueRenderer(private val client: Client, override val value: APLValue) : ValueRenderer {
    override val text = value.formatted(FormatStyle.PLAIN)
    override fun renderValue() = makeArrayNode(client, value)

    override fun addToMenu(contextMenu: ContextMenu) {
        if (value.dimensions.size == 2) {
            val item = MenuItem("Open in editor").apply { onAction = EventHandler { ArrayEditor.open(client, value) } }
            contextMenu.items.add(item)
        }
    }
}

private class Array2ContentEntry(val renderer: ValueRenderer) : EditorContent {
    override fun length() = renderer.text.length

    override fun createNode(renderContext: ClientRenderContext, style: TextStyle): Node {
        val node = renderer.renderValue()
        val contextMenu = ContextMenu(
            MenuItem("Copy as string").apply { onAction = EventHandler { copyAsString() } },
            MenuItem("Copy as code").apply { onAction = EventHandler { copyAsCode() } })
        renderer.addToMenu(contextMenu)
        node.setOnMouseClicked { event ->
            if (event.button == MouseButton.SECONDARY) {
                contextMenu.show(node, Side.RIGHT, -(node.width - event.x), event.y)
            }
        }
        return node
    }

    private fun copyAsString() {
        val clipboard = Clipboard.getSystemClipboard()
        clipboard.setContent(mapOf(DataFormat.PLAIN_TEXT to renderer.value.formatted(FormatStyle.PRETTY)))
    }

    private fun copyAsCode() {
        val clipboard = Clipboard.getSystemClipboard()
        clipboard.setContent(mapOf(DataFormat.PLAIN_TEXT to renderer.value.formatted(FormatStyle.READABLE)))
    }

    override fun joinSegment(nextSeg: EditorContent): Optional<EditorContent> = Optional.empty()
    override fun realGetText() = renderer.text
    override fun realCharAt(index: Int) = throw IllegalStateException("Can't get character array element")

    override fun realSubsequence(start: Int, end: Int): EditorContent {
        val text = renderer.text
        return if (start == 0 && end == text.length) {
            this
        } else {
            StringEditorContentEntry(text.substring(start, end))
        }
    }
}

private fun makeArrayNode(client: Client, value: APLValue): Region {
    fun renderAsString(): Region {
        return Label(value.formatted(FormatStyle.PRETTY)).apply { styleClass.add("kapresult") }
    }

    val d = value.dimensions
    val node = when {
        d.dimensions.any { it > 100 } -> renderOversizeValue(value)
        d.size == 1 && d[0] == 0 -> renderAsString()
        value.isStringValue() -> makeStringDisp(value)
        d.size == 1 -> makeArray1(client, value)
        d.size == 2 -> makeArray2(client, value)
        else -> renderAsString()
    }
    return node
}

private fun renderOversizeValue(value: APLValue): Region {
    val d = value.dimensions
    val s = d.dimensions.joinToString(", ")
    val text = Label("Oversized array: ${s}")
    text.border = Border(BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths(1.0)))
    return text
}

private fun makeArray1(client: Client, value: APLValue): Region {
    val grid = GridPane()
    grid.border = Border(BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths(1.0)))
    value.iterateMembersWithPosition { v, i ->
        val label = makeArrayNode(client, v)
        GridPane.setRowIndex(label, 0)
        GridPane.setColumnIndex(label, i)
        GridPane.setMargin(label, Insets(3.0, 3.0, 3.0, 3.0))
        GridPane.setHalignment(label, HPos.RIGHT)
        grid.children.add(label)
    }
    return grid
}

private fun makeArray2(client: Client, value: APLValue): Region {
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
            val label = makeArrayNode(client, value.valueAt(dimensions.indexFromPosition(intArrayOf(rowIndex, colIndex), multipliers)))
            GridPane.setRowIndex(label, rowIndex + rowOffset)
            GridPane.setColumnIndex(label, colIndex + colOffset)
            GridPane.setMargin(label, Insets(3.0, 3.0, 3.0, 3.0))
            GridPane.setHalignment(label, HPos.RIGHT)
            grid.children.add(label)
        }
    }

    return grid
}

private fun makeStringDisp(value: APLValue): Region {
    return Label(renderStringValueOptionalQuotes(value, true)).apply { styleClass.add("kapresult") }
}
