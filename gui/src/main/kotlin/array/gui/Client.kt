package array.gui

import array.APLValue
import array.Engine
import array.msofficereader.LoadExcelFileFunction
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import javafx.stage.Stage

class Client(val application: ClientApplication, val stage: Stage)  {
    private val resultList: ResultList2
    private var contentScrollPane: ScrollPane
    private val entryTextField: TextField
    private val inputFont: Font
    private val engine = Engine()
    private val functionListWindow: FunctionListWindow;
    private val renderContext = ClientRenderContextImpl()

    init {
        engine.registerFunction(engine.internSymbol("loadExcelFile"), LoadExcelFileFunction())

        val fontIn = Client::class.java.getResourceAsStream("fonts/FreeMono.otf")
        inputFont = fontIn.use { Font.loadFont(it, 18.0) }

        resultList = ResultList2(renderContext)
        contentScrollPane = ScrollPane(resultList)

        entryTextField = TextField().apply {
            font = inputFont
            onAction = EventHandler { sendEntry() }
            renderContext.extendedInput().addEventHandlerToNode(this)
        }
        HBox.setHgrow(entryTextField, Priority.ALWAYS)

        stage.title = "Test ui"

        val sendButton = Button("Submit").apply {
            onAction = EventHandler { sendEntry() }
            maxHeight = Double.MAX_VALUE
        }
        HBox.setHgrow(entryTextField, Priority.ALWAYS)

        val inputContainer = HBox(entryTextField, sendButton).apply {
            padding = Insets(5.0, 5.0, 5.0, 5.0)
            spacing = 15.0
        }

        val border = BorderPane().apply {
            top = makeMenuBar()
            center = contentScrollPane
            bottom = inputContainer
        }

        functionListWindow = FunctionListWindow.create(renderContext, engine)

        stage.scene = Scene(border, 600.0, 800.0)
        stage.show()
    }

    private fun makeMenuBar(): MenuBar {
        return MenuBar().apply {
            val fileMenu = Menu("File").apply {
                val closeItem = MenuItem("Close").apply {
                    onAction = EventHandler { stage.close() }
                }
                items.add(closeItem)
            }
            menus.add(fileMenu)
            
            val windowMenu = Menu("Window").apply {
                items.add(MenuItem("Keyboard"))
                val functionsItem = MenuItem("Functions").apply {
                    onAction = EventHandler { functionListWindow.show() }
                }
                items.add(functionsItem)
            }
            menus.add(windowMenu)
        }
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
        private val extendedInput = ExtendedCharsKeyboardInput()

        override fun engine() = engine
        override fun font() = inputFont
        override fun extendedInput() = extendedInput

        override fun valueClickCallback(value: APLValue) {
            entryTextField.text = value.toAPLExpression()
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            ClientApplication.main(args)
        }
    }
}
