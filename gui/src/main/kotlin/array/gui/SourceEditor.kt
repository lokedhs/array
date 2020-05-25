package array.gui

import array.SourceLocation
import array.StringCharacterProvider
import array.gui.styledarea.KAPEditorStyledArea
import array.gui.styledarea.ParStyle
import array.gui.styledarea.TextStyle
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ToolBar
import javafx.scene.layout.BorderPane
import javafx.scene.text.TextFlow
import javafx.stage.Stage
import org.fxmisc.richtext.StyledTextArea
import org.fxmisc.richtext.TextExt
import org.fxmisc.richtext.model.GenericEditableStyledDocument
import org.fxmisc.richtext.model.SegmentOps
import org.fxmisc.richtext.model.StyledSegment
import java.io.File
import java.io.FileWriter
import java.lang.ref.WeakReference
import java.nio.charset.StandardCharsets
import java.util.function.BiConsumer
import java.util.function.Function

class SourceEditor(val client: Client) {
    private val stage = Stage()
    private var doc: GenericEditableStyledDocument<ParStyle, String, TextStyle>
    private var styledArea: KAPEditorStyledArea
    private var loaded: File? = null

    init {
        val border = BorderPane()

        @Suppress("UNUSED_ANONYMOUS_PARAMETER")
        val applyParagraphStyle = BiConsumer<TextFlow, ParStyle> { t, u ->
        }
        val nodeFactory = Function<StyledSegment<String, TextStyle>, Node> { seg ->
            val applyStyle = { a: TextExt, b: TextStyle ->
                b.styleContent(a, client.renderContext)
            }
            StyledTextArea.createStyledTextNode(seg.segment, seg.style, applyStyle)
        }
        val styledTextOps = SegmentOps.styledTextOps<TextStyle>()
        doc = GenericEditableStyledDocument(ParStyle(), TextStyle(), styledTextOps)
        styledArea = KAPEditorStyledArea(
            client.renderContext.extendedInput(),
            ParStyle(),
            applyParagraphStyle,
            TextStyle(),
            doc,
            styledTextOps,
            nodeFactory
        )
        border.center = styledArea

        val toolbar = ToolBar(
            makeToolbarButton("Run", { runClicked() }),
            makeToolbarButton("Save", { saveClicked() }))
        border.top = toolbar

        stage.scene = Scene(border, 400.0, 600.0)
    }

    private fun makeToolbarButton(name: String, fn: () -> Unit): Button {
        return Button(name).apply {
            onAction = EventHandler { fn() }
        }
    }

    private fun runClicked() {
        val source = EditorSourceLocation(this, doc.text)
        client.evalSource(source, true)
    }

    private fun saveClicked() {
        if (loaded == null) {
            val selectedFile = client.selectFile(true) ?: return
            loaded = selectedFile
        }
        FileWriter(loaded, StandardCharsets.UTF_8).use { out ->
            out.write(doc.text)
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

    class EditorSourceLocation(editor: SourceEditor, val text: String) : SourceLocation {
        private val editorReference = WeakReference(editor)

        override fun sourceText() = text
        override fun open() = StringCharacterProvider(text)
    }
}
