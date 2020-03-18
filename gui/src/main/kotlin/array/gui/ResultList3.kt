package array.gui

import array.APLGenericException
import array.APLValue
import array.gui.styledarea.EditParStyle
import array.gui.styledarea.ParStyle
import array.gui.styledarea.ROStyledArea
import array.gui.styledarea.TextStyle
import javafx.scene.Node
import javafx.scene.text.TextFlow
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.StyledTextArea
import org.fxmisc.richtext.TextExt
import org.fxmisc.richtext.model.*
import java.util.function.BiConsumer
import java.util.function.Function

class ResultList3(val renderContext: ClientRenderContext) {
    private val styledTextOps = SegmentOps.styledTextOps<TextStyle>()
    private val styledArea: ROStyledArea
    private val scrollArea: VirtualizedScrollPane<ROStyledArea>

    init {
        val applyParagraphStyle = BiConsumer<TextFlow, ParStyle> { t, u ->
            println("accept: t=${t}, u=${u}")
        }
        val nodeFactory = Function<StyledSegment<String, TextStyle>, Node> { seg ->
            val applyStyle = { a: TextExt, b: TextStyle ->
                b.styleContent(a, renderContext)
            }
            StyledTextArea.createStyledTextNode(seg.segment, seg.style, applyStyle)
        }

        val segmentOps: TextOps<String, TextStyle> = styledTextOps

        val document = GenericEditableStyledDocument(ParStyle(), TextStyle(), styledTextOps)
        styledArea = ROStyledArea(
            ParStyle(),
            applyParagraphStyle,
            TextStyle(),
            document,
            segmentOps,
            nodeFactory
        )

        //styledArea.isEditable = false
        styledArea.isWrapText = false

        styledArea.withUpdateEnabled {
            val inputDocument = ReadOnlyStyledDocumentBuilder<ParStyle, String, TextStyle>(styledTextOps, EditParStyle())
                .addParagraph(listOf(
                    StyledSegment("> ", TextStyle(TextStyle.Type.PROMPT)),
                    StyledSegment("X", TextStyle(TextStyle.Type.INPUT))))
                .build()
            styledArea.insert(0, inputDocument)
        }

        scrollArea = VirtualizedScrollPane(styledArea)
    }

    fun getNode() = scrollArea

    fun addResult(text: String, v: APLValue) {
        addInput(text)
        styledArea.appendTextEnd(v.formatted() + "\n", TextStyle(TextStyle.Type.RESULT))
    }

    fun addResult(text: String, e: APLGenericException) {
        addInput(text)
        styledArea.appendTextEnd(e.formattedError() + "\n", TextStyle(TextStyle.Type.ERROR))
    }

    private fun addInput(text: String) {
        styledArea.appendTextEnd(text + "\n", TextStyle(TextStyle.Type.LOG_INPUT))
    }
}
