package array.gui

import array.APLValue
import javafx.geometry.Pos
import javafx.scene.control.Label

class SingleValueNode(context: ClientRenderContext, value: APLValue) : Label() {
    init {
        text = value.formatted(APLValue.FormatStyle.PRETTY)
        font = context.font()
        alignment = Pos.BASELINE_RIGHT
        maxWidth = Double.MAX_VALUE
    }
}
