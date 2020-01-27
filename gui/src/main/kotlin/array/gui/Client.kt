package array.gui

import array.Engine
import javafx.application.Application
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.GridPane
import javafx.scene.text.Font
import javafx.stage.Stage
import java.io.BufferedInputStream
import java.io.FileInputStream


class Client : Application() {
    private lateinit var engine: Engine

    override fun init() {
        super.init()
        engine = Engine()
    }

    override fun start(stage: Stage) {
        val fontIn = Client::class.java.getResourceAsStream("fonts/FreeMono.otf")
        val font = fontIn.use { Font.loadFont(it, 18.0) }

        stage.title = "Test ui"

        val grid = GridPane().apply {
            alignment = Pos.CENTER
            hgap = 10.0
            vgap = 10.0
            padding = Insets(25.0, 25.0, 25.0, 25.0)
        }

        val content = Label("This is supposed to be the results pane")
        grid.add(content, 0, 0, 2, 1)

        val entryTextField = ExtendedCharsInputField()
        val sendEntry = { sendInput(entryTextField.text) }
        entryTextField.onAction = EventHandler { sendEntry() }
        entryTextField.font = font
        grid.add(entryTextField, 0, 1)

        val sendButton = Button("Submit").apply {
            onAction = EventHandler<ActionEvent> { sendEntry() }
        }
        grid.add(sendButton, 1, 1)

        stage.scene = Scene(grid, 400.0, 300.0)
        stage.show()
    }

    private fun sendInput(text: String) {
        try {
            val instr = engine.parseString(text)
            instr.evalWithContext(engine.makeRuntimeContext())
        }
        catch(e: Exception) {
            e.printStackTrace()
        }
    }
}
