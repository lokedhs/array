package array

import array.builtins.compareAPLArrays
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
    LIST("list"),
    MAP("map")
}

enum class FormatStyle {
    PLAIN,
    READABLE,
    PRETTY
}

interface APLValue {
    val aplValueType: APLValueType

    val dimensions: Dimensions
    val rank: Int
        get() = dimensions.size

    fun valueAt(p: Int): APLValue
    val size: Int
        get() = dimensions.contentSize()

    fun formatted(style: FormatStyle = FormatStyle.PRETTY): String
    fun collapse(): APLValue
    fun isScalar(): Boolean = rank == 0
    fun defaultValue(): APLValue = APLLONG_0
    fun arrayify(): APLValue
    fun unwrapDeferredValue(): APLValue = this
    fun compareEquals(reference: APLValue): Boolean
    fun compare(reference: APLValue, pos: Position? = null): Int =
        throw IncompatibleTypeException("Comparison not implemented for objects of type ${this.aplValueType.typeName}")

    /**
     * Return a value which can be used as a hash key when storing references to this object in Kotlin maps.
     * The key must follow the standard equals/hashCode conventions with respect to the object which it
     * represents.
     *
     * In other words, if two instances of [APLValue] are to be considered equivalent, then the objects returned
     * by this method should be the same when compared using [equals] and return the same value from [hashCode].
     */
    fun makeKey(): Any

    fun singleValueOrError(): APLValue {
        return when {
            rank == 0 -> this
            size == 1 -> valueAt(0)
            else -> throw IllegalStateException("Expected a single element in array, found ${size} elements")
        }
    }

    fun ensureNumber(pos: Position? = null): APLNumber {
        val v = unwrapDeferredValue()
        if (v == this) {
            throw IncompatibleTypeException("Value $this is not a numeric value (type=${aplValueType.typeName})", pos)
        } else {
            return v.ensureNumber(pos)
        }
    }

    fun ensureSymbol(pos: Position? = null): APLSymbol {
        val v = unwrapDeferredValue()
        if (v == this) {
            throw IncompatibleTypeException("Value $this is not a symbol (type=${aplValueType.typeName})", pos)
        } else {
            return v.ensureSymbol(pos)
        }
    }

    fun ensureList(pos: Position? = null): APLList {
        val v = unwrapDeferredValue()
        if (v == this) {
            throw IncompatibleTypeException("Value $this is not a list (type=${aplValueType.typeName})", pos)
        } else {
            return v.ensureList(pos)
        }
    }

    fun toIntArray(pos: Position): IntArray {
        return IntArray(size) { i ->
            valueAt(i).ensureNumber(pos).asInt()
        }
    }
}

inline fun APLValue.iterateMembers(fn: (APLValue) -> Unit) {
    if (rank == 0) {
        fn(this)
    } else {
        for (i in 0 until size) {
            fn(valueAt(i))
        }
    }
}

fun APLValue.membersSequence(): Sequence<APLValue> {
    val v = unwrapDeferredValue()
    return if (v is APLSingleValue) {
        sequenceOf(v)
    } else {
        Sequence {
            val length = v.size
            var index = 0
            object : Iterator<APLValue> {
                override fun hasNext() = index < length
                override fun next() = v.valueAt(index++)
            }
        }
    }
}

abstract class APLSingleValue : APLValue {
    override val dimensions get() = emptyDimensions()
    override fun valueAt(p: Int) = throw APLIndexOutOfBoundsException("Reading index $p from scalar")
    override val size get() = 1
    override val rank get() = 0
    override fun collapse() = this
    override fun arrayify() = APLArrayImpl.make(dimensionsOfSize(1)) { this }
}

abstract class APLArray : APLValue {
    override val aplValueType: APLValueType get() = APLValueType.ARRAY

    override fun collapse(): APLValue {
        val v = unwrapDeferredValue()
        return when {
            v is APLSingleValue -> v
            v.rank == 0 -> EnclosedAPLValue(v.valueAt(0).collapse())
            else -> APLArrayImpl.make(v.dimensions) { v.valueAt(it).collapse() }
        }
    }

    override fun formatted(style: FormatStyle) =
        when (style) {
            FormatStyle.PLAIN -> arrayAsString(this, FormatStyle.PLAIN)
            FormatStyle.PRETTY -> arrayAsString(this, FormatStyle.PRETTY)
            FormatStyle.READABLE -> arrayToAPLFormat(this)
        }

    override fun arrayify() = if (rank == 0) APLArrayImpl.make(dimensionsOfSize(1)) { valueAt(0) } else this

    override fun compareEquals(reference: APLValue): Boolean {
        if (!dimensions.compareEquals(reference.dimensions)) {
            return false
        }
        for (i in 0 until size) {
            val o1 = valueAt(i)
            val o2 = reference.valueAt(i)
            if (!o1.compareEquals(o2)) {
                return false
            }
        }
        return true
    }

    override fun compare(reference: APLValue, pos: Position?): Int {
        return when {
            isScalar() && reference.isScalar() -> {
                return if (reference is APLSingleValue) {
                    -1
                } else {
                    valueAt(0).compare(reference.valueAt(0), pos)
                }
            }
            // Until we have a proper ordering of all types, we have to prevent comparing scalars to anything which is not a scalar
            isScalar() && !reference.isScalar() -> {
                throw IncompatibleTypeException("Comparison is not supported using these types", pos)
            }
            !isScalar() && reference.isScalar() -> {
                throw IncompatibleTypeException("Comparison is not supported using these types", pos)
            }
            else -> compareAPLArrays(this, reference)
        }
    }

    override fun makeKey() = object {
        override fun equals(other: Any?): Boolean {
            return other is APLArray && compare(other) == 0
        }

        override fun hashCode(): Int {
            var curr = 0
            dimensions.dimensions.forEach { dim ->
                curr = (curr * 63) xor dim
            }
            membersSequence().forEach { v ->
                curr = (curr * 63) xor v.makeKey().hashCode()
            }
            return curr
        }
    }
}

class APLMap(val content: ImmutableMap<Any, APLValue>) : APLSingleValue() {
    override val aplValueType get() = APLValueType.MAP
    override val dimensions = emptyDimensions()

    override fun formatted(style: FormatStyle): String {
        return "map[size=${content.size}]"
    }

    override fun compareEquals(reference: APLValue): Boolean {
        if (reference !is APLMap) {
            return false
        }
        if (content.size != reference.content.size) {
            return false
        }
        content.forEach { (key, value) ->
            val v = reference.content[key] ?: return false
            if (!value.compareEquals(v)) {
                return false
            }
        }
        return true
    }

    override fun makeKey(): Any {
        return content
    }

    fun lookupValue(key: APLValue): APLValue {
        return content[key.makeKey()] ?: APLNullValue()
    }

    fun updateValue(key: APLValue, value: APLValue): APLMap {
        return APLMap(content.copyAndPut(key.makeKey(), value))
    }

    companion object {
        fun makeEmptyMap() = APLMap(ImmutableMap())
    }
}

class APLList(val elements: List<APLValue>) : APLValue {
    override val aplValueType: APLValueType get() = APLValueType.LIST

    override val dimensions get() = emptyDimensions()

    override fun valueAt(p: Int): APLValue {
        TODO("not implemented")
    }

    override fun formatted(style: FormatStyle): String {
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

    override fun compareEquals(reference: APLValue): Boolean {
        if (reference !is APLList) {
            return false
        }
        if (elements.size != reference.elements.size) {
            return false
        }
        elements.indices.forEach { i ->
            if (!listElement(i).compareEquals(reference.listElement(i))) {
                return false
            }
        }
        return true
    }

    override fun makeKey(): Any {
        return ComparableList<Any>().apply { addAll(elements.map(APLValue::makeKey)) }
    }

    fun listSize() = elements.size
    fun listElement(index: Int) = elements[index]
}

class ComparableList<T> : MutableList<T> by ArrayList<T>() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ComparableList<*>) return false
        if (size != other.size) return false
        for (i in 0 until size) {
            if (this[i] != other[i]) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var curr = 0
        for (i in 0 until size) {
            curr = (curr * 63) xor this[i].hashCode()
        }
        return curr
    }
}

private fun arrayToAPLFormat(value: APLArray): String {
    val v = value.collapse()
    return if (isStringValue(v)) {
        renderStringValue(v, FormatStyle.READABLE)
    } else {
        arrayToAPLFormatStandard(v as APLArray)
    }
}

private fun arrayToAPLFormatStandard(value: APLArray): String {
    val buf = StringBuilder()
    val dimensions = value.dimensions
    if (dimensions.size == 0) {
        buf.append("⊂")
        buf.append(value.valueAt(0).formatted(FormatStyle.READABLE))
    } else {
        for (i in dimensions.indices) {
            if (i > 0) {
                buf.append(" ")
            }
            buf.append(dimensions[i])
        }
        buf.append("⍴")
        if (value.size == 0) {
            buf.append("1")
        } else {
            for (i in 0 until value.size) {
                val a = value.valueAt(i)
                if (i > 0) {
                    buf.append(" ")
                }
                buf.append(a.formatted(FormatStyle.READABLE))
            }
        }
    }
    return buf.toString()
}

fun isNullValue(value: APLValue): Boolean {
    val dimensions = value.dimensions
    return dimensions.size == 1 && dimensions[0] == 0
}

fun isStringValue(value: APLValue): Boolean {
    val dimensions = value.dimensions
    if (dimensions.size == 1) {
        for (i in 0 until value.size) {
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
    val dimensions = array.dimensions
    if (dimensions.size != 1) {
        throw IncompatibleTypeException("Argument is not a string", pos)
    }

    val buf = StringBuilder()
    for (i in 0 until array.size) {
        val v = array.valueAt(i)
        if (v !is APLChar) {
            throw IncompatibleTypeException("Argument is not a string", pos)
        }
        buf.append(v.asString())
    }

    return buf.toString()
}

fun arrayAsString(array: APLValue, style: FormatStyle): String {
    val v = array.collapse() // This is to prevent multiple evaluations during printing
    return when {
        isNullValue(v) -> renderNullValue()
        isStringValue(v) -> renderStringValue(v, style)
        else -> encloseInBox(v)
    }
}

class ConstantArray(
    override val dimensions: Dimensions,
    private val value: APLValue
) : APLArray() {

    override fun valueAt(p: Int) = value
}

class APLArrayImpl(
    override val dimensions: Dimensions,
    private val values: Array<APLValue>
) : APLArray() {

    override fun valueAt(p: Int) = values[p]
    override fun toString() = Arrays.toString(values)

    companion object {
        inline fun make(dimensions: Dimensions, fn: (index: Int) -> APLValue): APLArrayImpl {
            val content = Array(dimensions.contentSize()) { index -> fn(index) }
            return APLArrayImpl(dimensions, content)
        }
    }
}

class EnclosedAPLValue(val value: APLValue) : APLArray() {
    override val dimensions: Dimensions
        get() = emptyDimensions()

    override fun valueAt(p: Int): APLValue {
        if (p != 0) {
            throw APLIndexOutOfBoundsException("Attempt to read from a non-zero index ")
        }
        return value
    }
}

class APLChar(val value: Int) : APLSingleValue() {
    override val aplValueType: APLValueType get() = APLValueType.CHAR
    fun asString() = charToString(value)
    override fun formatted(style: FormatStyle) = when (style) {
        FormatStyle.PLAIN -> charToString(value)
        FormatStyle.PRETTY -> "@${charToString(value)}"
        FormatStyle.READABLE -> "@${charToString(value)}"
    }

    override fun compareEquals(reference: APLValue) = reference is APLChar && value == reference.value

    override fun compare(reference: APLValue, pos: Position?): Int {
        if (reference is APLChar) {
            return value.compareTo(reference.value)
        } else {
            throw IncompatibleTypeException("Chars must be compared to chars")
        }
    }

    override fun toString() = "APLChar['${asString()}' 0x${value.toString(16)}]"

    override fun makeKey() = value
}

fun makeAPLString(s: String) = APLString(s)

class APLString(val content: IntArray) : APLArray() {
    constructor(string: String) : this(string.asCodepointList().toIntArray())

    override val dimensions = dimensionsOfSize(content.size)
    override fun valueAt(p: Int) = APLChar(content[p])
}

private val NULL_DIMENSIONS = dimensionsOfSize(0)

class APLNullValue : APLArray() {
    override val dimensions get() = NULL_DIMENSIONS
    override fun valueAt(p: Int) = throw APLIndexOutOfBoundsException("Attempt to read a value from the null value")
}

abstract class DeferredResultArray : APLArray() {
    override fun unwrapDeferredValue(): APLValue {
        return if (dimensions.isEmpty()) valueAt(0).unwrapDeferredValue() else this
    }
}

class APLSymbol(val value: Symbol) : APLSingleValue() {
    override val aplValueType: APLValueType get() = APLValueType.SYMBOL
    override fun formatted(style: FormatStyle) =
        when (style) {
            FormatStyle.PLAIN -> value.namespace.name + ":" + value.symbolName
            FormatStyle.PRETTY -> value.namespace.name + ":" + value.symbolName
            FormatStyle.READABLE -> "'" + value.symbolName
        }

    override fun compareEquals(reference: APLValue) = reference is APLSymbol && value == reference.value

    override fun compare(reference: APLValue, pos: Position?): Int {
        if (reference is APLSymbol) {
            return value.compareTo(reference.value)
        } else {
            throw IncompatibleTypeException("Symbols can't be compared to values with type: ${reference.aplValueType.typeName}")
        }
    }

    override fun ensureSymbol(pos: Position?) = this

    override fun makeKey() = value
}

class LambdaValue(val fn: APLFunction) : APLSingleValue() {
    override val aplValueType: APLValueType get() = APLValueType.LAMBDA_FN
    override fun formatted(style: FormatStyle) =
        when (style) {
            FormatStyle.PLAIN -> "function"
            FormatStyle.READABLE -> throw IllegalArgumentException("Functions can't be printed in readable form")
            FormatStyle.PRETTY -> "function"
        }

    override fun compareEquals(reference: APLValue) = this === reference

    override fun makeKey() = fn
}
