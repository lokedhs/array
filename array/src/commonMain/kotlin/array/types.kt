package array

import array.rendertext.encloseInBox

typealias Dimensions = IntArray

interface APLValue {
    fun dimensions(): Dimensions
    fun rank(): Int = dimensions().size
    fun valueAt(p: Int): APLValue
    fun size(): Int = if (rank() == 0) 1 else dimensions().reduce { a, b -> a * b }
    fun formatted(): String = "unprintable"
    fun collapse(): APLValue
    fun toAPLExpression(): String = "not implemented"
    fun ensureNumber(): APLNumber = throw IncompatibleTypeException("Value ${formatted()} is not a numeric value")
    fun isScalar(): Boolean = rank() == 0
    fun defaultValue(): APLValue = APLLong(0)
    fun isAtom() = false
    fun arrayify(): APLValue
}

abstract class APLSingleValue : APLValue {
    override fun dimensions() = intArrayOf()
    override fun valueAt(p: Int) = throw APLIndexOutOfBoundsException("Reading index $p from scalar")
    override fun size() = 1
    override fun rank() = 0
    override fun collapse() = this
    override fun isAtom() = true
    override fun arrayify() = APLArrayImpl(intArrayOf(1)) { i -> this }
}

abstract class APLArray : APLValue {
    override fun collapse(): APLValue {
        return if (rank() == 0) {
            EnclosedAPLValue(valueAt(0).collapse())
        } else {
            APLArrayImpl(dimensions()) { valueAt(it).collapse() }
        }
    }

    override fun formatted() = arrayAsString(this)
    override fun arrayify() = if (rank() == 0) APLArrayImpl(intArrayOf(1)) { valueAt(0) } else this
}

fun arrayAsString(array: APLValue): String {
    return encloseInBox(array)
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
    override fun dimensions(): Dimensions = intArrayOf()

    override fun valueAt(p: Int): APLValue {
        if (p != 0) {
            throw APLIndexOutOfBoundsException("Attempt to read from a non-zero index ")
        }
        return value
    }
}

class APLChar(private val value: Int) : APLSingleValue() {
    fun codepoint(): Int = value
    fun asString(): String = charToString(value)
}

class APLSymbol(val value: Symbol) : APLSingleValue()

fun indexFromDimensions(d: Dimensions, p: Array<Int>): Int {
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

fun dimensionsToMultipliers(dimensions: Dimensions): IntArray {
    var curr = 1
    val a = IntArray(dimensions.size) { 0 }
    for (i in dimensions.size - 1 downTo 0) {
        a[i] = curr
        curr *= dimensions[i]
    }
    return a
}
