package array.gui

import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

class Settings {
    private val settingsController: SettingsController
    private val stage = Stage()

    init {
        val loader = FXMLLoader(Settings::class.java.getResource("settings.fxml"))
        val root = loader.load<Parent>()
        settingsController = loader.getController()

        val scene = Scene(root, 800.0, 300.0)
        stage.title = "Keyboard Layout"
        stage.scene = scene
    }

    fun show() {
        stage.show()
    }
}
