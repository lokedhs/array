package array.gui

import array.APLValue
import javafx.scene.text.Text
import javafx.scene.text.TextFlow

class ResultList2(val context: ClientRenderContext) : TextFlow() {
    fun addResult(text: String, v: APLValue) {
        children.add(TextWithStyle(text + "\n"))
        children.add(TextWithStyle(v.formatted() + "\n"))
    }
    
    inner class TextWithStyle(s: String) : Text(s) {
        init {
            font = context.font()
        }
    }
}
