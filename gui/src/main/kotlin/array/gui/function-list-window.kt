package array.gui

import array.*
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.CheckBoxTableCell
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.stage.Stage
import javafx.util.Callback
import java.util.*

class FunctionListWindow private constructor(val renderContext: ClientRenderContext, engine: Engine) {
    private val stage = Stage()
    private var functionListPanel: FunctionListPanel

    init {
        stage.title = "Functions"

        functionListPanel = FunctionListPanel(engine)
        val buttonPanel = makeButtonPanel()

        val border = BorderPane().apply {
            center = functionListPanel
            bottom = buttonPanel
        }
        stage.scene = Scene(border, 400.0, 400.0)
    }

    private fun makeButtonPanel(): HBox {
        val addButton = Button("Add")
        val editButton = Button("Edit")
        val removeButton = Button("Remove")
        return HBox(addButton, editButton, removeButton)
    }

    fun show() {
        stage.show()
    }

    companion object {
        fun create(renderContext: ClientRenderContext, engine: Engine): FunctionListWindow {
            return FunctionListWindow(renderContext, engine)
        }
    }

}

class FunctionListPanel(engine: Engine) : TableView<FunctionRow>() {
    private val listener: FunctionDefinitionListener

    init {
        val list = engine.getUserDefinedFunctions().map { FunctionRow(it.key, it.value) }.sortedBy { it.name }
        items = FXCollections.observableArrayList(list)

        listener = FunctionListener()
        engine.addFunctionDefinitionListener(listener)

        columns.add(TableColumn<FunctionRow, APLSymbolWrapper>().apply {
            cellValueFactory = SymbolValueFactory()
            cellFactory = Callback { NamespaceNameCell() }
            text = "Namespace"
        })
        columns.add(TableColumn<FunctionRow, APLSymbolWrapper>().apply {
            cellValueFactory = SymbolValueFactory()
            cellFactory = Callback { FunctionNameCell() }
            text = "Name"
        })
    }

    private inner class FunctionListener : FunctionDefinitionListener {
        override fun functionDefined(name: Symbol, fn: APLFunctionDescriptor) {
            if (fn is UserFunction) {
                val functionRow = FunctionRow(name, fn)
                val result = Collections.binarySearch(items, functionRow, { a, b -> a.name.compareTo(b.name) })
                if (result >= 0) {
                    items[result] = functionRow
                } else {
                    items.add(-result - 1, functionRow)
                }
            }
        }

        override fun functionRemoved(name: Symbol) {
            val result = items.indexOfFirst { name == it.name }
            if (result >= 0) {
                items.removeAt(result)
            }
        }
    }
}

class NamespaceNameCell() : TableCell<FunctionRow, APLSymbolWrapper>() {
    override fun updateItem(item: APLSymbolWrapper?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            text = null
            graphic = null
        } else {
            text = item.symbol.namespace.name
        }
    }
}

class FunctionNameCell : TableCell<FunctionRow, APLSymbolWrapper>() {
    override fun updateItem(item: APLSymbolWrapper?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            text = null
            graphic = null
        } else {
            text = item.symbol.symbolName
        }
    }
}

class SymbolValueFactory :
    Callback<TableColumn.CellDataFeatures<FunctionRow, APLSymbolWrapper>, ObservableValue<APLSymbolWrapper>> {
    override fun call(param: TableColumn.CellDataFeatures<FunctionRow, APLSymbolWrapper>): ObservableValue<APLSymbolWrapper> {
        return SimpleObjectProperty(APLSymbolWrapper(param.value.name))
    }
}

data class FunctionRow(val name: Symbol, val aplFunction: UserFunction)
data class APLSymbolWrapper(val symbol: Symbol)
