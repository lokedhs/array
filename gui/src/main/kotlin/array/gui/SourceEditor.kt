package array.gui

import array.Position
import array.SourceLocation
import array.StringCharacterProvider
import array.gui.styledarea.KAPEditorStyledArea
import array.gui.styledarea.TextStyle
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.control.ToolBar
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.TextFlow
import javafx.stage.Stage
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.StyledTextArea
import org.fxmisc.richtext.TextExt
import org.fxmisc.richtext.model.GenericEditableStyledDocument
import org.fxmisc.richtext.model.SegmentOps
import org.fxmisc.richtext.model.StyledSegment
import org.fxmisc.richtext.model.TextOps
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
            makeToolbarButton("Run", this::runClicked),
            makeToolbarButton("Save", this::saveClicked))
        vbox.children.add(toolbar)

        styledArea = initStyledArea()
        val scrollArea = VirtualizedScrollPane(styledArea)
        vbox.children.add(scrollArea)
        VBox.setVgrow(scrollArea, Priority.ALWAYS)

        vbox.children.add(messageArea)

        stage.scene = Scene(vbox, 400.0, 600.0)

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
            println("changing: ${parStyle.type}")
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
            client.renderContext.extendedInput(),
            SourceEditorParStyle(),
            applyParagraphStyle,
            TextStyle(),
            doc,
            styledTextOps,
            nodeFactory)

        srcEdit.content.paragraphs.addChangeObserver { change ->
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

    private fun runClicked() {
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
        val content = file.readText()
        styledArea.deleteText(0, styledArea.length)
        styledArea.insert(0, content, TextStyle())
        loaded = file
    }

    fun show() {
        stage.show()
    }

    fun highlightError(message: String, pos: Position) {
        messageArea.text = message
        styledArea.caretSelectionBind.moveTo(pos.line, pos.col)
        styledArea.highlightRow(pos.line)
        highlightedRow = pos.line
        styledArea.requestFocus()
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
    extendedInput: ExtendedCharsKeyboardInput,
    parStyle: SourceEditorParStyle,
    applyParagraphStyle: BiConsumer<TextFlow, SourceEditorParStyle>,
    textStyle: TextStyle,
    doc: GenericEditableStyledDocument<SourceEditorParStyle, String, TextStyle>,
    styledTextOps: TextOps<String, TextStyle>,
    nodeFactory: Function<StyledSegment<String, TextStyle>, Node>
) : KAPEditorStyledArea<SourceEditorParStyle, String>(
    extendedInput,
    parStyle,
    applyParagraphStyle,
    textStyle,
    doc,
    styledTextOps,
    nodeFactory
) {
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
