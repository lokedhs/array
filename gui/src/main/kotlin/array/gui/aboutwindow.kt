package array.gui

import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

class AboutWindow {
    private val stage: Stage = Stage()

    init {
        val loader = FXMLLoader(javaClass.getResource("about_window.fxml"))
        val root: Parent = loader.load()
        val scene = Scene(root, 300.0, 180.0)
        stage.title = "About KAP"
        stage.scene = scene
    }

    fun show() {
        stage.show()
    }
}

class AboutWindowController
