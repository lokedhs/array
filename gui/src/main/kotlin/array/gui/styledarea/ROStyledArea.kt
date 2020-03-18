package array.gui.styledarea

import javafx.event.Event
import javafx.scene.Node
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
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

    init {
        val inputMap: InputMap<Event> = InputMap.sequence(
            InputMap.consume(EventPattern.keyTyped("a", KeyCombination.ALT_DOWN), { event: KeyEvent -> println("event: $event") }))
        Nodes.addInputMap(this, inputMap)
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
        if (updatesEnabled) {
            super.replace(start, end, replacement)
        } else if (!containsNonInput(start, end)) {
            super.replace(start, end, makeInputStyle(replacement))
        }
    }

    private fun makeInputStyle(doc: StyledDocument<ParStyle, String, TextStyle>): StyledDocument<ParStyle, String, TextStyle> {
        return ReadOnlyStyledDocumentBuilder(segOps, ParStyle())
            .addParagraph(doc.text, TextStyle(type = TextStyle.Type.INPUT))
            .build()
    }

    private fun containsNonInput(start: Int, end: Int): Boolean {
        val spans = document.getStyleSpans(start, end)
        val firstNonInputSpan = spans.find { span ->
            span.style.type != TextStyle.Type.INPUT
        }
        return firstNonInputSpan != null
    }

    companion object {
        val LOGGER = Logger.getLogger(ROStyledArea::class.qualifiedName)
    }
}
