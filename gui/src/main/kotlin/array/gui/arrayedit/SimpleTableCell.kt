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
            graphic = null
        } else {
            val v = item.value
            graphic = when {
                v.isStringValue() -> Text(v.toStringValue()).apply { fill = Color.RED }
                v is APLNumber -> Text(v.formatted(FormatStyle.PRETTY)).apply { alignment = Pos.BASELINE_RIGHT }
                else -> Text(v.formatted(FormatStyle.PRETTY))
            }
        }
    }
}
