package array.gui.styledarea

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
import java.util.logging.Level
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
//    override fun replace(start: Int, end: Int, seg: String?, style: TextStyle?) {
//        super.replace(start, end, seg, style)
//    }

    private var updatesEnabled = false
    private var defaultKeymap: InputMap<*>
    private val commandListeners = ArrayList<(String) -> Unit>()

    init {
        defaultKeymap = Nodes.getInputMap(this)
        updateKeymap()
    }

    fun addCommandListener(fn: (String) -> Unit) {
        commandListeners.add(fn)
    }

    private fun updateKeymap() {
        val entries = ArrayList<InputMap<out Event>>()
        for (e in renderContext.extendedInput().keymap) {
            val modifiers =
                if (e.key.shift) arrayOf(KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN) else arrayOf(KeyCombination.ALT_DOWN)
            val v = InputMap.consume(EventPattern.keyTyped(e.key.character, *modifiers)) { replaceSelection(e.value) }
            entries.add(v)
        }
        entries.add(InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER)) { sendCurrentContent() })
        entries.add(defaultKeymap)
        Nodes.pushInputMap(this, InputMap.sequence(*entries.toTypedArray()))
    }

    enum class State {
        INITIAL,
    }

    fun sendCurrentContent() {
        var state = State.INITIAL
        var promptStartPos: Int? = null
        var startPos = 0
        var endPos: Int? = null
        loop@ for (span in document.getStyleSpans(0, document.length())) {
            when (state) {
                // do some things here
            }
        }
        loop@ for (span in document.getStyleSpans(0, document.length())) {
            when (span.style.type) {
                TextStyle.Type.PROMPT -> {
                    if (promptStartPos != null) {
                        throw Exception("Multiple prompt segments")
                    }
                    promptStartPos = startPos
                }
                TextStyle.Type.INPUT -> {
                    endPos = startPos + span.length
                    break@loop
                }
            }
            startPos += span.length
        }
        if (endPos == null) {
            return
        }
        if (promptStartPos == null) {
            throw Exception("No prompt segment")
        }
        val caretPos = caretPosition
        if (caretPos < startPos || caretPos > endPos) {
            return
        }
        val text = document.subSequence(startPos, endPos).text
        withUpdateEnabled {
            deleteText(startPos, endPos)
        }
        commandListeners.forEach { listener -> listener(text) }
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
            val doc = ReadOnlyStyledDocumentBuilder(segOps, ParStyle())
                .addParagraph(text, style)
                .build()
            insert(document.length(), doc)
        }
    }

    override fun replace(start: Int, end: Int, replacement: StyledDocument<ParStyle, String, TextStyle>) {
        LOGGER.log(Level.INFO, "replacing: pos:(${start} - ${end}): ${replacement.text}: ${replacement}")
        when {
            updatesEnabled -> super.replace(start, end, replacement)
            isAtInput(start, end) -> super.replace(start, end, makeInputStyle(replacement))
        }
    }

    private fun makeInputStyle(doc: StyledDocument<ParStyle, String, TextStyle>): StyledDocument<ParStyle, String, TextStyle> {
        return ReadOnlyStyledDocumentBuilder(segOps, ParStyle())
            .addParagraph(doc.text, TextStyle(type = TextStyle.Type.INPUT))
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

    companion object {
        val LOGGER = Logger.getLogger(ROStyledArea::class.qualifiedName)
    }
}
