package array

import array.rendertext.encloseInBox
import array.rendertext.renderNullValue
import array.rendertext.renderStringValue

enum class APLValueType(val typeName: String) {
    INTEGER("integer"),
    FLOAT("float"),
    COMPLEX("complex"),
    CHAR("char"),
    ARRAY("array"),
    SYMBOL("symbol")
}

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

    val aplValueType: APLValueType

    fun ensureNumber(): APLNumber {
        val v = unwrapDeferredValue()
        if (v == this) {
            throw IncompatibleTypeException("Value $this is not a numeric value (type=${this::class.qualifiedName})")
        } else {
            return v.ensureNumber()
        }
    }

    fun ensureSymbol(): APLSymbol {
        val v = unwrapDeferredValue()
        if (v == this) {
            throw IncompatibleTypeException("Value $this is not a symbol (type=${this::class.qualifiedName})")
        } else {
            return v.ensureSymbol()
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
    override val aplValueType: APLValueType = APLValueType.ARRAY

    override fun collapse(): APLValue {
        val v = unwrapDeferredValue()
        return when {
            v is APLSingleValue -> v
            v.rank() == 0 -> EnclosedAPLValue(v.valueAt(0).collapse())
            else -> APLArrayImpl(v.dimensions()) { v.valueAt(it).collapse() }
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
            if (v !is APLChar) {
                return false
            }
        }
        return true
    } else {
        return false
    }
}

fun arrayAsStringValue(array: APLValue): String {
    val dimensions = array.dimensions()
    if (dimensions.size != 1) {
        throw IncompatibleTypeException("Argument is not a string")
    }

    val buf = StringBuilder()
    for (i in 0 until array.size()) {
        val v = array.valueAt(i)
        if (v !is APLChar) {
            throw IncompatibleTypeException("Argument is not a string")
        }
        buf.append(v.asString())
    }

    return buf.toString()
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

    private val values = Array(dimensions.contentSize(), init)

    override fun dimensions() = dimensions
    override fun valueAt(p: Int) = values[p]
    override fun toString() = Arrays.toString(values)
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
    override val aplValueType: APLValueType = APLValueType.CHAR
    override fun formatted() = "@${charToString(value)}"
    fun asString() = charToString(value)
    override fun toString() = "APLChar['${asString()}' 0x${value.toString(16)}]"
}

fun makeAPLString(s: String): APLValue {
    val codepointList = s.asCodepointList()
    return APLArrayImpl(dimensionsOfSize(codepointList.size)) { i -> APLChar(codepointList[i]) }
}

private val NULL_DIMENSIONS = dimensionsOfSize(0)

class APLNullValue : APLArray() {
    override fun dimensions() = NULL_DIMENSIONS
    override fun valueAt(p: Int) = throw APLIndexOutOfBoundsException("Attempt to read a value from the null value")
}

abstract class DeferredResultArray : APLArray() {
    override fun unwrapDeferredValue(): APLValue {
        return if (dimensions().isEmpty()) valueAt(0).unwrapDeferredValue() else this
    }
}

class APLSymbol(val value: Symbol) : APLSingleValue() {
    override val aplValueType: APLValueType = APLValueType.SYMBOL
    override fun formatted() = "'" + value.symbolName
    override fun ensureSymbol() = this
}
