package array.gui

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import javafx.stage.Stage

class Client : Application() {
    override fun start(stage: Stage) {
        val label = Label("Test label")
        val scene = Scene(StackPane(label), 400.0, 300.0)
        stage.scene = scene
        stage.show()
    }
}
