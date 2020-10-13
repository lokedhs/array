package array.gui

import array.*
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.image.WritableImage
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.min

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

    fun updateContent(w: Int, h: Int, content: DoubleArray) {
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
            v.updateContent(bDimensions[1], bDimensions[0], b.toDoubleArray(pos))
            return b
        }
    }

    // ∇ range (low;high;v) { low+(⍳v)÷(v÷(high-low)) }
    // ∇ m (x) { z←r←n←0 ◊ while((r ≤ 2) ∧ (n < 20)) { z ← x+z⋆2 ◊ r ← |z ◊ n←n+1} ◊ n÷20 }
    // m¨(0J1×range(-2;2;200)) ∘.+ range(-2;2;200)
    override fun make(pos: Position) = DrawGraphicFunctionImpl(pos)
}

class GraphicWindow(width: Int, height: Int) {
    private var content = AtomicReference<Content?>()

    init {
        Platform.runLater {
            content.set(Content(width, height))
        }
    }

    private inner class Content(width: Int, height: Int) {
        val stage = Stage()
        val canvas: Canvas
        val image: WritableImage

        init {
            image = WritableImage(width, height)
            canvas = Canvas(width.toDouble(), height.toDouble())
            val border = BorderPane().apply {
                center = canvas
            }
            stage.scene = Scene(border, width.toDouble(), height.toDouble())
            stage.show()
        }

        fun repaintCanvas(width: Int, height: Int, array: DoubleArray) {
            val imageWidth = image.width
            val imageHeight = image.height
            val xStride = width.toDouble() / imageWidth
            val yStride = height.toDouble() / imageHeight
            val pixelWriter = image.pixelWriter
            for (y in 0 until imageHeight.toInt()) {
                for (x in 0 until imageWidth.toInt()) {
                    val value = array[(y * yStride).toInt() * width + (x * xStride).toInt()]
                    val valueByte = min(max(value * 256, 0.0), 255.0).toInt() and 0xFF
                    val colour = (0xFF shl 24) or (valueByte shl 16) or (valueByte shl 8) or valueByte
                    pixelWriter.setArgb(x, y, colour)
                }
            }
            canvas.graphicsContext2D.drawImage(image, 0.0, 0.0)
        }
    }

    fun repaintContent(width: Int, height: Int, array: DoubleArray) {
        content.get()?.repaintCanvas(width, height, array)
    }
}

fun initGraphicCommands(client: Client) {
    val engine = client.engine
    val guiNamespace = engine.makeNamespace("gui")
    engine.registerFunction(engine.internSymbol("makeGraphic", guiNamespace), MakeGraphicFunction())
    engine.registerFunction(engine.internSymbol("drawArray", guiNamespace), DrawGraphicFunction())
}
