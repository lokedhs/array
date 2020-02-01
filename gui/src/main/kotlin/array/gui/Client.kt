package array.gui

import array.APLValue
import array.Engine
import javafx.application.Application
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ScrollPane
import javafx.scene.layout.*
import javafx.scene.text.Font
import javafx.stage.Stage


class Client : Application() {
    private val resultList: ResultList
    private val entryTextField: ExtendedCharsInputField
    private val inputFont: Font
    private val engine = Engine()

    init {
        val fontIn = Client::class.java.getResourceAsStream("fonts/FreeMono.otf")
        inputFont = fontIn.use { Font.loadFont(it, 18.0) }

        resultList = ResultList()

        entryTextField = ExtendedCharsInputField().apply {
            font = inputFont
            onAction = EventHandler { sendEntry() }
        }
        HBox.setHgrow(entryTextField, Priority.ALWAYS)
    }

    override fun start(stage: Stage) {
        stage.title = "Test ui"

        val contentScrollPane = ScrollPane(resultList)

        val sendButton = Button("Submit").apply {
            onAction = EventHandler<ActionEvent> { sendEntry() }
            maxHeight = Double.MAX_VALUE
        }
        HBox.setHgrow(sendButton, Priority.ALWAYS)

        val inputContainer = HBox(entryTextField, sendButton).apply {
            padding = Insets(5.0, 5.0, 5.0, 5.0)
            spacing = 15.0
        }

        val border = BorderPane().apply {
            center = contentScrollPane
            bottom = inputContainer
        }

        stage.scene = Scene(border, 400.0, 300.0)
        stage.show()
    }

    private fun sendInput(text: String) {
        try {
            val instr = engine.parseString(text)
            val v = instr.evalWithContext(engine.makeRuntimeContext())
            resultList.addResult(ClientRenderContextImpl(), text, v)
        }
        catch(e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendEntry() {
        sendInput(entryTextField.text)
    }

    private inner class ClientRenderContextImpl : ClientRenderContext {
        override fun font(): Font {
            return inputFont
        }

        override fun valueClickCallback(value: APLValue) {
            entryTextField.text = value.toAPLExpression()
        }
    }
}
