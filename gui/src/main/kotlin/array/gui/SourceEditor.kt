package array.gui

import array.Position
import array.SourceLocation
import array.StringCharacterProvider
import array.gui.styledarea.KAPEditorStyledArea
import array.gui.styledarea.TextStyle
import javafx.event.Event
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.control.ToolBar
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.TextFlow
import javafx.stage.Stage
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.StyledTextArea
import org.fxmisc.richtext.TextExt
import org.fxmisc.richtext.model.*
import org.fxmisc.wellbehaved.event.EventPattern
import org.fxmisc.wellbehaved.event.InputMap
import java.io.File
import java.io.FileWriter
import java.lang.ref.WeakReference
import java.nio.charset.StandardCharsets
import java.util.function.BiConsumer
import java.util.function.Function

class SourceEditor(val client: Client) {
    private val stage = Stage()
    private var styledArea: SourceEditorStyledArea
    private var loaded: File? = null
    private val messageArea = TextField()

    init {
        val vbox = VBox()

        val toolbar = ToolBar(
            makeToolbarButton("Save", this::saveClicked),
            makeToolbarButton("Run", this::runClicked))
        vbox.children.add(toolbar)

        styledArea = initStyledArea()
        val scrollArea = VirtualizedScrollPane(styledArea)
        vbox.children.add(scrollArea)
        VBox.setVgrow(scrollArea, Priority.ALWAYS)

        vbox.children.add(messageArea)

        stage.scene = Scene(vbox, 1200.0, 800.0)

        client.sourceEditors.add(this)
        stage.onCloseRequest = EventHandler { client.sourceEditors.remove(this) }
    }

    private var highlightedRow: Int? = null

    private fun initStyledArea(): SourceEditorStyledArea {
        @Suppress("UNUSED_ANONYMOUS_PARAMETER")
        val applyParagraphStyle = BiConsumer<TextFlow, SourceEditorParStyle> { flow, parStyle ->
            flow.background = when (parStyle.type) {
                SourceEditorParStyle.StyleType.NORMAL -> Background.EMPTY
                SourceEditorParStyle.StyleType.ERROR -> Background(BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY))
            }
        }
        val nodeFactory = Function<StyledSegment<String, TextStyle>, Node> { seg ->
            val applyStyle = { a: TextExt, b: TextStyle ->
                b.styleContent(a, client.renderContext)
            }
            StyledTextArea.createStyledTextNode(seg.segment, seg.style, applyStyle)
        }
        val styledTextOps = SegmentOps.styledTextOps<TextStyle>()
        val doc = GenericEditableStyledDocument(SourceEditorParStyle(), TextStyle(), styledTextOps)
        val srcEdit = SourceEditorStyledArea(
            this,
            SourceEditorParStyle(),
            applyParagraphStyle,
            TextStyle(),
            doc,
            styledTextOps,
            nodeFactory)

        srcEdit.content.paragraphs.addChangeObserver {
            if (highlightedRow != null) {
                highlightedRow = null
                srcEdit.clearHighlights()
                messageArea.text = ""
            }
        }

        return srcEdit
    }

    private fun makeToolbarButton(name: String, fn: () -> Unit): Button {
        return Button(name).apply {
            onAction = EventHandler { fn() }
        }
    }

    fun runClicked() {
        val source = EditorSourceLocation(this, styledArea.document.text)
        client.evalSource(source, true)
    }

    private fun saveClicked() {
        if (loaded == null) {
            val selectedFile = client.selectFile(true) ?: return
            loaded = selectedFile
        }
        FileWriter(loaded, StandardCharsets.UTF_8).use { out ->
            out.write(styledArea.document.text)
        }
    }

    fun setFile(file: File) {
        val content = file.readLines()
        styledArea.deleteText(0, styledArea.length)
        //styledArea.insert(0, content, TextStyle())
        val builder = ReadOnlyStyledDocumentBuilder(styledArea.segOps, styledArea.initialParagraphStyle)
        content.forEach { text ->
            builder.addParagraph(text, styledArea.initialTextStyle)
        }
        styledArea.insert(0, builder.build())
        loaded = file
    }

    fun show() {
        stage.show()
    }

    fun highlightError(pos: Position, message: String?) {
        try {
            messageArea.text = message ?: ""
            styledArea.caretSelectionBind.moveTo(pos.line, pos.col)
            styledArea.highlightRow(pos.line)
            highlightedRow = pos.line
            styledArea.requestFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    class EditorSourceLocation(editor: SourceEditor, val text: String) : SourceLocation {
        private val editorReference = WeakReference(editor)

        val editor get() = editorReference.get()

        override fun sourceText() = text
        override fun open() = StringCharacterProvider(text)
    }
}

class SourceEditorParStyle(val type: StyleType = StyleType.NORMAL) {
    enum class StyleType {
        NORMAL,
        ERROR
    }
}

class SourceEditorStyledArea(
    private val sourceEditor: SourceEditor,
    parStyle: SourceEditorParStyle,
    applyParagraphStyle: BiConsumer<TextFlow, SourceEditorParStyle>,
    textStyle: TextStyle,
    doc: GenericEditableStyledDocument<SourceEditorParStyle, String, TextStyle>,
    styledTextOps: TextOps<String, TextStyle>,
    nodeFactory: Function<StyledSegment<String, TextStyle>, Node>
) : KAPEditorStyledArea<SourceEditorParStyle, String>(
    sourceEditor.client,
    parStyle,
    applyParagraphStyle,
    textStyle,
    doc,
    styledTextOps,
    nodeFactory
) {
    override fun addInputMappings(entries: MutableList<InputMap<out Event>>) {
        entries.add(InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER, KeyCombination.CONTROL_DOWN), { sourceEditor.runClicked() }))
    }

    fun highlightRow(row: Int) {
        clearHighlights()
        setParagraphStyle(row, SourceEditorParStyle(type = SourceEditorParStyle.StyleType.ERROR))
    }

    fun clearHighlights() {
        for (row in 0 until paragraphs.size) {
            setParagraphStyle(row, SourceEditorParStyle(type = SourceEditorParStyle.StyleType.NORMAL))
        }
    }
}
