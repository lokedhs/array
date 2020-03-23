package array.gui

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Insets
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.GridPane
import javafx.scene.paint.Color
import javafx.stage.Stage

class KeyboardHelpWindow(val renderContext: ClientRenderContext) {
    private val stage: Stage

    init {
        stage = Stage()
        val loader = FXMLLoader(javaClass.getResource("keyboard.fxml"))
        val root: Parent = loader.load()
        val controller: KeyboardHelp = loader.getController()
        controller.gridPane = root as GridPane
        controller.init(renderContext)
        val scene = Scene(root, 600.0, 300.0)
        stage.title = "Keyboard Layout"
        stage.scene = scene
    }

    fun show() {
        stage.show()
    }
}

class KeyboardHelp {
    @FXML
    @JvmField
    var gridPane: GridPane? = null

    lateinit var renderContext: ClientRenderContext

    fun init(context: ClientRenderContext) {
        renderContext = context
        updateShortcuts()
    }

    fun updateShortcuts() {
        renderContext.extendedInput().keymap.forEach { key, value ->
            gridPane!!.children.forEach key@{ label ->
                if(label is KeyboardButtonLabel) {
                    if(label.getUpperLabel() == key.character) {
                        label.upperAltLabel = value
                        label.updateLabelText()
                        return@key
                    }
                    if(label.getLowerLabel() == key.character) {
                        label.lowerAltLabel = value
                        label.updateLabelText()
                        return@key
                    }
                }
            }
        }
    }
}

class KeyboardButtonLabel : Label() {
    private var upperLabel: String = ""
    private var lowerLabel: String = ""
    var upperAltLabel: String = ""
    var lowerAltLabel: String = ""

    init {
        background = Background(BackgroundFill(Color(0.95, 0.95, 0.95, 1.0), CornerRadii.EMPTY, Insets.EMPTY))
        padding = Insets(2.0, 2.0, 2.0, 2.0)
        maxWidth = Double.MAX_VALUE
    }

    fun setUpperLabel(s: String) {
        upperLabel = s
        updateLabelText()
    }

    fun getUpperLabel() = upperLabel

    fun setLowerLabel(s: String) {
        lowerLabel = s
        updateLabelText()
    }

    fun getLowerLabel() = lowerLabel

    fun updateLabelText() {
        text = (if (upperAltLabel == "") "" else " $upperAltLabel") + upperLabel + "\n" + (if(lowerAltLabel == "") "" else " $lowerAltLabel") + lowerLabel
    }
}
