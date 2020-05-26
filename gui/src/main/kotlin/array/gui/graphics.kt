package array.gui

import array.*
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.layout.BorderPane
import javafx.scene.paint.Color
import javafx.stage.Stage

class GraphicWindowAPLValue(width: Int, height: Int) : APLSingleValue() {
    private val window: GraphicWindow

    init {
        window = GraphicWindow(width, height)
    }

    override val aplValueType: APLValueType
        get() = APLValueType.INTERNAL

    override fun formatted(style: FormatStyle) = "graphic-window"

    override fun compareEquals(reference: APLValue) = reference is GraphicWindowAPLValue && window === reference.window

    override fun makeKey() = window

    fun updateContent(w: Int, h: Int, content: IntArray) {
        Platform.runLater {
            window.repaintContent(w, h, content)
        }
    }
}

class MakeGraphicFunction : APLFunctionDescriptor {
    class MakeGraphicFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val aDimensions = a.dimensions
            if (aDimensions.size != 1 || aDimensions[0] != 2) {
                throw InvalidDimensionsException("Argument must be a two-element vector")
            }
            val width = a.valueAt(0).ensureNumber(pos).asInt()
            val height = a.valueAt(1).ensureNumber(pos).asInt()
            return GraphicWindowAPLValue(width, height)
        }
    }

    override fun make(pos: Position) = MakeGraphicFunctionImpl(pos)
}

class DrawGraphicFunction : APLFunctionDescriptor {
    class DrawGraphicFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val v = a.unwrapDeferredValue()
            if (v !is GraphicWindowAPLValue) {
                throw APLIncompatibleDomainsException("Left argument must be a graphic object", pos)
            }
            val bDimensions = b.dimensions
            if (bDimensions.size != 2) {
                throw InvalidDimensionsException("Right argument must be a two-dimensional array", pos)
            }
            v.updateContent(bDimensions[1], bDimensions[0], b.toIntArray(pos))
            return b
        }
    }

    override fun make(pos: Position) = DrawGraphicFunctionImpl(pos)
}

class GraphicWindow(val width: Int, val height: Int) {
    private var content: Content? = null

    init {
        Platform.runLater {
            content = Content()
        }
    }

    private inner class Content {
        val stage = Stage()
        val canvas: Canvas

        init {
            canvas = Canvas(width.toDouble(), height.toDouble())
            val border = BorderPane().apply {
                center = canvas
            }
            stage.scene = Scene(border, width.toDouble(), height.toDouble())
            stage.show()
        }
    }

    private fun findContent(): Content? {
        synchronized(this) {
            return content
        }
    }

    fun repaintContent(w: Int, h: Int, content: IntArray) {
        val c = findContent() ?: return

        val canvasWidth = c.canvas.width
        val canvasHeight = c.canvas.height
        val cellWidth = canvasWidth / w
        val cellHeight = canvasHeight / h
        val graphicsContext = c.canvas.graphicsContext2D
        val col0 = Color(1.0, 1.0, 1.0, 1.0)
        val col1 = Color(0.0, 0.0, 0.0, 1.0)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val colour = content[y * w + x]
                graphicsContext.fill = if (colour == 0) col0 else col1
                graphicsContext.fillRect(x * cellWidth, y * cellHeight, cellWidth, cellHeight)
            }
        }
    }
}

fun initGraphicCommands(client: Client) {
    val engine = client.engine
    val guiNamespace = engine.makeNamespace("gui")
    engine.registerFunction(engine.internSymbol("makeGraphic", guiNamespace), MakeGraphicFunction())
    engine.registerFunction(engine.internSymbol("drawArray", guiNamespace), DrawGraphicFunction())
}
