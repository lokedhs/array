package array.gui

import array.APLGenericException
import array.CharacterOutput
import array.Engine
import array.RuntimeContext
import array.msofficereader.LoadExcelFileFunction
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.layout.BorderPane
import javafx.scene.text.Font
import javafx.stage.Stage

class Client(val application: ClientApplication, val stage: Stage) {
    val renderContext: ClientRenderContext = ClientRenderContextImpl()

    private val resultList: ResultList3

    private val inputFont: Font
    private val engine: Engine
    private val context: RuntimeContext
    private val functionListWindow: FunctionListWindow
    private val keyboardHelpWindow: KeyboardHelpWindow

    init {
        engine = Engine()
        context = engine.makeRuntimeContext()

        engine.registerFunction(engine.internSymbol("loadExcelFile"), LoadExcelFileFunction())
        engine.standardOutput = SendToMainCharacterOutput()

        val fontIn = javaClass.getResourceAsStream("fonts/FreeMono.otf")
        inputFont = fontIn.use { Font.loadFont(it, 18.0) }

        resultList = ResultList3(this)

        stage.title = "Test ui"

        val border = BorderPane().apply {
            top = makeMenuBar()
            center = resultList.getNode()//contentScrollPane
        }

        functionListWindow = FunctionListWindow.create(renderContext, engine)
        keyboardHelpWindow = KeyboardHelpWindow(renderContext)

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
                items.add(MenuItem("Keyboard").apply {
                    onAction = EventHandler { keyboardHelpWindow.show() }
                })
                items.add(MenuItem("Functions").apply {
                    onAction = EventHandler { functionListWindow.show() }
                })
            }
            menus.add(windowMenu)
        }
    }

    fun sendInput(text: String) {
        try {
            val instr = engine.parseString(text)
            val v = instr.evalWithContext(context).collapse()
            resultList.addResult(v)
        } catch (e: APLGenericException) {
            resultList.addResult(e)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private inner class ClientRenderContextImpl : ClientRenderContext {
        private val extendedInput = ExtendedCharsKeyboardInput()

        override fun engine() = engine
        override fun font() = inputFont
        override fun extendedInput() = extendedInput
    }

    private inner class SendToMainCharacterOutput : CharacterOutput {
        override fun writeString(s: String) {
            resultList.addOutput(s)
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            ClientApplication.main(args)
        }
    }
}
