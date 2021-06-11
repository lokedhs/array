package array.gui.styledarea

import array.gui.ClientRenderContext
import javafx.scene.Node
import javafx.scene.control.Button
import org.fxmisc.richtext.StyledTextArea
import org.fxmisc.richtext.TextExt
import java.util.*

interface EditorContent {
    fun length(): Int
    fun createNode(renderContext: ClientRenderContext, style: TextStyle): Node
    fun joinSegment(nextSeg: EditorContent): Optional<EditorContent>
    fun realGetText(): String?
    fun realCharAt(index: Int): Char
    fun realSubsequence(start: Int, end: Int): EditorContent

    companion object {
        fun makeBlank() = StringEditorContentEntry("")
        fun makeString(s: String) = StringEditorContentEntry(s)
    }
}

class StringEditorContentEntry(val text: String) : EditorContent {
    override fun length() = text.length

    override fun createNode(renderContext: ClientRenderContext, style: TextStyle): Node {
        val applyStyle = { a: TextExt, b: TextStyle ->
            b.styleContent(a, renderContext)
        }
        return StyledTextArea.createStyledTextNode(text, style, applyStyle)
    }

    override fun joinSegment(nextSeg: EditorContent): Optional<EditorContent> {
        return if (nextSeg is StringEditorContentEntry) {
            Optional.of(StringEditorContentEntry(text + nextSeg.text))
        } else {
            Optional.empty()
        }
    }

    override fun realGetText() = text

    override fun realCharAt(index: Int) = text[index]

    override fun realSubsequence(start: Int, end: Int) = StringEditorContentEntry(text.substring(start, end))
}

class FXButtonEditorContentEntry(val text: String) : EditorContent {
    override fun length(): Int {
        return text.length
    }

    override fun createNode(renderContext: ClientRenderContext, style: TextStyle): Node {
        return Button(text)
    }

    override fun joinSegment(nextSeg: EditorContent): Optional<EditorContent> {
        return Optional.empty()
    }

    override fun realGetText(): String {
        return text
    }

    override fun realCharAt(index: Int): Char {
        return text[index]
    }

    override fun realSubsequence(start: Int, end: Int): EditorContent {
        return if (start == 0 && end == text.length) {
            this
        } else {
            StringEditorContentEntry(text.substring(start, end))
        }
    }

}
