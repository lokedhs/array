package array.gui

import array.Engine
import array.Symbol
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TextArea
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.stage.Stage

class FunctionEditor(val renderContext: ClientRenderContext, engine: Engine, name: Symbol? = null) {
    private val stage: Stage
    private val textArea: TextArea

    init {
        stage = Stage()

        textArea = TextArea().apply {
            font = renderContext.font()
            renderContext.extendedInput().addEventHandlerToNode(this)
        }
        val buttonPanel = HBox().apply {
            val saveButton = Button("Save").apply {
                onAction = EventHandler { saveAndClose() }
            }
            children.add(saveButton)
        }
        val border = BorderPane().apply {
            center = textArea
            bottom = buttonPanel
        }
        stage.scene = Scene(border, 400.0, 400.0)
    }

    fun show() {
        stage.show()
    }

    fun saveAndClose() {
        // need to parse the function here
    }
}
