package array.gui.arrayedit

import array.APLNumber
import array.FormatStyle
import array.isStringValue
import array.toStringValue
import javafx.geometry.Pos
import javafx.scene.control.TableCell
import javafx.scene.paint.Color
import javafx.scene.text.Text

class ArrayEditorTableCell : TableCell<ArrayEditorRow, ArrayEditorCell>() {
    override fun updateItem(item: ArrayEditorCell?, empty: Boolean) {
        super.updateItem(item, empty)
        if (item == null) {
            text = ""
        } else {
            val v = item.value
            when {
                v.isStringValue() -> {
                    text = v.toStringValue()
                    textFill = Color.RED
                    alignment = Pos.BASELINE_LEFT
                }
                v is APLNumber -> {
                    text = v.formatted(FormatStyle.PRETTY)
                    textFill = Color.BLACK
                    alignment = Pos.BASELINE_RIGHT
                }
                else -> {
                    text = v.formatted(FormatStyle.PRETTY)
                    textFill = Color.BLACK
                    alignment = Pos.BASELINE_LEFT
                }
            }
        }
    }
}
