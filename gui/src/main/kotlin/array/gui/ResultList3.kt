package array.gui

import array.APLGenericException
import array.APLValue
import array.FormatStyle
import array.gui.styledarea.HistoryListener
import array.gui.styledarea.ParStyle
import array.gui.styledarea.ROStyledArea
import array.gui.styledarea.TextStyle
import javafx.scene.Node
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
            //println("accept: t=${t}, u=${u}")
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

        //styledArea.isEditable = false
        styledArea.isWrapText = false

        scrollArea = VirtualizedScrollPane(styledArea)
    }

    private fun processCommand(text: String) {
        if (text.trim().isNotBlank()) {
            println("adding command: $text, size = ${history.size}")
            history.add(text)
            historyPos = history.size
            pendingInput = null
            addInput(text)
            client.sendInput(text)
        }
        styledArea.displayPrompt()
    }

    fun getNode() = scrollArea

    fun addResult(v: APLValue) {
        styledArea.withUpdateEnabled {
            styledArea.appendTextEnd(v.formatted(FormatStyle.PRETTY) + "\n", TextStyle(TextStyle.Type.RESULT))
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
            styledArea.appendTextEnd(text + "\n", TextStyle(TextStyle.Type.LOG_INPUT))
        }
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
