package array

typealias Dimensions = Array<Int>

interface APLValue {
    fun dimensions(): Dimensions
    fun rank(): Int = dimensions().size
    fun valueAt(p: Int): APLValue
    fun size(): Int = if (rank() == 0) 1 else dimensions().reduce { a, b -> a * b }
    fun formatted(): String = "unprintable"
    fun collapse(): APLValue
    fun toAPLExpression(): String = "not implemented"
    fun ensureNumber(): APLNumber = throw IncompatibleTypeException("Value ${formatted()} is not a numeric value")
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
            EnclosedAPLValue(valueAt(0).collapse())
        } else {
            APLArrayImpl(dimensions()) { valueAt(it).collapse() }
        }
    }

    override fun formatted() = arrayAsString(this)
}

fun arrayAsString(array: APLValue): String {
    return when {
        array.rank() == 0 -> TODO("need implementation")//encloseInBox(array.valueAt(0).formatted())
        else -> TODO("Printing of arrays of dimensions >2 is not supported")
    }
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

class EnclosedAPLValue(val value: APLValue) : APLArray() {
    override fun dimensions(): Dimensions = emptyArray()

    override fun valueAt(p: Int): APLValue {
        if(p != 0) {
            throw APLIndexOutOfBoundsException("Attempt to read from a non-zero index ")
        }
        return value
    }
}

class APLChar(private val value: Int) : APLSingleValue() {
    fun codepoint(): Int = value
    fun asString(): String = charToString(value)
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
