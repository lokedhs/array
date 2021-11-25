package array.gui.graphics

import array.*
import array.gui.Client
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.image.WritableImage
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

class GraphicWindowAPLValue(engine: Engine, width: Int, height: Int) : APLSingleValue() {
    val window = GraphicWindow(engine, width, height)

    override val aplValueType: APLValueType
        get() = APLValueType.INTERNAL

    override fun formatted(style: FormatStyle) = "graphic-window"
    override fun compareEquals(reference: APLValue) = reference is GraphicWindowAPLValue && window === reference.window
    override fun makeKey() = APLValueKeyImpl(this, window)

    fun updateContent2D(w: Int, h: Int, content: DoubleArray) {
        Platform.runLater {
            window.repaintContent2D(w, h, content)
        }
    }

    fun updateContent3D(w: Int, h: Int, content: DoubleArray) {
        Platform.runLater {
            window.repaintContent3D(w, h, content)
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
            val width = a.valueAtInt(0, pos)
            val height = a.valueAtInt(1, pos)
            return GraphicWindowAPLValue(context.engine, width, height)
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
            when (bDimensions.size) {
                2 -> {
                    v.updateContent2D(bDimensions[1], bDimensions[0], b.toDoubleArray(pos))
                }
                3 -> {
                    if (bDimensions[2] != 3) {
                        throwAPLException(APLIllegalArgumentException("Colour arrays needs must have a third dimension of size 3", pos))
                    }
                    v.updateContent3D(bDimensions[1], bDimensions[0], b.toDoubleArray(pos))
                }
                else -> {
                    throw InvalidDimensionsException("Right argument must be 2 or 3-dimensional", pos)
                }
            }

            return b
        }
    }

    override fun make(pos: Position) = DrawGraphicFunctionImpl(pos)
}

typealias EventType = KClass<out KapWindowEvent>

class GraphicWindow(val engine: Engine, width: Int, height: Int) {
    private var content = AtomicReference<Content?>()
    private val events = LinkedList<KapWindowEvent>()
    private val enabledEvents = HashSet<EventType>()
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    init {
        Platform.runLater {
            content.set(Content(width, height))
        }
    }

    fun nextEvent(): KapWindowEvent? {
        return lock.withLock {
            events.poll()
        }
    }

    fun waitForEvent(): KapWindowEvent {
        lock.withLock {
            while (events.isEmpty()) {
                condition.await()
            }
            return events.removeFirst()
        }
    }

    fun addEnabledEvent(v: EventType): Boolean {
        return lock.withLock {
            enabledEvents.add(v)
        }
    }

    fun removeEnabledEvent(v: EventType): Boolean {
        return lock.withLock {
            enabledEvents.remove(v)
        }
    }

    fun publishEventIfEnabled(event: KapWindowEvent) {
        lock.withLock {
            if (enabledEvents.contains(event::class)) {
                events.add(event)
                condition.signal()
            }
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
            val scene = Scene(border, width.toDouble(), height.toDouble())
            scene.addEventFilter(KeyEvent.KEY_PRESSED, ::processKeyPress)
            stage.scene = scene
            stage.show()
        }

        fun repaintCanvas2D(width: Int, height: Int, array: DoubleArray) {
            assert(array.size == width * height)
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

        fun repaintCanvas3D(width: Int, height: Int, array: DoubleArray) {
            assert(array.size == width * height * 3)
            val lineWidth = width * 3
            val imageWidth = image.width
            val imageHeight = image.height
            val xStride = width.toDouble() / imageWidth
            val yStride = height.toDouble() / imageHeight
            val pixelWriter = image.pixelWriter
            for (y in 0 until imageHeight.toInt()) {
                for (x in 0 until imageWidth.toInt()) {
                    val offset = (y * yStride).toInt() * lineWidth + (x * xStride).toInt() * 3
                    val valueR = min(max((array[offset] * 255).toInt(), 0), 255) and 0xFF
                    val valueG = min(max((array[offset + 1] * 255).toInt(), 0), 255) and 0xFF
                    val valueB = min(max((array[offset + 2] * 255).toInt(), 0), 255) and 0xFF
                    val colour = (0xFF shl 24) or (valueR shl 16) or (valueG shl 8) or valueB
                    pixelWriter.setArgb(x, y, colour)
                }
            }
            canvas.graphicsContext2D.drawImage(image, 0.0, 0.0)
        }

        private fun processKeyPress(event: KeyEvent) {
            publishEventIfEnabled(KapKeyEvent(event))
        }
    }

    fun repaintContent2D(width: Int, height: Int, array: DoubleArray) {
        content.get()?.repaintCanvas2D(width, height, array)
    }

    fun repaintContent3D(width: Int, height: Int, array: DoubleArray) {
        content.get()?.repaintCanvas3D(width, height, array)
    }
}

fun initGraphicCommands(client: Client) {
    val engine = client.engine
    val guiNamespace = engine.makeNamespace("gui")

    fun addFn(name: String, fn: APLFunctionDescriptor) {
        engine.registerFunction(engine.internSymbol(name, guiNamespace), fn)
    }

    addFn("create", MakeGraphicFunction())
    addFn("draw", DrawGraphicFunction())
    addFn("nextEvent", ReadEventFunction())
    addFn("nextEventBlocking", ReadEventBlockingFunction())
    addFn("enableEvents", EnableEventsFunction())
    addFn("disableEvents", DisableEventsFunction())
}
