package array.gui

import array.APLValue
import javafx.scene.text.Text
import javafx.scene.text.TextFlow

class ResultList2(private val context: ClientRenderContext) : TextFlow() {
    fun addResult(text: String, v: APLValue) {
        children.apply {
            add(textWithStyle(text + "\n"))
            add(textWithStyle(v.formatted() + "\n"))
        }
    }

    private fun textWithStyle(s: String): Text {
        return Text(s).apply {
            font = context.font()
        }
    }
}
