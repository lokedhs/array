package array.gui

import array.APLValue
import javafx.geometry.Insets
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.GridPane
import javafx.scene.paint.Color

class ArrayResult(context: ClientRenderContext, rows: Int, cols: Int, valueReader: (Int) -> APLValue) : GridPane() {
    init {
        padding = Insets(2.0, 2.0, 2.0, 2.0)
        vgap = 1.0
        hgap = 1.0
        background = Background(BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY))

        var i = 0
        for(y in 0 until rows) {
            for(x in 0 until cols) {
                val pane = ComputedValuePane(context, valueReader(i++)).apply {
                    background = Background(BackgroundFill(Color(0.95, 0.95, 0.95, 1.0), CornerRadii.EMPTY, Insets.EMPTY))
                    padding = Insets(2.0, 2.0, 2.0, 2.0)
                    maxWidth = Double.MAX_VALUE
                }
                add(pane, x, y)
            }
        }
    }
}
