package array.gui

import array.APLGenericException
import array.APLValue
import array.gui.styledarea.*
import javafx.scene.Node
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.TextFlow
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.model.GenericEditableStyledDocument
import org.fxmisc.richtext.model.SegmentOpsBase
import org.fxmisc.richtext.model.StyledSegment
import org.fxmisc.richtext.model.TextOps
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Function

class ResultList3(val client: Client) {
    private val styledOps = CodeSegmentOps()
    private val styledArea: ROStyledArea
    private val scrollArea: VirtualizedScrollPane<ROStyledArea>

    private val history = ArrayList<String>()
    private var historyPos = 0
    private var pendingInput: String? = null

    init {
        val applyParagraphStyle = BiConsumer<TextFlow, ParStyle> { text, parStyle ->
            if (parStyle.type == ParStyle.ParStyleType.INDENT) {
                text.border =
                    Border(BorderStroke(Color.TRANSPARENT, BorderStrokeStyle.NONE, CornerRadii.EMPTY, BorderWidths(5.0, 5.0, 5.0, 30.0)))
            }
        }
        val nodeFactory = Function<StyledSegment<EditorContent, TextStyle>, Node> { segment ->
            segment.segment.createNode(client.renderContext, segment.style)
        }

        val document = GenericEditableStyledDocument(ParStyle(), TextStyle(), styledOps)
        styledArea = ROStyledArea(client, applyParagraphStyle, document, styledOps, nodeFactory)

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
        styledArea.appendAPLValueEnd(v, TextStyle(TextStyle.Type.RESULT))
    }

    fun addExceptionResult(e: Exception) {
        val message = if (e is APLGenericException) {
            e.formattedError()
        } else {
            "Exception from KAP engine: ${e.message}"
        }
        styledArea.appendErrorMessage(message)
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

    class CodeSegmentOps : SegmentOpsBase<EditorContent, TextStyle>(EditorContent.makeBlank()), TextOps<EditorContent, TextStyle> {
        override fun length(seg: EditorContent): Int {
            return seg.length()
        }

        override fun joinSeg(currentSeg: EditorContent, nextSeg: EditorContent): Optional<EditorContent> {
            return currentSeg.joinSegment(nextSeg)
        }

        override fun realGetText(seg: EditorContent): String? {
            return seg.realGetText()
        }

        override fun realCharAt(seg: EditorContent, index: Int): Char {
            return seg.realCharAt(index)
        }

        override fun realSubSequence(seg: EditorContent, start: Int, end: Int): EditorContent {
            return seg.realSubsequence(start, end)
        }

        override fun create(text: String): EditorContent {
            return EditorContent.makeString(text)
        }
    }
}
