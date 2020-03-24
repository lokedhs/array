package array.gui

import array.APLGenericException
import array.APLValue
import javafx.scene.text.Text
import javafx.scene.text.TextFlow

class ResultList2(private val context: ClientRenderContext) : TextFlow() {

    private fun addInput(text: String) {
        children.add(textWithStyle(text + "\n"))
    }

    fun addResult(text: String, v: APLValue) {
        addInput(text)
        children.add(textWithStyle(v.formatted(APLValue.FormatStyle.PRETTY) + "\n"))
    }

    fun addResult(text: String, exception: APLGenericException) {
        addInput(text)
        val errorMessage = textWithStyle(exception.formattedError() + "\n").apply {
            style = "-fx-fill: #ff0000;"
        }
        children.add(errorMessage)
    }

    private fun textWithStyle(s: String): Text {
        return Text(s).apply {
            font = context.font()
        }
    }
}
