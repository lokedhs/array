package array.gui

import array.APLValue
import array.Engine
import javafx.application.Application
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ScrollPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import javafx.stage.Stage


class Client : Application() {
    private val resultList: ResultList2
    private var contentScrollPane: ScrollPane
    private val entryTextField: ExtendedCharsInputField
    private val inputFont: Font
    private val engine = Engine()

    init {
        val fontIn = Client::class.java.getResourceAsStream("fonts/FreeMono.otf")
        inputFont = fontIn.use { Font.loadFont(it, 18.0) }

        resultList = ResultList2(ClientRenderContextImpl())
        contentScrollPane = ScrollPane(resultList)

        entryTextField = ExtendedCharsInputField().apply {
            font = inputFont
            onAction = EventHandler { sendEntry() }
        }
        HBox.setHgrow(entryTextField, Priority.ALWAYS)
    }

    override fun start(stage: Stage) {
        stage.title = "Test ui"

        val sendButton = Button("Submit").apply {
            onAction = EventHandler { sendEntry() }
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

        stage.scene = Scene(border, 600.0, 800.0)
        stage.show()
    }

    private fun sendInput(text: String) {
        try {
            val instr = engine.parseString(text)
            val v = instr.evalWithContext(engine.makeRuntimeContext())
            resultList.addResult(text, v)
            contentScrollPane.layout()
            contentScrollPane.vvalue = contentScrollPane.vmax
        } catch (e: Exception) {
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
