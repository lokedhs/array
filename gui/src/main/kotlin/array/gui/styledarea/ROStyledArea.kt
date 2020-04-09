package array.gui.styledarea

import array.assertx
import array.gui.ClientRenderContext
import javafx.event.Event
import javafx.scene.Node
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.text.TextFlow
import org.fxmisc.richtext.GenericStyledArea
import org.fxmisc.richtext.model.*
import org.fxmisc.wellbehaved.event.EventPattern
import org.fxmisc.wellbehaved.event.InputMap
import org.fxmisc.wellbehaved.event.Nodes
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.logging.Logger

class ROStyledArea(
    val renderContext: ClientRenderContext,
    parStyle: ParStyle,
    applyParagraphStyle: BiConsumer<TextFlow, ParStyle>,
    textStyle: TextStyle,
    document: EditableStyledDocument<ParStyle, String, TextStyle>,
    segmentOps: TextOps<String, TextStyle>,
    nodeFactory: Function<StyledSegment<String, TextStyle>, Node>
) : GenericStyledArea<ParStyle, String, TextStyle>(
    parStyle,
    applyParagraphStyle,
    textStyle,
    document,
    segmentOps,
    nodeFactory
) {

    private var updatesEnabled = false
    private var defaultKeymap: InputMap<*>
    private val commandListeners = ArrayList<(String) -> Unit>()
    private val historyListeners = ArrayList<HistoryListener>()

    init {
        defaultKeymap = Nodes.getInputMap(this)
        updateKeymap()
        displayPrompt()
    }

    fun displayPrompt() {
        withUpdateEnabled {
            val inputDocument = ReadOnlyStyledDocumentBuilder<ParStyle, String, TextStyle>(segOps, ParStyle())
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

    private var prefixActive = false

    private fun updateKeymap() {
        val entries = ArrayList<InputMap<out Event>>()

        // Keymap
        renderContext.extendedInput().keymap.forEach { e ->
            val modifiers =
                if (e.key.shift) arrayOf(KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN) else arrayOf(KeyCombination.ALT_DOWN)
            val v = InputMap.consume(EventPattern.keyTyped(e.key.character, *modifiers), { replaceSelection(e.value) })
            entries.add(v)
        }
        entries.add(InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER), { sendCurrentContent() }))

        // History navigation
        entries.add(InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.UP), { atEditboxStart() }, { prevHistory() }))
        entries.add(InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.DOWN), { atEditboxEnd() }, { nextHistory() }))

        // Prefix input
        entries.add(makePrefixInputKeymap())

        entries.add(defaultKeymap)
        Nodes.pushInputMap(this, InputMap.sequence(*entries.toTypedArray()))
    }

    private fun makePrefixInputKeymap(): InputMap<out Event> {
        fun verifyPrefixActive() = prefixActive
        val entries = ArrayList<InputMap<out Event>>()
        entries.add(InputMap.consumeWhen(EventPattern.keyTyped("."), { !prefixActive }) { prefixActive = true })
        renderContext.extendedInput().keymap.forEach { e ->
            val modifiers = if (e.key.shift) arrayOf(KeyCombination.SHIFT_DOWN) else emptyArray()
            entries.add(InputMap.consumeWhen(EventPattern.keyTyped(e.key.character, *modifiers),
                ::verifyPrefixActive,
                { replaceSelection(e.value) }))
        }
        return InputMap.sequence(*entries.toTypedArray())
    }

    private fun atEditboxStart(): Boolean {
        println("Should check if we're at the top of the editbox")
        return true
    }

    private fun atEditboxEnd(): Boolean {
        println("Should check if we're at the bottom of the editbox")
        return true
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

    fun replaceInputText(s: String) {
        val inputPos = findInputStartEnd()
        withUpdateEnabled {
            deleteText(inputPos.inputStart, inputPos.inputEnd)
            replace(inputPos.inputStart, inputPos.inputStart, makeInputStyle(s))
        }
    }

    data class InputPositions(val promptStartPos: Int, val inputStart: Int, val inputEnd: Int)

    companion object {
        val LOGGER = Logger.getLogger(ROStyledArea::class.qualifiedName)
    }
}
