package array.gui.arrayedit

import array.APLValue
import array.gui.Client
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.Clipboard
import javafx.stage.Stage
import org.jsoup.Jsoup

class ArrayEditor {
    private lateinit var stage: Stage

    @FXML
    @JvmField
    var table: TableView<ArrayEditorRow>? = null

    @FXML
    @JvmField
    var variableField: TextField? = null

    var client: Client? = null

    fun show() {
        stage.show()
    }

    fun loadFromVariable(@Suppress("UNUSED_PARAMETER") event: ActionEvent) {
        println("Variable name: ${variableField?.text}")
        val name = variableField!!.text
        client!!.calculationQueue.pushReadVariableRequest(name) { result ->
            if (result != null) {
                val v = result.collapse()
                Platform.runLater {
                    loadArray(v)
                }
            }
        }
    }

    fun loadArray(value: APLValue) {
        val d = value.dimensions
        if (d.size != 2) {
            throw IllegalArgumentException("Only rank-2 arrays can be loaded")
        }

        val columnLabels = value.labels?.labels?.get(1)
        val colList = (0 until d[1]).map { index ->
            TableColumn<ArrayEditorRow, ArrayEditorCell>().apply {
                cellValueFactory = ArrayEditorCellValueFactory(index)
                cellFactory = ArrayEditorCellFactory()
                text = columnLabels?.get(index)?.title ?: index.toString()
            }
        }

        table!!.columns.setAll(colList)

        val rows = (0 until d[0]).map { i -> ArrayEditorRow(value, i, d[1]) }.toTypedArray()
        table!!.items = FXCollections.observableArrayList()
        table!!.items.setAll(*rows)
    }

    private fun pasteToTable() {
        val clipboard = Clipboard.getSystemClipboard()
        when {
            clipboard.hasHtml() -> {
                pasteHtml(clipboard.html)
            }
        }
    }

    private fun pasteHtml(html: String) {
        val doc = Jsoup.parse(html)
        val result = htmlTableToArray(doc)
        if (result != null) {
            loadArray(result)
        }
    }

    companion object {
        private fun makeArrayEditor(client: Client): ArrayEditor {
            val loader = FXMLLoader(ArrayEditor::class.java.getResource("arrayeditor.fxml"))
            val root = loader.load<Parent>()
            val controller = loader.getController<ArrayEditor>()

            controller.client = client
            controller.stage = Stage()
            val scene = Scene(root, 800.0, 300.0)
            controller.stage.title = "Array Editor"
            controller.stage.scene = scene

            controller.table!!.contextMenu = ContextMenu(MenuItem("Paste").apply { onAction = EventHandler { controller.pasteToTable() } })

            return controller
        }

        fun open(client: Client, value: APLValue? = null): ArrayEditor {
            return makeArrayEditor(client).apply {
                if (value != null) {
                    loadArray(value)
                }
                show()
            }
        }
    }
}
