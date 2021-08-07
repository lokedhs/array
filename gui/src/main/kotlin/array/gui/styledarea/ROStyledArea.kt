package array.gui.styledarea

import array.APLValue
import array.gui.Client
import array.gui.display.ValueRenderer
import javafx.scene.Node
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.text.TextFlow
import org.fxmisc.richtext.model.*
import org.fxmisc.wellbehaved.event.EventPattern
import org.fxmisc.wellbehaved.event.InputMap
import java.util.function.BiConsumer
import java.util.function.Function
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
class ROStyledArea(
    client: Client,
    applyParagraphStyle: BiConsumer<TextFlow, ParStyle>,
    document: EditableStyledDocument<ParStyle, EditorContent, TextStyle>,
    styledTextOps: TextOps<EditorContent, TextStyle>,
    nodeFactory: Function<StyledSegment<EditorContent, TextStyle>, Node>
) : KAPEditorStyledArea<ParStyle, EditorContent>(
    client,
    ParStyle(),
    applyParagraphStyle,
    TextStyle(),
    document,
    styledTextOps,
    nodeFactory
) {
    private var updatesEnabled = false
    private val commandListeners = ArrayList<(String) -> Unit>()
    private val historyListeners = ArrayList<HistoryListener>()

    init {
        displayPrompt()
    }

    override fun addInputMappings(entries: MutableList<InputMap<*>>) {
        entries.add(InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER), { sendCurrentContent() }))
        entries.add(InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER, KeyCombination.CONTROL_DOWN), { insertNewline() }))

        // History navigation
        entries.add(InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.UP), { atEditboxStart() }, { prevHistory() }))
        entries.add(InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.DOWN), { atEditboxEnd() }, { nextHistory() }))

        // Cursor movement
        entries.add(InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.HOME), { atEditbox() }, { moveToBeginningOfInput() }))
    }

    fun displayPrompt() {
        withUpdateEnabled {
            val inputDocument = ReadOnlyStyledDocumentBuilder(segOps, ParStyle(ParStyle.ParStyleType.NORMAL))
                .addParagraph(
                    listOf(
                        StyledSegment(EditorContent.makeString(">"), TextStyle(TextStyle.Type.PROMPT)),
                        StyledSegment(EditorContent.makeString(" "), TextStyle(TextStyle.Type.PROMPT, promptTag = true))))
                .build()
            insert(document.length(), inputDocument)
        }
    }

    fun addCommandListener(fn: (String) -> Unit) {
        commandListeners.add(fn)
    }

    private fun atEditboxStart(): Boolean {
        println("Should check if we're at the top of the editbox")
        return true
    }

    private fun atEditboxEnd(): Boolean {
        println("Should check if we're at the bottom of the editbox")
        return true
    }

    private fun atEditbox(): Boolean {
        return true
    }

    private fun isAtInput(start: Int, end: Int): Boolean {
        return if (start == end && document.getStyleAtPosition(start).promptTag) {
            true
        } else {
            val spans = document.getStyleSpans(start, end)
            val firstNonInputSpan = spans.find { span ->
                span.style.type != TextStyle.Type.INPUT
            }
            firstNonInputSpan == null
        }
    }

    fun addHistoryListener(historyListener: HistoryListener) {
        historyListeners.add(historyListener)
    }

    private fun insertNewline() {
        insertText(caretPosition, "\n")
    }

    private fun prevHistory() {
        historyListeners.forEach { it.prevHistory() }
    }

    private fun nextHistory() {
        historyListeners.forEach { it.nextHistory() }
    }

    private fun findInputStartEnd(): InputPositions {
        var pos = document.length() - 1
        while (pos >= 0) {
            val style = document.getStyleOfChar(pos)
            if (style.promptTag) {
                break
            }
            pos--
        }
        assert(pos >= 0)

        val inputStartPos = pos + 1
        while (pos >= 0) {
            val style = document.getStyleOfChar(pos)
            if (style.type != TextStyle.Type.PROMPT) {
                break
            }
            pos--
        }

        val promptStartPos = pos + 1
        return InputPositions(promptStartPos, inputStartPos, document.length())
    }

    private fun sendCurrentContent() {
        val inputPosition = findInputStartEnd()
        val text = document.subSequence(inputPosition.inputStart, inputPosition.inputEnd).text
        withUpdateEnabled {
            deleteText(inputPosition.inputStart, inputPosition.inputEnd)
        }
        commandListeners.forEach { callback -> callback(text) }
    }

    fun currentInput(): String {
        val inputPosition = findInputStartEnd()
        return document.subSequence(inputPosition.inputStart, inputPosition.inputEnd).text
    }

    fun <T> withUpdateEnabled(fn: () -> T): T {
        contract { callsInPlace(fn, InvocationKind.EXACTLY_ONCE) }
        val oldEnabled = updatesEnabled
        updatesEnabled = true
        try {
            val result = fn()
            moveToEndOfInput()
            return result
        } finally {
            updatesEnabled = oldEnabled
        }
    }

    fun appendTextEnd(text: String, style: TextStyle, parStyle: ParStyle? = null) {
        withUpdateEnabled {
            val builder = ReadOnlyStyledDocumentBuilder(segOps, parStyle ?: ParStyle())
            text.split("\n").forEach { part -> builder.addParagraph(EditorContent.makeString(part), style) }
            val inputPos = findInputStartEnd()
            insert(inputPos.promptStartPos, builder.build())
        }
        showParagraphAtTop(document.paragraphs.size - 1)
    }

    fun appendAPLValueEnd(value: APLValue, style: TextStyle, parStyle: ParStyle = ParStyle()) {
        withUpdateEnabled {
            val newDoc = ReadOnlyStyledDocumentBuilder(segOps, parStyle)
                .addParagraph(
                    mutableListOf(
                        StyledSegment(ValueRenderer.makeContent(client, value), style)))
                .addParagraph(EditorContent.makeBlank(), style)
                .build()
            val inputPos = findInputStartEnd()
            insert(inputPos.promptStartPos, newDoc)
        }
        showParagraphAtTop(document.paragraphs.size - 1)
    }

    fun appendErrorMessage(text: String) {
        withUpdateEnabled {
            val inputPos = findInputStartEnd()
            val newDoc = ReadOnlyStyledDocumentBuilder(segOps, ParStyle())
                .addParagraph(
                    mutableListOf(
                        StyledSegment(EditorContent.makeString(text), TextStyle(TextStyle.Type.ERROR))))
                .addParagraph(EditorContent.makeBlank(), TextStyle(TextStyle.Type.ERROR))
                .build()
            insert(inputPos.promptStartPos, newDoc)
        }
    }

    fun appendOutputEnd(text: String) {
        withUpdateEnabled {
            val textStyle = TextStyle(TextStyle.Type.OUTPUT)
            val builder = ReadOnlyStyledDocumentBuilder(segOps, ParStyle(ParStyle.ParStyleType.OUTPUT))
            text.split("\n").forEach { part -> builder.addParagraph(EditorContent.makeString(part), textStyle) }

            val inputPos = findInputStartEnd()
            val p = inputPos.promptStartPos
            // Input position at the beginning of the buffer
            if (p == 0) {
                TODO("This code path is never tested")
            } else {
                val style = document.getParagraphStyleAtPosition(p - 1)
                val newPos = if (style.type == ParStyle.ParStyleType.OUTPUT) {
                    p - 1
                } else {
                    builder.addParagraph(EditorContent.makeBlank(), textStyle)
                    p
                }
                insert(newPos, builder.build())
            }
        }
    }

    override fun replace(start: Int, end: Int, replacement: StyledDocument<ParStyle, EditorContent, TextStyle>) {
        when {
            updatesEnabled -> super.replace(start, end, replacement)
            isAtInput(start, end) -> super.replace(start, end, makeInputStyle(replacement.text))
        }
    }

    private fun makeInputStyle(s: String): StyledDocument<ParStyle, EditorContent, TextStyle> {
        return ReadOnlyStyledDocumentBuilder(segOps, ParStyle())
            .addParagraph(EditorContent.makeString(s), TextStyle(type = TextStyle.Type.INPUT))
            .build()
    }

    fun replaceInputText(s: String) {
        val inputPos = findInputStartEnd()
        withUpdateEnabled {
            deleteText(inputPos.inputStart, inputPos.inputEnd)
            replace(inputPos.inputStart, inputPos.inputStart, makeInputStyle(s))
        }
    }

    private fun moveToBeginningOfInput() {
        val inputPosition = findInputStartEnd()
        caretSelectionBind.moveTo(inputPosition.inputStart)
    }

    private fun moveToEndOfInput() {
        val inputPosition = findInputStartEnd()
        caretSelectionBind.moveTo(inputPosition.inputEnd)
    }

    data class InputPositions(val promptStartPos: Int, val inputStart: Int, val inputEnd: Int)
}
