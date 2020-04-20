package array.gui.styledarea

import array.assertx
import array.gui.ExtendedCharsKeyboardInput
import javafx.scene.Node
import javafx.scene.input.KeyCode
import javafx.scene.text.TextFlow
import org.fxmisc.richtext.model.*
import org.fxmisc.wellbehaved.event.EventPattern
import org.fxmisc.wellbehaved.event.InputMap
import java.util.function.BiConsumer
import java.util.function.Function

class ROStyledArea(
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
    private var updatesEnabled = false
    private val commandListeners = ArrayList<(String) -> Unit>()
    private val historyListeners = ArrayList<HistoryListener>()

    init {
        displayPrompt()
    }

    override fun addInputMappings(entries: MutableList<InputMap<*>>) {
        entries.add(InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER), { sendCurrentContent() }))

        // History navigation
        entries.add(InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.UP), { atEditboxStart() }, { prevHistory() }))
        entries.add(InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.DOWN), { atEditboxEnd() }, { nextHistory() }))

        // Cursor movement
        entries.add(InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.HOME), { atEditbox() }, { moveToBeginningOfInput() }))
    }

    fun displayPrompt() {
        withUpdateEnabled {
            val inputDocument = ReadOnlyStyledDocumentBuilder(segOps, ParStyle())
                .addParagraph(listOf(
                    StyledSegment(">", TextStyle(TextStyle.Type.PROMPT)),
                    StyledSegment(" ", TextStyle(TextStyle.Type.PROMPT, promptTag = true))))
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
        assertx(pos >= 0)

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
            deleteText(inputPosition.promptStartPos, document.length())
        }
        commandListeners.forEach { callback -> callback(text) }
    }

    fun currentInput(): String {
        val inputPosition = findInputStartEnd()
        return document.subSequence(inputPosition.inputStart, inputPosition.inputEnd).text
    }

    fun withUpdateEnabled(fn: () -> Unit) {
        val oldEnabled = updatesEnabled
        updatesEnabled = true
        try {
            fn()
        } finally {
            updatesEnabled = oldEnabled
        }
    }

    fun appendTextEnd(text: String, style: TextStyle) {
        withUpdateEnabled {
            val builder = ReadOnlyStyledDocumentBuilder(segOps, ParStyle())
            text.split("\n").forEach { part -> builder.addParagraph(part, style) }
            insert(document.length(), builder.build())
        }
        showParagraphAtTop(document.paragraphs.size - 1)
    }

    override fun replace(start: Int, end: Int, replacement: StyledDocument<ParStyle, String, TextStyle>) {
        when {
            updatesEnabled -> super.replace(start, end, replacement)
            isAtInput(start, end) -> super.replace(start, end, makeInputStyle(replacement.text))
        }
    }

    private fun makeInputStyle(s: String): StyledDocument<ParStyle, String, TextStyle> {
        return ReadOnlyStyledDocumentBuilder(segOps, ParStyle())
            .addParagraph(s, TextStyle(type = TextStyle.Type.INPUT))
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

    data class InputPositions(val promptStartPos: Int, val inputStart: Int, val inputEnd: Int)
}
