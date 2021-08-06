package array.gui.arrayedit

import array.APLValue
import javafx.collections.FXCollections
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.stage.Stage
import java.lang.IllegalArgumentException

class ArrayEditor {
    private lateinit var stage: Stage

    private val table = TableView<ArrayEditorRow>()
    private val items = FXCollections.observableArrayList<ArrayEditorRow>()

    fun show() {
        stage.show()
    }

    fun loadArray(value: APLValue) {
        val d = value.dimensions
        if(d.size != 2) {
            throw IllegalArgumentException("Only rank-2 arrays can be loaded")
        }

        val columnLabels = value.labels?.labels?.get(0)
        val colList = (0 until d[1]).map { index ->
            TableColumn<ArrayEditorRow, ArrayEditorCell>().apply {
                cellValueFactory = ArrayEditorCellValueFactory(index)
                cellFactory = ArrayEditorCellFactory()
                text = columnLabels?.get(index)?.title ?: index.toString()
            }
        }

        val rows = (0 until d[0]).map {i -> ArrayEditorRow(value, d[1], i)}.toTypedArray()
        items.setAll(*rows)
    }

    companion object {
        fun makeArrayEditor(): ArrayEditor {
            val loader = FXMLLoader(ArrayEditor::class.java.getResource("arrayeditor.fxml"))
            val root = loader.load<Parent>()
            val controller = loader.getController<ArrayEditor>()

            controller.stage = Stage()
            val scene = Scene(root, 800.0, 300.0)
            controller.stage.title = "Array Editor"
            controller.stage.scene = scene

            return controller
        }
    }
}
