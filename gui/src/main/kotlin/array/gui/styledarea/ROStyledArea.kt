package array.gui.styledarea

import javafx.scene.Node
import javafx.scene.text.TextFlow
import org.fxmisc.richtext.GenericStyledArea
import org.fxmisc.richtext.model.EditableStyledDocument
import org.fxmisc.richtext.model.StyledDocument
import org.fxmisc.richtext.model.StyledSegment
import org.fxmisc.richtext.model.TextOps
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
) : GenericStyledArea<ParStyle, String, TextStyle>(parStyle, applyParagraphStyle, textStyle, document, segmentOps, nodeFactory) {
//    override fun replace(start: Int, end: Int, seg: String?, style: TextStyle?) {
//        super.replace(start, end, seg, style)
//    }

    override fun replace(start: Int, end: Int, replacement: StyledDocument<ParStyle, String, TextStyle>) {
        LOGGER.log(Level.INFO, "replacing: pos:(${start} - ${end}): ${replacement.text}: ${replacement}")
        val firstReplacementInputSpan =
            replacement.paragraphs.find { paragraph -> paragraph.styleSpans.find { span -> span.style.type == TextStyle.Type.INPUT } != null }
        val firstNonInputDocumentSpan = document.getStyleSpans(start, end).find { span -> span.style.type != TextStyle.Type.INPUT }
        if (firstReplacementInputSpan == null || firstNonInputDocumentSpan == null) {
            super.replace(start, end, replacement)
        }
//        super.replace(start, end, replacement)
    }

    companion object {
        val LOGGER = Logger.getLogger(ROStyledArea::class.qualifiedName)
    }
}
