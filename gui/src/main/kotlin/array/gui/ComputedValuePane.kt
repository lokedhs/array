package array.gui

import array.APLValue
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane

class ComputedValuePane(context: ClientRenderContext, value: APLValue) : BorderPane() {
    init {
        val dimensions = value.dimensions()
        val contentNode = when (value.rank()) {
            0 -> SingleValueNode(context, value)
//            1 -> TableResult(value)
//            2 -> TableResult(value)
            1 -> ArrayResult(context, 1, dimensions[0]) { value.valueAt(it) }
            2 -> ArrayResult(context, dimensions[0], dimensions[1]) { value.valueAt(it) }
            else -> Label("Invalid type")
        }
        center = contentNode
    }
}
