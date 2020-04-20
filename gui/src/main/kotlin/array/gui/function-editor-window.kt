package array.gui

import array.*
import array.gui.styledarea.KAPEditorStyledArea
import array.gui.styledarea.ParStyle
import array.gui.styledarea.TextStyle
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.text.TextFlow
import javafx.stage.Stage
import org.fxmisc.richtext.StyledTextArea
import org.fxmisc.richtext.TextExt
import org.fxmisc.richtext.model.*
import java.util.function.BiConsumer
import java.util.function.Function

class FunctionEditorWindow(val renderContext: ClientRenderContext, val engine: Engine) {
    private val styledTextOps = SegmentOps.styledTextOps<TextStyle>()
    private val stage = Stage()
    private val textArea: FunctionEditorArea
    private val messageArea: TextField

    init {
        val vbox = VBox()

        val applyParagraphStyle = BiConsumer<TextFlow, ParStyle> { t, u ->
            //println("accept: t=${t}, u=${u}")
        }
        val nodeFactory = Function<StyledSegment<String, TextStyle>, Node> { seg ->
            val applyStyle = { a: TextExt, b: TextStyle ->
                b.styleContent(a, renderContext)
            }
            StyledTextArea.createStyledTextNode(seg.segment, seg.style, applyStyle)
        }
        val document = GenericEditableStyledDocument(ParStyle(), TextStyle(), styledTextOps)
        textArea = FunctionEditorArea(renderContext.extendedInput(), applyParagraphStyle, document, styledTextOps, nodeFactory)
        vbox.children.add(textArea)

        messageArea = TextField().apply {
            isEditable = false
        }
        vbox.children.add(messageArea)

        val buttonPanel = HBox().apply {
            val saveButton = Button("Save").apply {
                onAction = EventHandler { saveAndClose() }
            }
            children.add(saveButton)
        }
        vbox.children.add(buttonPanel)

        VBox.setVgrow(textArea, Priority.ALWAYS)

        stage.scene = Scene(vbox, 600.0, 700.0)
    }

    fun show() {
        stage.show()
    }

    private fun positionToIndex(text: String, line: Int, col: Int): Int {
        var currCharIndex = 0
        var currIndex = 0
        var currLine = 0
        while (currLine < line) {
            val ch = text.codePointAt(currCharIndex)
            if (ch == '\n'.toInt()) {
                currLine++
            }
            currCharIndex = text.offsetByCodePoints(currCharIndex, 1)
            currIndex++
        }

        var currCol = 0
        while (currCol < col) {
            currCharIndex = text.offsetByCodePoints(currCharIndex, 1)
            currCol++
        }

        return currCharIndex
    }

    fun highlightError(message: String, pos: Position?) {
        messageArea.text = message
        val line: Int
        val col: Int
        if (pos != null) {
            line = pos.line
            col = pos.col
        } else {
            line = 0
            col = 0
        }
        textArea.caretSelectionBind.moveTo(positionToIndex(textArea.text, line, col))
        textArea.requestFocus()
    }

    fun saveAndClose() {
        val source = StringSourceLocation(textArea.text)
        val tokeniser = TokenGenerator(engine, source)
        tokeniser.nextToken().let { token ->
            if (token != FnDefSym) {
                highlightError("Illegal character at start of content", Position(source, 0, 0))
                return
            }
        }
        val parser = APLParser(tokeniser)
        val definedUserFunction = try {
            parser.parseUserDefinedFn(Position(source, 0, 0))
        } catch (e: APLGenericException) {
            highlightError(e.message ?: "Missing message", e.pos)
            return
        }
        // Make sure there is no extra content at the end of the content
        while (true) {
            val (token, pos) = tokeniser.nextTokenWithPosition()
            if (token is EndOfFile) {
                break
            } else if (token !is Newline) {
                highlightError("Invalid characters after function definition", pos)
                return
            }
        }

        parser.registerDefinedUserFunction(definedUserFunction)
    }
}

class FunctionEditorArea(
    keyboardInput: ExtendedCharsKeyboardInput,
    applyParagraphStyle: BiConsumer<TextFlow, ParStyle>,
    document: EditableStyledDocument<ParStyle, String, TextStyle>,
    styledTextOps: TextOps<String, TextStyle>,
    nodeFactory: Function<StyledSegment<String, TextStyle>, Node>
) : KAPEditorStyledArea(
    keyboardInput,
    ParStyle(),
    applyParagraphStyle,
    TextStyle(),
    document,
    styledTextOps,
    nodeFactory
) {

}
