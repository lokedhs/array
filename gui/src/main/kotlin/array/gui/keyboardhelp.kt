package array.gui

import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.VPos
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.stage.Stage

class KeyboardHelpWindow(renderContext: ClientRenderContext) {
    private val stage = Stage()

    init {
        val loader = FXMLLoader(javaClass.getResource("keyboard.fxml"))
        val root: Parent = loader.load()
        val controller: KeyboardHelp = loader.getController()
        controller.borderPane = root as BorderPane
        val grid = root.center as GridPane
        grid.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE)
        controller.gridPane = grid
        controller.init(renderContext)
        val scene = Scene(root, 800.0, 300.0)
        stage.title = "Keyboard Layout"
        stage.scene = scene
    }

//    init {
//        val bp = BorderPane().apply {
//            style = "-fx-background-color: red;"
//            center = GridPane().apply {
//                style = "-fx-background-color: green;"
//                setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE)
//
//                fun addCell(name: String, col: Int, row: Int, colour: String? = null) {
//                    add(Button(name).also { b ->
//                        if (colour != null) {
//                            b.style = "-fx-background-color: $colour;"
//                        }
//                        GridPane.setConstraints(b, col, row, 1, 1, HPos.CENTER, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS)
//                        b.maxWidth = Double.MAX_VALUE
//                        b.maxHeight = Double.MAX_VALUE
//                    }, col, row)
//                }
//
//                addCell("[0,0]", 0, 0, "blue")
//                addCell("[1,0]", 1, 0)
//                addCell("[0,1]", 0, 1)
//                addCell("[1,1]", 1, 1, "yellow")
//            }
//        }
//        val scene = Scene(bp, 800.0, 600.0)
//        stage.title = "Foo"
//        stage.scene = scene
//    }

    fun show() {
        stage.show()
    }
}

class KeyboardHelp {
    @FXML
    @JvmField
    var borderPane: BorderPane? = null

    @FXML
    @JvmField
    var gridPane: GridPane? = null

    lateinit var renderContext: ClientRenderContext

    fun init(context: ClientRenderContext) {
        renderContext = context
        forEachKeyLabel { label ->
            label.initLabel(renderContext.font())
        }
        updateShortcuts()
    }

    fun updateShortcuts() {
        renderContext.extendedInput().keymap.forEach { (key, value) ->
            forEachKeyLabel key@{ label ->
                if (label.getUpperLabel() == key.character) {
                    label.setAltUpperLabel(value)
                    return@key
                }
                if (label.getLowerLabel() == key.character) {
                    label.setAltLowerLabel(value)
                    return@key
                }
            }
        }
    }

    fun forEachKeyLabel(fn: (KeyboardButtonLabel) -> Unit) {
        gridPane!!.children.forEach { label ->
            if (label is KeyboardButtonLabel) {
                fn(label)
            }
        }
    }
}

class KeyboardButtonLabel : AnchorPane() {
    private val altUpperFx = Label()
    private val altLowerFx = Label()
    private val upperFx = Label()
    private val lowerFx = Label()

    private var altUpperLabel: String = ""
    private var altLowerLabel: String = ""
    private var upperLabel: String = ""
    private var lowerLabel: String = ""
    var clickable: Boolean = true

    init {
        background = Background(BackgroundFill(Color(0.95, 0.95, 0.95, 1.0), CornerRadii.EMPTY, Insets.EMPTY))
        padding = Insets(2.0, 2.0, 2.0, 2.0)
        maxWidth = Double.MAX_VALUE
        maxHeight = Double.MAX_VALUE
        GridPane.setHalignment(this, HPos.CENTER)
        GridPane.setValignment(this, VPos.CENTER)
        GridPane.setHgrow(this, Priority.ALWAYS)
        GridPane.setVgrow(this, Priority.ALWAYS)

        children.add(altUpperFx)
        children.add(altLowerFx)
        children.add(upperFx)
        children.add(lowerFx)

        val margin = 2.0
        setTopAnchor(altUpperFx, margin)
        setLeftAnchor(altUpperFx, margin)
        setBottomAnchor(altLowerFx, margin)
        setLeftAnchor(altLowerFx, margin)
        setTopAnchor(upperFx, margin)
        setRightAnchor(upperFx, margin)
        setBottomAnchor(lowerFx, margin)
        setRightAnchor(lowerFx, margin)
    }

    private fun handleClick(s: String) {
        println("send '${s}' to window")
    }

    @FXML
    fun setUpperLabel(s: String) {
        upperLabel = s
        upperFx.text = s
    }

    @FXML
    fun getUpperLabel() = upperLabel

    @FXML
    fun setLowerLabel(s: String) {
        lowerLabel = s
        lowerFx.text = s
    }

    @FXML
    fun getLowerLabel() = lowerLabel

    fun setAltUpperLabel(s: String) {
        altUpperLabel = s
        altUpperFx.text = s
    }

    fun setAltLowerLabel(s: String) {
        altLowerLabel = s
        altLowerFx.text = s
    }

    fun initLabel(font: Font) {
        altUpperFx.font = font
        altLowerFx.font = font
        upperFx.font = font
        lowerFx.font = font

        if (clickable) {
            altUpperFx.apply {
                styleClass.setAll("keyboard-button-label-clickable")
                onMouseClicked = EventHandler { handleClick(altUpperLabel) }
            }
            altLowerFx.apply {
                styleClass.setAll("keyboard-button-label-clickable")
                onMouseClicked = EventHandler { handleClick(altLowerLabel) }
            }
            upperFx.apply {
                styleClass.setAll("keyboard-button-label-clickable")
                onMouseClicked = EventHandler { handleClick(upperLabel) }
            }
            lowerFx.apply {
                styleClass.setAll("keyboard-button-label-clickable")
                onMouseClicked = EventHandler { handleClick(lowerLabel) }
            }
        } else {
            styleClass.setAll("keyboard-button-label")
        }
    }
}
