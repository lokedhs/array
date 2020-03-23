package array.gui

import javafx.fxml.FXMLLoader
import javafx.geometry.Insets
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.paint.Color
import javafx.stage.Stage

class KeyboardHelpWindow(val renderContext: ClientRenderContext) {
    private val stage: Stage

    init {
        stage = Stage()
        val root: Parent = FXMLLoader.load(javaClass.getResource("keyboard.fxml"))
        val scene = Scene(root, 600.0, 300.0)
        stage.title = "Keyboard Layout"
        stage.scene = scene
    }

    fun show() {
        stage.show()
    }
}

class KeyboardHelp {

}

class KeyboardButtonLabel : Label() {
    private var upperLabel: String = ""
    private var lowerLabel: String = ""

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
        text = upperLabel + "\n" + lowerLabel
    }
}
