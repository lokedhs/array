package array.gui

import array.*
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.layout.BorderPane
import javafx.scene.text.Font
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.io.File

class Client(val application: ClientApplication, val stage: Stage) {
    val renderContext: ClientRenderContext = ClientRenderContextImpl()

    private val resultList: ResultList3

    private val inputFont: Font
    private val engine: Engine
    private val context: RuntimeContext
    private val functionListWindow: FunctionListWindow
    private val keyboardHelpWindow: KeyboardHelpWindow
    private val aboutWindow: AboutWindow

    init {
        engine = Engine()
        engine.addLibrarySearchPath("../array/standard-lib")
        engine.parseString("use(\"standard-lib.kap\")")
        context = engine.makeRuntimeContext()

        engine.standardOutput = SendToMainCharacterOutput()

        val fontIn = javaClass.getResourceAsStream("fonts/FreeMono.otf")
        inputFont = fontIn.use { Font.loadFont(it, 18.0) }

        resultList = ResultList3(this)

        stage.title = "Test ui"

        val border = BorderPane().apply {
            top = makeMenuBar()
            center = resultList.getNode()
        }

        functionListWindow = FunctionListWindow.create(renderContext, engine)
        keyboardHelpWindow = KeyboardHelpWindow(renderContext)
        aboutWindow = AboutWindow()

        stage.scene = Scene(border, 1000.0, 800.0)
        stage.show()
    }

    private fun makeMenuBar(): MenuBar {
        return MenuBar().apply {
            val fileMenu = Menu("File").apply {
                items.add(MenuItem("New").apply {
                    onAction = EventHandler { openNewFile() }
                })
                items.add(MenuItem("Open").apply {
                    onAction = EventHandler { selectAndEditFile() }
                })
                items.add(MenuItem("Settings").apply {
                    onAction = EventHandler { openSettings() }
                })
                items.add(MenuItem("Close").apply {
                    onAction = EventHandler { stage.close() }
                })
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

            val helpMenu = Menu("Help").apply {
                items.add(MenuItem("About").apply {
                    onAction = EventHandler { aboutWindow.show() }
                })
            }
            menus.add(helpMenu)
        }
    }

    private fun openNewFile() {
        val editor = SourceEditor(this)
        editor.show()
    }

    fun selectFile(forSave: Boolean = false): File? {
        val fileSelector = FileChooser().apply {
            title = "Open KAP file"
            selectedExtensionFilter = FileChooser.ExtensionFilter("KAP files", ".kap")
        }
        return if (forSave) {
            fileSelector.showSaveDialog(stage)
        } else {
            fileSelector.showOpenDialog(stage)
        }
    }

    private fun selectAndEditFile() {
        val file = selectFile()
        if (file != null) {
            val editor = SourceEditor(this)
            editor.setFile(file)
            editor.show()
        }
    }

    private fun openSettings() {
        println("Settings panel not implemented")
    }

    fun sendInput(text: String) {
        try {
            evalSource(StringSourceLocation(text))
        } catch (e: APLGenericException) {
            resultList.addResult(e)
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun evalSource(source: SourceLocation, linkNewContext: Boolean = false) {
        val instr = engine.parseWithTokenGenerator(TokenGenerator(engine, source))
        val v = instr.evalWithContext(if (linkNewContext) context.link() else context)
        resultList.addResult(v)
    }

    private inner class ClientRenderContextImpl : ClientRenderContext {
        private val extendedInput = ExtendedCharsKeyboardInput()

        override fun engine() = engine
        override fun runtimeContext() = context
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
