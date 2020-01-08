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
}

class APLInteger(val value: Int) : APLNumber() {
    override fun asNumber() = value

    override fun add(a: APLValue): APLValue {
        return when (a) {
            is APLInteger -> APLInteger(value + a.value)
            is APLDouble -> a.add(this)
            else -> throw IncompatibleTypeException("Attempt to add an integer to a non-number")
        }
    }
}

class APLDouble(val value: Double) : APLNumber() {
    override fun asNumber() = value

    override fun add(a: APLValue): APLValue {
        return when (a) {
            is APLDouble -> APLDouble(value + a.value)
            is APLInteger -> APLDouble(value + a.value)
            else -> throw IncompatibleTypeException("Attempt to add an integer to a non-number")
        }
    }
}

interface APLArray : APLValue {
    fun dimensions(): Dimensions
    fun rank() = this.dimensions().size
    fun valueAt(vararg p: Int): APLValue
}

class ConstantArray(private val dimensions: Dimensions,
                    private val value: APLValue) : APLArray {

    override fun add(a: APLValue): APLValue {
        TODO("not implemented")
    }

    override fun dimensions() = dimensions
    override fun valueAt(vararg p: Int) = value
}

class APLArrayImpl(private val dimensions: Dimensions,
                   init: (Int) -> APLValue) : APLArray {

    private val values: Array<APLValue>

    init {
        values = Array(dimensions.reduce { a, b -> a * b }, init)
    }

    override fun dimensions() = dimensions

    override fun valueAt(vararg p: Int): APLValue {
        unless(p.size == rank()) {
            throw IncompatibleTypeException("Incorrect rank. Got ${p.size}, expected ${rank()}")
        }
        val pos = indexFromDimensions(dimensions(), p)
        return values[pos]
    }

    override fun add(a: APLValue): APLValue {
        return when (a) {
            is APLArray -> {
                unless(rank() == a.rank()) {
                    throw IncompatibleTypeException("Arrays are of different dimensions")
                }
                ArraySum(this, a, ADD_FN)
            }
            is APLNumber -> {
                ArraySum(this, ConstantArray(this.dimensions(), a), ADD_FN)
            }
            else -> throw IncompatibleTypeException("Can't add an array to an object of different type")
        }
    }

    override fun toString() = Arrays.toString(values)
}

class ArraySum(private val a: APLArray,
               private val b: APLArray,
               private val fn: (APLValue, APLValue) -> APLValue) : APLArray {

    override fun dimensions() = a.dimensions() // Both arrays are of the same dimension

    override fun valueAt(vararg p: Int): APLValue {
        val o1 = a.valueAt(*p)
        val o2 = b.valueAt(*p)
        return fn(o1, o2)
    }

    override fun add(a: APLValue): APLValue {
        TODO("not implemented")
    }
}

fun make_simple_array(vararg elements: APLValue) = APLArrayImpl(arrayOf(elements.size)) { elements.get(it) }

fun iota(n: Int) = APLArrayImpl(arrayOf(n)) { APLInteger(it) }

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
