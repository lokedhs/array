package array.gui

import array.APLValue
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.text.Font

class ResultPanel(context: ClientRenderContext, inputString: String, value: APLValue) : VBox() {
    init {
        val label = Label(inputString).apply {
            font = context.font()
        }
        children.add(label)

        children.add(ComputedValuePane(context, value))
    }
}
