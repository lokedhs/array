package array.gui.arrayedit

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.scene.control.TableColumn
import javafx.util.Callback

class ArrayEditorCellValueFactory(val col: Int) :
    Callback<TableColumn.CellDataFeatures<ArrayEditorRow, ArrayEditorCell>, ObservableValue<ArrayEditorCell>> {

    override fun call(param: TableColumn.CellDataFeatures<ArrayEditorRow, ArrayEditorCell>): ObservableValue<ArrayEditorCell> {
        val rowValue = param.value
        return SimpleObjectProperty(ArrayEditorCell(rowValue.values[col]))
    }
}
