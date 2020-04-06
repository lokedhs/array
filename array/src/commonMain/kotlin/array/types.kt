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
    SYMBOL("symbol"),
    LAMBDA_FN("function"),
    LIST("list")
}

interface APLValue {
    enum class FormatStyle {
        PLAIN,
        READABLE,
        PRETTY
    }

    fun dimensions(): Dimensions
    fun rank(): Int = dimensions().size
    fun valueAt(p: Int): APLValue
    fun size(): Int = dimensions().contentSize()
    fun formatted(style: FormatStyle = FormatStyle.PRETTY): String
    fun collapse(): APLValue
    fun isScalar(): Boolean = rank() == 0
    fun defaultValue(): APLValue = APLLong(0)
    fun arrayify(): APLValue
    fun unwrapDeferredValue(): APLValue = this

    val aplValueType: APLValueType

    fun ensureNumber(pos: Position? = null): APLNumber {
        val v = unwrapDeferredValue()
        if (v == this) {
            throw IncompatibleTypeException("Value $this is not a numeric value (type=${this::class.qualifiedName})", pos)
        } else {
            return v.ensureNumber(pos)
        }
    }

    fun ensureSymbol(pos: Position? = null): APLSymbol {
        val v = unwrapDeferredValue()
        if (v == this) {
            throw IncompatibleTypeException("Value $this is not a symbol (type=${this::class.qualifiedName})", pos)
        } else {
            return v.ensureSymbol(pos)
        }
    }

    fun ensureList(pos: Position? = null): APLList {
        val v = unwrapDeferredValue()
        if (v == this) {
            throw IncompatibleTypeException("Value $this is not a list (type=${this::class.qualifiedName})", pos)
        } else {
            return v.ensureList(pos)
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

    override fun formatted(style: APLValue.FormatStyle) =
        when (style) {
            APLValue.FormatStyle.PLAIN -> arrayAsString(this, APLValue.FormatStyle.PLAIN)
            APLValue.FormatStyle.PRETTY -> arrayAsString(this, APLValue.FormatStyle.PRETTY)
            APLValue.FormatStyle.READABLE -> arrayToAPLFormat(this)
        }

    override fun arrayify() = if (rank() == 0) APLArrayImpl(dimensionsOfSize(1)) { valueAt(0) } else this
}

class APLList(val elements: List<APLValue>) : APLValue {
    override val aplValueType: APLValueType = APLValueType.LIST

    override fun dimensions() = emptyDimensions()

    override fun valueAt(p: Int): APLValue {
        TODO("not implemented")
    }

    override fun formatted(style: APLValue.FormatStyle): String {
        val buf = StringBuilder()
        var first = true
        for (v in elements) {
            if (first) {
                first = false
            } else {
                buf.append("\n; value\n")
            }
            buf.append(v.formatted())
        }
        return buf.toString()
    }

    override fun collapse() = this

    override fun arrayify(): APLValue {
        TODO("not implemented")
    }

    override fun ensureList(pos: Position?) = this

    fun listSize() = elements.size
    fun listElement(index: Int) = elements[index]
}

private fun arrayToAPLFormat(value: APLArray): String {
    val v = value.collapse()
    return if (isStringValue(v)) {
        renderStringValue(v, APLValue.FormatStyle.READABLE)
    } else {
        arrayToAPLFormatStandard(v as APLArray)
    }
}

private fun arrayToAPLFormatStandard(value: APLArray): String {
    val buf = StringBuilder()
    val dimensions = value.dimensions()
    if (dimensions.size == 0) {
        buf.append("⊂")
        buf.append(value.valueAt(0).formatted(APLValue.FormatStyle.READABLE))
    } else {
        for (i in dimensions.indices) {
            if (i > 0) {
                buf.append(" ")
            }
            buf.append(dimensions[i])
        }
        buf.append("⍴")
        if (value.size() == 0) {
            buf.append("1")
        } else {
            for (i in 0 until value.size()) {
                val a = value.valueAt(i)
                if (i > 0) {
                    buf.append(" ")
                }
                buf.append(a.formatted(APLValue.FormatStyle.READABLE))
            }
        }
    }
    return buf.toString()
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

fun arrayAsStringValue(array: APLValue, pos: Position? = null): String {
    val dimensions = array.dimensions()
    if (dimensions.size != 1) {
        throw IncompatibleTypeException("Argument is not a string", pos)
    }

    val buf = StringBuilder()
    for (i in 0 until array.size()) {
        val v = array.valueAt(i)
        if (v !is APLChar) {
            throw IncompatibleTypeException("Argument is not a string", pos)
        }
        buf.append(v.asString())
    }

    return buf.toString()
}

fun arrayAsString(array: APLValue, style: APLValue.FormatStyle): String {
    val v = array.collapse() // This is to prevent multiple evaluations during printing
    return when {
        isNullValue(v) -> renderNullValue()
        isStringValue(v) -> renderStringValue(v, style)
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
    fun asString() = charToString(value)
    override fun formatted(style: APLValue.FormatStyle) = when (style) {
        APLValue.FormatStyle.PLAIN -> charToString(value)
        APLValue.FormatStyle.PRETTY -> "@${charToString(value)}"
        APLValue.FormatStyle.READABLE -> "@${charToString(value)}"
    }

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
    override fun formatted(style: APLValue.FormatStyle) =
        when (style) {
            APLValue.FormatStyle.PLAIN -> value.symbolName
            APLValue.FormatStyle.PRETTY -> value.symbolName
            APLValue.FormatStyle.READABLE -> "'" + value.symbolName
        }

    override fun ensureSymbol(pos: Position?) = this
}

class LambdaValue(val fn: APLFunction) : APLSingleValue() {
    override val aplValueType: APLValueType = APLValueType.LAMBDA_FN
    override fun formatted(style: APLValue.FormatStyle) =
        when (style) {
            APLValue.FormatStyle.PLAIN -> "function"
            APLValue.FormatStyle.READABLE -> throw IllegalArgumentException("Functions can't be printed in readable form")
            APLValue.FormatStyle.PRETTY -> "function"
        }
}
