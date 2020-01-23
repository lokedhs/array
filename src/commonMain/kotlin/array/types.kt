package array

typealias Dimensions = Array<Int>

interface APLValue {
    fun add(a: APLValue): APLValue
    fun formatted(): String = this.toString()
}

val ADD_FN = { a: APLValue, b: APLValue -> a.add(b) }

abstract class APLNumber : APLValue {
    abstract fun asNumber(): Number
    override fun formatted() = asNumber().toString()
    override fun toString() = "APLLong(${formatted()})"
}

class APLLong(val value: Long) : APLNumber() {
    override fun asNumber() = value

    override fun add(a: APLValue): APLValue {
        return when (a) {
            is APLLong -> APLLong(value + a.value)
            is APLDouble -> a.add(this)
            is APLArray -> a.add(this)
            else -> throw IncompatibleTypeException("Attempt to add an integer to a non-number")
        }
    }
}

class APLDouble(val value: Double) : APLNumber() {
    override fun asNumber() = value

    override fun add(a: APLValue): APLValue {
        return when (a) {
            is APLDouble -> APLDouble(value + a.value)
            is APLLong -> APLDouble(value + a.value)
            else -> throw IncompatibleTypeException("Attempt to add an integer to a non-number")
        }
    }
}

abstract class APLArray : APLValue {
    abstract val dimensions: Dimensions
    abstract fun valueAt(vararg p: Int): APLValue

    fun rank() = this.dimensions.size

    override fun add(a: APLValue): APLValue {
        return when (a) {
            is APLArray -> {
                unless(rank() == a.rank()) {
                    throw IncompatibleTypeException("Arrays are of different dimensions")
                }
                ArraySum(this, a, ADD_FN)
            }
            is APLNumber -> {
                ArraySum(this, ConstantArray(this.dimensions, a), ADD_FN)
            }
            else -> throw IncompatibleTypeException("Can't add an array to an object of different type")
        }
    }

    override fun formatted(): String {
        val buf = StringBuilder()

        fun outputLine(start: Int, end: Int) {
            var first = true
            for (i in start until end) {
                val value = valueAt(i)
                if (first) first = false else buf.append(" ")
                buf.append(value.formatted())
            }
        }

        fun output2D() {
            val height = dimensions[0]
            val width = dimensions[1]
            var first = true
            for (y in 0 until height) {
                if(first) first = false else buf.append("\n")
                outputLine(y * width, (y + 1) * width)
            }
        }

        when {
            rank() == 0 -> buf.append(valueAt(0).formatted())
            rank() == 1 -> outputLine(0, dimensions[0])
            rank() == 2 -> output2D()
        }

        return buf.toString()
    }
}

class ConstantArray(
    override val dimensions: Dimensions,
    private val value: APLValue
) : APLArray() {

    override fun add(a: APLValue): APLValue {
        TODO("not implemented")
    }

    override fun valueAt(vararg p: Int) = value
}

class APLArrayImpl(
    override val dimensions: Dimensions,
    init: (Int) -> APLValue
) : APLArray() {

    private val values: Array<APLValue>

    init {
        values = Array(dimensions.reduce { a, b -> a * b }, init)
    }

    override fun valueAt(vararg p: Int): APLValue {
        unless(p.size == rank()) {
            throw IncompatibleTypeException("Incorrect rank. Got ${p.size}, expected ${rank()}")
        }
        val pos = indexFromDimensions(dimensions, p)
        return values[pos]
    }

    override fun toString() = Arrays.toString(values)
}

class ArraySum(
    private val a: APLArray,
    private val b: APLArray,
    private val fn: (APLValue, APLValue) -> APLValue
) : APLArray() {

    override val dimensions = a.dimensions // Both arrays are of the same dimension

    override fun valueAt(vararg p: Int): APLValue {
        val o1 = a.valueAt(*p)
        val o2 = b.valueAt(*p)
        return fn(o1, o2)
    }
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
