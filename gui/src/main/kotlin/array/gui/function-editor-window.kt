package array.gui

import array.*
import array.gui.styledarea.KAPEditorStyledArea
import array.gui.styledarea.ParStyle
import array.gui.styledarea.TextStyle
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.text.TextFlow
import javafx.stage.Stage
import org.fxmisc.richtext.StyledTextArea
import org.fxmisc.richtext.TextExt
import org.fxmisc.richtext.model.*
import java.util.function.BiConsumer
import java.util.function.Function

class FunctionEditorWindow(val renderContext: ClientRenderContext, val engine: Engine, name: Symbol? = null) {
    private val styledTextOps = SegmentOps.styledTextOps<TextStyle>()
    private val stage = Stage()
    private val textArea: FunctionEditorArea

    init {

//        textArea = TextArea().apply {
//            font = renderContext.font()
//            renderContext.extendedInput().addEventHandlerToNode(this)
//        }
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
        textArea = FunctionEditorArea(applyParagraphStyle, document, styledTextOps, nodeFactory)
        val buttonPanel = HBox().apply {
            val saveButton = Button("Save").apply {
                onAction = EventHandler { saveAndClose() }
            }
            children.add(saveButton)
        }
        val border = BorderPane().apply {
            center = textArea
            bottom = buttonPanel
        }

        stage.scene = Scene(border, 600.0, 700.0)
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
        println("Message: ${message}")
        if (pos != null) {
//            textArea.positionCaret(positionToIndex(textArea.text, pos.line, pos.col))
        }
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
    applyParagraphStyle: BiConsumer<TextFlow, ParStyle>,
    document: EditableStyledDocument<ParStyle, String, TextStyle>,
    styledTextOps: TextOps<String, TextStyle>,
    nodeFactory: Function<StyledSegment<String, TextStyle>, Node>
) : KAPEditorStyledArea(
    ParStyle(),
    applyParagraphStyle,
    TextStyle(),
    document,
    styledTextOps,
    nodeFactory
) {

}
