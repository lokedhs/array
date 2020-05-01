package array.gui.tableresult

import array.APLValue
import array.FormatStyle
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.util.Callback

class TableResult(content: APLValue) : TableView<APLRowWrapper>() {
    init {
        val dimensions = content.dimensions
        val rows: Int
        val cols: Int
        if (dimensions.size == 1) {
            rows = 1
            cols = dimensions[0]
        } else {
            rows = dimensions[0]
            cols = dimensions[1]
        }

        val elementList = (0 until rows).map { index -> APLRowWrapper(content, index, cols) }.toTypedArray()
        items = FXCollections.observableArrayList(*elementList)

        val colList = (0 until cols).map { index ->
            TableColumn<APLRowWrapper, APLValueWrapper>().apply {
                cellValueFactory = ResultCellValueFactory(index)
                cellFactory = SimpleResultCellFactory()
                text = index.toString()
            }
        }
        columns.setAll(colList)
    }
}

class SimpleResultCellFactory : Callback<TableColumn<APLRowWrapper, APLValueWrapper>, TableCell<APLRowWrapper, APLValueWrapper>> {
    override fun call(param: TableColumn<APLRowWrapper, APLValueWrapper>): TableCell<APLRowWrapper, APLValueWrapper> {
        return SimpleAPLCell()
    }
}

class SimpleAPLCell : TableCell<APLRowWrapper, APLValueWrapper>() {
    override fun updateItem(item: APLValueWrapper?, empty: Boolean) {
        super.updateItem(item, empty)

        if (empty || item == null) {
            text = null
            graphic = null
        } else {
            text = item.value.formatted(FormatStyle.PRETTY)
        }
    }
}


class ResultCellValueFactory(val col: Int) :
    Callback<TableColumn.CellDataFeatures<APLRowWrapper, APLValueWrapper>, ObservableValue<APLValueWrapper>> {
    override fun call(param: TableColumn.CellDataFeatures<APLRowWrapper, APLValueWrapper>): ObservableValue<APLValueWrapper> {
        val rowWrapper = param.value
        return SimpleObjectProperty(APLValueWrapper(rowWrapper.getValue(col)))
    }
}

class APLRowWrapper(val parent: APLValue, val row: Int, val cols: Int) {
    private val values: ArrayList<APLValue?>

    init {
        values = ArrayList(cols)
        for (i in 0 until cols) {
            values.add(null)
        }
    }

    fun getValue(col: Int): APLValue {
        val v = values[col]
        return if (v == null) {
            val updated = parent.valueAt(row * cols + col).collapse()
            values[col] = updated
            updated
        } else {
            v
        }
    }
}

class APLValueWrapper(val value: APLValue)
