package array

import array.rendertext.encloseInBox
import array.rendertext.renderNullValue
import array.rendertext.renderStringValue

inline class Dimensions(val dimensions: IntArray) {
    val size: Int
        get() = dimensions.size

    val indices: IntRange
        get() = dimensions.indices

    operator fun get(i: Int) = dimensions[i]

    fun contentSize() = if (dimensions.isEmpty()) 1 else dimensions.reduce { a, b -> a * b }

    fun isEmpty() = dimensions.isEmpty()

    fun compare(other: Dimensions) = Arrays.equals(dimensions, other.dimensions)

    fun insert(pos: Int, newValue: Int): Dimensions {
        val v = IntArray(dimensions.size + 1) { index ->
            when {
                index < pos -> dimensions[index]
                index > pos -> dimensions[index - 1]
                else -> newValue
            }
        }
        return Dimensions(v)
    }

    fun remove(toRemove: Int): Dimensions {
        checkIndexRange(dimensions, toRemove)
        val v = IntArray(dimensions.size - 1) { index ->
            if (index < toRemove) dimensions[index] else dimensions[index + 1]
        }
        return Dimensions(v)
    }
}

private val EMPTY_DIMENSIONS = Dimensions(intArrayOf())

fun emptyDimensions() = EMPTY_DIMENSIONS
fun dimensionsOfSize(vararg values: Int) = Dimensions(values)

interface APLValue {
    fun dimensions(): Dimensions
    fun rank(): Int = dimensions().size
    fun valueAt(p: Int): APLValue
    fun size(): Int = dimensions().contentSize()
    fun formatted(): String = "unprintable"
    fun collapse(): APLValue
    fun toAPLExpression(): String = "not implemented"
    fun isScalar(): Boolean = rank() == 0
    fun defaultValue(): APLValue = APLLong(0)
    fun arrayify(): APLValue
    fun unwrapDeferredValue(): APLValue = this

    fun ensureNumber(): APLNumber {
        val v = unwrapDeferredValue()
        if (v == this) {
            throw IncompatibleTypeException("Value ${formatted()} is not a numeric value (type=${this::class.qualifiedName})")
        } else {
            return v.ensureNumber()
        }
    }
}

abstract class APLSingleValue : APLValue {
    override fun dimensions() = emptyDimensions()
    override fun valueAt(p: Int) = throw APLIndexOutOfBoundsException("Reading index $p from scalar")
    override fun size() = 1
    override fun rank() = 0
    override fun collapse() = this
    override fun arrayify() = APLArrayImpl(dimensionsOfSize(1)) { this }
}

abstract class APLArray : APLValue {
    override fun collapse(): APLValue {
        val v = unwrapDeferredValue()
        return when {
            v is APLSingleValue -> v
            v.rank() == 0 -> EnclosedAPLValue(v.valueAt(0).collapse())
            else -> APLArrayImpl(dimensions()) { v.valueAt(it).collapse() }
        }
    }

    override fun formatted() = arrayAsString(this)
    override fun arrayify() = if (rank() == 0) APLArrayImpl(dimensionsOfSize(1)) { valueAt(0) } else this
}

fun isNullValue(value: APLValue): Boolean {
    val dimensions = value.dimensions()
    return dimensions.size == 1 && dimensions[0] == 0
}

fun isStringValue(value: APLValue): Boolean {
    val dimensions = value.dimensions()
    if (dimensions.size == 1) {
        for (i in 0 until value.size()) {
            val v = value.valueAt(i)
            if (!(v is APLChar)) {
                return false
            }
        }
        return true
    } else {
        return false
    }
}

fun arrayAsString(array: APLValue): String {
    val v = array.collapse() // This is to prevent multiple evaluations during printing
    return when {
        isNullValue(v) -> renderNullValue()
        isStringValue(v) -> renderStringValue(v)
        else -> encloseInBox(v)
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
        values = Array(dimensions.contentSize(), init)
    }

    override fun dimensions() = dimensions
    override fun valueAt(p: Int) = values[p]
    override fun toString() = Arrays.toString(values)
}

fun makeFromInts(d: Dimensions, vararg values: Long): APLArray {
    return APLArrayImpl(d) { index ->
        APLLong(values[index])
    }
}

class EnclosedAPLValue(val value: APLValue) : APLArray() {
    override fun dimensions(): Dimensions = emptyDimensions()

    override fun valueAt(p: Int): APLValue {
        if (p != 0) {
            throw APLIndexOutOfBoundsException("Attempt to read from a non-zero index ")
        }
        return value
    }
}

class APLChar(val value: Int) : APLSingleValue() {
    override fun formatted() = "@${charToString(value)}"
    fun asString() = charToString(value)
}

fun makeAPLString(s: String): APLValue {
    val codepointList = s.asCodepointList()
    return APLArrayImpl(dimensionsOfSize(codepointList.size)) { i -> APLChar(codepointList[i]) }
}

class APLNullValue : APLArray() {
    override fun dimensions() = emptyDimensions()
    override fun valueAt(p: Int) = throw APLIndexOutOfBoundsException("Attempt to read a value from the null value")
}

abstract class DeferredResultArray : APLArray() {
    override fun unwrapDeferredValue(): APLValue {
        return if (dimensions().isEmpty()) valueAt(0).unwrapDeferredValue() else this
    }
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
