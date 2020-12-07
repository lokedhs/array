package array.gui

import array.*
import javafx.application.Platform
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

    val resultList: ResultList3

    val inputFont: Font
    val engine: Engine
    val functionListWindow: FunctionListWindow
    val keyboardHelpWindow: KeyboardHelpWindow
    val aboutWindow: AboutWindow
    val calculationQueue: CalculationQueue
    val sourceEditors = ArrayList<SourceEditor>()

    init {
        engine = Engine()
        engine.addLibrarySearchPath("../array/standard-lib")
        initCustomFunctions()
        engine.parseAndEval(StringSourceLocation("use(\"standard-lib.kap\")"), false)

        engine.standardOutput = SendToMainCharacterOutput()
        calculationQueue = CalculationQueue(engine)

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

        calculationQueue.start()
        stage.onCloseRequest = EventHandler { calculationQueue.stop() }

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
        selectFile()?.let { file ->
            val editor = SourceEditor(this)
            editor.setFile(file)
            editor.show()
        }
    }

    private fun openSettings() {
        println("Settings panel not implemented")
    }

    fun sendInput(text: String) {
        evalSource(StringSourceLocation(text))
    }

    fun evalSource(source: SourceLocation, linkNewContext: Boolean = false) {
        calculationQueue.pushRequest(source, linkNewContext) { result ->
            if (result is Either.Right) {
                result.value.printStackTrace()
            }
            Platform.runLater { displayResult(result) }
        }
    }

    private fun displayResult(result: Either<APLValue, Exception>) {
        when (result) {
            is Either.Left -> resultList.addResult(result.value)
            is Either.Right -> {
                val ex = result.value
                resultList.addExceptionResult(ex)
                if (ex is APLGenericException) {
                    if (ex.pos != null) {
                        val pos = ex.pos
                        if (pos != null) {
                            highlightSourceLocation(pos, ex.message ?: "no error message")
                        }
                    }
                    if (ex is APLEvalException) {
                        StackTrace.makeStackTraceWindow(this, ex)
                    }
                }
            }
        }
    }

    fun highlightSourceLocation(pos: Position, message: String? = null) {
        val sourceLocation = pos.source
        if (sourceLocation is SourceEditor.EditorSourceLocation) {
            sourceLocation.editor?.let { editor ->
                sourceEditors.forEach { e ->
                    if (e === editor) {
                        editor.highlightError(pos, message)
                    }
                }
            }
        }

    }

    private fun initCustomFunctions() {
        initGraphicCommands(this)
    }

    private inner class ClientRenderContextImpl : ClientRenderContext {
        private val extendedInput = ExtendedCharsKeyboardInput()

        override fun engine() = engine
        override fun font() = inputFont
        override fun extendedInput() = extendedInput
    }

    private inner class SendToMainCharacterOutput : CharacterOutput {
        override fun writeString(s: String) {
            Platform.runLater {
                resultList.addOutput(s)
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            ClientApplication.main(args)
        }
    }
}
