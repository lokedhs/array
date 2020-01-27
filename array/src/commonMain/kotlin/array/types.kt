package array

typealias Dimensions = Array<Int>

interface APLValue {
    fun dimensions(): Dimensions
    fun rank(): Int = dimensions().size
    fun valueAt(p: Int): APLValue
    fun size(): Int = dimensions().reduce { a, b -> a * b }
    fun asDouble(): Double = throw IncompatibleTypeException("Type cannot be converted to a number: ${this::class.qualifiedName}")

    fun formatted(): String = arrayAsString(this)
}

abstract class APLSingleValue : APLValue {
    override fun dimensions() = emptyArray<Int>()
    override fun valueAt(p: Int): APLValue {
        if(p != 0) {
            throw APLIndexOutOfBoundsException("Reading index $p from scalar")
        }
        else {
            return this
        }
    }
    override fun size() = 1
    override fun rank() = 0
}

abstract class APLArray : APLValue {
//    override fun add(a: APLValue): APLValue {
//        return when (a) {
//            is APLArray -> {
//                unless(rank() == a.rank()) {
//                    throw IncompatibleTypeException("Arrays are of different dimensions")
//                }
//                ArraySum(this, a, ADD_FN)
//            }
//            is APLNumber -> {
//                ArraySum(this, ConstantArray(this.dimensions(), a), ADD_FN)
//            }
//            else -> throw IncompatibleTypeException("Can't add an array to an object of different type")
//        }
//    }
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
        values = Array(dimensions.reduce { a, b -> a * b }, init)
    }

    override fun dimensions() = dimensions
    override fun valueAt(p: Int) = values[p]
    override fun toString() = Arrays.toString(values)
}

fun make_simple_array(vararg elements: APLValue) = APLArrayImpl(arrayOf(elements.size)) { elements.get(it) }

fun iota(n: Int) = APLArrayImpl(arrayOf(n)) { APLLong(it.toLong()) }

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
