package array.gui

import array.APLGenericException
import array.APLValue
import array.gui.styledarea.HistoryListener
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

class ResultList3(val client: Client) {
    private val styledTextOps = SegmentOps.styledTextOps<TextStyle>()
    private val styledArea: ROStyledArea
    private val scrollArea: VirtualizedScrollPane<ROStyledArea>

    val history = ArrayList<String>()
    var historyPos = 0

    init {
        val applyParagraphStyle = BiConsumer<TextFlow, ParStyle> { t, u ->
            //println("accept: t=${t}, u=${u}")
        }
        val nodeFactory = Function<StyledSegment<String, TextStyle>, Node> { seg ->
            val applyStyle = { a: TextExt, b: TextStyle ->
                b.styleContent(a, client.renderContext)
            }
            StyledTextArea.createStyledTextNode(seg.segment, seg.style, applyStyle)
        }

        val segmentOps: TextOps<String, TextStyle> = styledTextOps

        val document = GenericEditableStyledDocument(ParStyle(), TextStyle(), styledTextOps)
        styledArea = ROStyledArea(
            client.renderContext,
            ParStyle(),
            applyParagraphStyle,
            TextStyle(),
            document,
            segmentOps,
            nodeFactory
        )

        styledArea.addCommandListener(::processCommand)

        val historyListener = ResultHistoryListener()
        styledArea.addHistoryListener(historyListener)

        //styledArea.isEditable = false
        styledArea.isWrapText = false

        scrollArea = VirtualizedScrollPane(styledArea)
    }

    private fun processCommand(text: String) {
        if (text.trim().isNotBlank()) {
            println("adding command: $text, size = ${history.size}")
            history.add(text)
            historyPos = history.size
            addInput(text)
            client.sendInput(text)
        }
        styledArea.displayPrompt()
    }

    fun getNode() = scrollArea

    fun addResult(v: APLValue) {
        styledArea.withUpdateEnabled {
            styledArea.appendTextEnd(v.formatted(APLValue.FormatStyle.PRETTY) + "\n", TextStyle(TextStyle.Type.RESULT))
        }
    }

    fun addResult(e: APLGenericException) {
        styledArea.withUpdateEnabled {
            styledArea.appendTextEnd(e.formattedError() + "\n", TextStyle(TextStyle.Type.ERROR))
        }
    }

    fun addOutput(text: String) {
        styledArea.withUpdateEnabled {
            styledArea.appendTextEnd(text + "\n", TextStyle(TextStyle.Type.OUTPUT))
        }
    }

    private fun addInput(text: String) {
        styledArea.withUpdateEnabled {
            val doc = GenericEditableStyledDocument(Paragraph(ParStyle(indent = true),
                styledTextOps,
                text + "\n",
                TextStyle(TextStyle.Type.LOG_INPUT)))
            styledArea.append(doc)
        }
    }

    inner class ResultHistoryListener : HistoryListener {
        override fun prevHistory() {
            if (historyPos > 0) {
                historyPos--
                styledArea.replaceInputText(history[historyPos])
            }
        }

        override fun nextHistory() {
            if (historyPos < history.size - 1) {
                historyPos++
                styledArea.replaceInputText(history[historyPos])
            }
        }
    }
}
