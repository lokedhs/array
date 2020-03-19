package array.gui

import array.APLGenericException
import array.CharacterOutput
import array.Engine
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
    private val resultList: ResultList3

    private val inputFont: Font
    private val engine = Engine()
    private val functionListWindow: FunctionListWindow

    val renderContext: ClientRenderContext = ClientRenderContextImpl()

    init {
        engine.registerFunction(engine.internSymbol("loadExcelFile"), LoadExcelFileFunction())
        engine.standardOutput = SendToMainCharacterOutput()

        val fontIn = Client::class.java.getResourceAsStream("fonts/FreeMono.otf")
        inputFont = fontIn.use { Font.loadFont(it, 18.0) }

        resultList = ResultList3(this)

        stage.title = "Test ui"

        val border = BorderPane().apply {
            top = makeMenuBar()
            center = resultList.getNode()//contentScrollPane
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

    fun sendInput(text: String) {
        try {
            val instr = engine.parseString(text)
            val v = instr.evalWithContext(engine.makeRuntimeContext()).collapse()
            resultList.addResult(v)
        } catch (e: APLGenericException) {
            resultList.addResult(text, e)
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
