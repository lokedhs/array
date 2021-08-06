package array.gui.arrayedit

import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.util.Callback

class ArrayEditorCellFactory : Callback<TableColumn<ArrayEditorRow, ArrayEditorCell>, TableCell<ArrayEditorRow, ArrayEditorCell>> {
    override fun call(param: TableColumn<ArrayEditorRow, ArrayEditorCell>): TableCell<ArrayEditorRow, ArrayEditorCell> {
        return ArrayEditorTableCell()
    }
}
