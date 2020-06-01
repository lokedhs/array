package array.gui

import array.APLGenericException
import array.APLValue
import array.FormatStyle
import array.gui.styledarea.HistoryListener
import array.gui.styledarea.ParStyle
import array.gui.styledarea.ROStyledArea
import array.gui.styledarea.TextStyle
import javafx.scene.Node
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.TextFlow
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.StyledTextArea
import org.fxmisc.richtext.TextExt
import org.fxmisc.richtext.model.GenericEditableStyledDocument
import org.fxmisc.richtext.model.SegmentOps
import org.fxmisc.richtext.model.StyledSegment
import java.util.function.BiConsumer
import java.util.function.Function

class ResultList3(val client: Client) {
    private val styledTextOps = SegmentOps.styledTextOps<TextStyle>()
    private val styledArea: ROStyledArea
    private val scrollArea: VirtualizedScrollPane<ROStyledArea>

    private val history = ArrayList<String>()
    private var historyPos = 0
    private var pendingInput: String? = null

    init {
        val applyParagraphStyle = BiConsumer<TextFlow, ParStyle> { t, u ->
            if (u.type == ParStyle.ParStyleType.INDENT) {
                t.border =
                    Border(BorderStroke(Color.TRANSPARENT, BorderStrokeStyle.NONE, CornerRadii.EMPTY, BorderWidths(5.0, 5.0, 5.0, 30.0)))
            }
        }
        val nodeFactory = Function<StyledSegment<String, TextStyle>, Node> { seg ->
            val applyStyle = { a: TextExt, b: TextStyle ->
                b.styleContent(a, client.renderContext)
            }
            StyledTextArea.createStyledTextNode(seg.segment, seg.style, applyStyle)
        }

        val document = GenericEditableStyledDocument(ParStyle(), TextStyle(), styledTextOps)
        styledArea = ROStyledArea(client.renderContext.extendedInput(), applyParagraphStyle, document, styledTextOps, nodeFactory)

        styledArea.addCommandListener(::processCommand)

        val historyListener = ResultHistoryListener()
        styledArea.addHistoryListener(historyListener)

        styledArea.isWrapText = false

        scrollArea = VirtualizedScrollPane(styledArea)
    }

    private fun processCommand(text: String) {
        if (text.trim().isNotBlank()) {
            history.add(text)
            historyPos = history.size
            pendingInput = null
            addInput(text)
            client.sendInput(text)
        }
    }

    fun getNode() = scrollArea

    fun addResult(v: APLValue) {
        styledArea.appendTextEnd(v.formatted(FormatStyle.PRETTY) + "\n", TextStyle(TextStyle.Type.RESULT))
    }

    fun addExceptionResult(e: Exception) {
        val message = if (e is APLGenericException) {
            e.formattedError()
        } else {
            "Exception from KAP engine: ${e.message}"
        }
        styledArea.appendTextEnd(message + "\n", TextStyle(TextStyle.Type.ERROR))
    }

    fun addOutput(text: String) {
        styledArea.appendOutputEnd(text)
    }

    private fun addInput(text: String) {
        styledArea.appendTextEnd(text + "\n", TextStyle(TextStyle.Type.LOG_INPUT), ParStyle(ParStyle.ParStyleType.INDENT))
    }

    inner class ResultHistoryListener : HistoryListener {
        override fun prevHistory() {
            if (historyPos > 0) {
                if (historyPos == history.size) {
                    pendingInput = styledArea.currentInput()
                }
                historyPos--
                styledArea.replaceInputText(history[historyPos])
            }
        }

        override fun nextHistory() {
            when {
                historyPos < history.size - 1 -> {
                    historyPos++
                    styledArea.replaceInputText(history[historyPos])
                }
                historyPos == history.size - 1 -> {
                    historyPos++
                    styledArea.replaceInputText(pendingInput ?: "")
                    pendingInput = null
                }
            }
        }
    }
}
