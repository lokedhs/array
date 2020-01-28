package array

typealias Dimensions = Array<Int>

interface APLValue {
    fun dimensions(): Dimensions
    fun rank(): Int = dimensions().size
    fun valueAt(p: Int): APLValue
    fun size(): Int = if (rank() == 0) 1 else dimensions().reduce { a, b -> a * b }
    fun asDouble(): Double =
        throw IncompatibleTypeException("Type cannot be converted to a number: ${this::class.qualifiedName}")

    fun formatted(): String = arrayAsString(this)
    fun collapse(): APLValue
}

abstract class APLSingleValue : APLValue {
    override fun dimensions() = emptyArray<Int>()
    override fun valueAt(p: Int) = throw APLIndexOutOfBoundsException("Reading index $p from scalar")
    override fun size() = 1
    override fun rank() = 0
    override fun collapse() = this
}

abstract class APLArray : APLValue {
    override fun collapse(): APLValue {
        return if(rank() == 0) {
            valueAt(0)
        } else {
            APLArrayImpl(dimensions()) { valueAt(it) }
        }
    }
}

fun arrayAsString(array: APLValue): String {
    val buf = StringBuilder()

    fun outputLine(start: Int, end: Int) {
        var first = true
        for (i in start until end) {
            val value = array.valueAt(i)
            if (first) first = false else buf.append(" ")
            buf.append(value.formatted())
        }
    }

    fun output2D() {
        val d = array.dimensions()
        val height = d[0]
        val width = d[1]
        var first = true
        for (y in 0 until height) {
            if (first) first = false else buf.append("\n")
            outputLine(y * width, (y + 1) * width)
        }
    }

    when {
        array.rank() == 0 -> buf.append(array.valueAt(0).formatted())
        array.rank() == 1 -> outputLine(0, array.dimensions()[0])
        array.rank() == 2 -> output2D()
        else -> TODO("Printing of arrays of dimensions >2 is not supported")
    }

    return buf.toString()
}

class ConstantArray(
    private val dimensions: Dimensions,
    private val value: APLValue
) : APLArray() {

    override fun dimensions() = dimensions

    override fun valueAt(p: Int) = value
}

class APLArrayImpl(
    private val dimensions: Dimensions,
    init: (Int) -> APLValue
) : APLArray() {

    private val values: Array<APLValue>

    init {
        values = if (dimensions.isEmpty()) {
            Array(1, init)
        } else {
            Array(dimensions.reduce { a, b -> a * b }, init)
        }
    }

    override fun dimensions() = dimensions
    override fun valueAt(p: Int) = values[p]
    override fun toString() = Arrays.toString(values)
}

fun makeSimpleArray(vararg elements: APLValue) = APLArrayImpl(arrayOf(elements.size)) { elements[it] }

fun indexFromDimensions(d: Dimensions, p: IntArray): Int {
    val sizes = Array(d.size) { 0 }
    var curr = 1
    for (i in (d.size - 1) downTo 0) {
        sizes[i] = curr
        curr *= d[i]
    }

    var pos = 0
    for (i in p.indices) {
        val pi = p[i]
        val di = d[i]
        if (pi >= di) {
            throw APLIndexOutOfBoundsException("Index out of range")
        }
        pos += pi * sizes[i]
    }
    return pos
}
