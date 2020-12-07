package array.gui

import array.APLEvalException
import array.CallStackElement
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.stage.Stage
import javafx.util.Callback

class StackTrace {
    @FXML
    @JvmField
    var stackTraceTable: TableView<StackTraceRow>? = null

    @FXML
    @JvmField
    var stackTraceLevelColumn: TableColumn<StackTraceRow, Int>? = null

    @FXML
    @JvmField
    var stackTraceNameColumn: TableColumn<StackTraceRow, String>? = null

    companion object {
        fun makeStackTraceWindow(client: Client, ex: APLEvalException) {
            val loader = FXMLLoader(StackTrace::class.java.getResource("stack.fxml"))
            val root = loader.load<Parent>()
            val controller = loader.getController<StackTrace>()
            val rows = ArrayList<StackTraceRow>().apply {
                add(StackTraceRow(0, CallStackElement(ex.formattedError(), ex.pos), ex.formattedError()))
                ex.callStack?.let { callStack ->
                    callStack.asReversed().forEachIndexed { i, element ->
                        add(StackTraceRow(i + 1, element, null))
                    }
                }
            }
            controller.stackTraceTable!!.items = FXCollections.observableArrayList(rows)
            controller.stackTraceLevelColumn!!.apply {
                cellValueFactory = StackTraceLevelCellValueFactory()
                cellFactory = StackTraceLevelCellFactory()
            }
            controller.stackTraceNameColumn!!.apply {
                cellValueFactory = StackTraceNameCellValueFactory()
                cellFactory = StackTraceNameCellFactory()
            }
            @Suppress("UNUSED_ANONYMOUS_PARAMETER")
            val listener = ChangeListener<StackTraceRow> { observable, oldValue, newValue ->
                val entryPos = newValue.entry.pos
                if (entryPos != null) {
                    client.highlightSourceLocation(entryPos, newValue.message)
                }
            }
            controller.stackTraceTable!!.selectionModel.selectedItemProperty().addListener(listener)

            val stage = Stage()
            val scene = Scene(root, 800.0, 300.0)
            stage.title = "Stack Trace"
            stage.scene = scene
            stage.show()
        }
    }
}

data class StackTraceRow(val level: Int, val entry: CallStackElement, val message: String?)

/////////////////////////////////////////////
// Level
/////////////////////////////////////////////

class StackTraceLevelCellFactory : Callback<TableColumn<StackTraceRow, Int>, TableCell<StackTraceRow, Int>> {
    override fun call(param: TableColumn<StackTraceRow, Int>): TableCell<StackTraceRow, Int> {
        return StackTraceLevelCell()
    }
}

class StackTraceLevelCellValueFactory : Callback<TableColumn.CellDataFeatures<StackTraceRow, Int>, ObservableValue<Int>> {
    override fun call(param: TableColumn.CellDataFeatures<StackTraceRow, Int>): ObservableValue<Int> {
        return SimpleObjectProperty(param.value.level)
    }
}

class StackTraceLevelCell : TableCell<StackTraceRow, Int>() {
    override fun updateItem(item: Int?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            text = null
            graphic = null
        } else {
            text = item.toString()
        }
    }
}

/////////////////////////////////////////////
// Name
/////////////////////////////////////////////

class StackTraceNameCellFactory : Callback<TableColumn<StackTraceRow, String>, TableCell<StackTraceRow, String>> {
    override fun call(param: TableColumn<StackTraceRow, String>): TableCell<StackTraceRow, String> {
        return StackTraceNameCell()
    }
}

class StackTraceNameCellValueFactory : Callback<TableColumn.CellDataFeatures<StackTraceRow, String>, ObservableValue<String>> {
    override fun call(param: TableColumn.CellDataFeatures<StackTraceRow, String>): ObservableValue<String> {
        return SimpleObjectProperty(param.value.entry.name)
    }
}

class StackTraceNameCell : TableCell<StackTraceRow, String>() {
    override fun updateItem(item: String?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            text = null
            graphic = null
        } else {
            text = item
        }
    }
}
