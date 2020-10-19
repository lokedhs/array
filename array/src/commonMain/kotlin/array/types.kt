package array

import array.builtins.compareAPLArrays
import array.rendertext.encloseInBox
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
    MAP("map"),
    INTERNAL("internal")
}

enum class FormatStyle {
    PLAIN,
    READABLE,
    PRETTY
}

class AxisLabel(val title: String)

class DimensionLabels(val labels: List<List<AxisLabel?>?>) {
    companion object {
        fun makeEmpty(dimensions: Dimensions): DimensionLabels {
            val result = ArrayList<List<AxisLabel?>?>(dimensions.size)
            repeat(dimensions.size) {
                result.add(null)
            }
            return DimensionLabels(result)
        }

        fun makeDerived(dimensions: Dimensions, oldLabels: DimensionLabels?, newLabels: List<List<AxisLabel?>?>): DimensionLabels {
            assertx(newLabels.size == dimensions.size)
            val oldLabelsList = oldLabels?.labels
            val result = ArrayList<List<AxisLabel?>?>(dimensions.size)
            repeat(dimensions.size) { i ->
                val newLabelsList = newLabels[i]
                val v = when {
                    newLabelsList != null -> {
                        assertx(newLabelsList.size == dimensions[i])
                        newLabelsList
                    }
                    oldLabelsList != null -> oldLabelsList[i]
                    else -> null
                }
                result.add(v)
            }
            return DimensionLabels(result)
        }
    }
}

interface APLValue {
    val aplValueType: APLValueType

    val dimensions: Dimensions
    val rank: Int
        get() = dimensions.size

    fun valueAt(p: Int): APLValue
    fun valueAtWithScalarCheck(p: Int): APLValue = valueAt(p)
    val size: Int
        get() = dimensions.contentSize()

    fun formatted(style: FormatStyle = FormatStyle.PRETTY): String
    fun collapseInt(): APLValue
    fun isScalar(): Boolean = rank == 0
    fun defaultValue(): APLValue = APLLONG_0
    fun arrayify(): APLValue
    fun unwrapDeferredValue(): APLValue = this
    fun compareEquals(reference: APLValue): Boolean
    fun compare(reference: APLValue, pos: Position? = null): Int =
        throw IncompatibleTypeException("Comparison not implemented for objects of type ${this.aplValueType.typeName}", pos)

    fun disclose(): APLValue

    val labels: DimensionLabels? get() = null

    fun collapse(): APLValue {
        val l = labels
        val v = collapseInt()
        return if (l == null) {
            v
        } else if (v === this) {
            this
        } else {
            LabelledArray(v, l)
        }
    }

    /**
     * Return a value which can be used as a hash key when storing references to this object in Kotlin maps.
     * The key must follow the standard equals/hashCode conventions with respect to the object which it
     * represents.
     *
     * In other words, if two instances of [APLValue] are to be considered equivalent, then the objects returned
     * by this method should be the same when compared using [equals] and return the same value from [hashCode].
     */
    fun makeKey(): APLValueKey

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

    fun toDoubleArray(pos: Position): DoubleArray {
        return DoubleArray(size) { i ->
            valueAt(i).ensureNumber(pos).asDouble()
        }
    }

    fun asBoolean(): Boolean {
        val v = unwrapDeferredValue()
        return if (v == this) {
            true
        } else {
            v.asBoolean()
        }
    }

    abstract class APLValueKey(val value: APLValue) {
        override fun equals(other: Any?) = other is APLValueKey && value.compareEquals(other.value)
        override fun hashCode(): Int = throw RuntimeException("Need to implement hashCode")
    }

    class APLValueKeyImpl(value: APLValue, val data: Any) : APLValueKey(value) {
        override fun equals(other: Any?) = other is APLValueKeyImpl && data == other.data
        override fun hashCode() = data.hashCode()
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

inline fun APLValue.iterateMembersWithPosition(fn: (APLValue, Int) -> Unit) {
    if (rank == 0) {
        fn(this, 0)
    } else {
        for (i in 0 until size) {
            fn(valueAt(i), i)
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
    override fun valueAt(p: Int) = throw APLIndexOutOfBoundsException("Reading index ${p} from scalar")
    override fun valueAtWithScalarCheck(p: Int) =
        if (p == 0) this else throw APLIndexOutOfBoundsException("Reading at non-zero index ${p} from scalar")

    override val size get() = 1
    override val rank get() = 0
    override fun collapseInt() = this
    override fun arrayify() = APLArrayImpl.make(dimensionsOfSize(1)) { this }
    override fun disclose() = this
}

abstract class APLArray : APLValue {
    override val aplValueType: APLValueType get() = APLValueType.ARRAY

    override fun collapseInt(): APLValue {
        val v = unwrapDeferredValue()
        return when {
            v is APLSingleValue -> v
            v.rank == 0 -> EnclosedAPLValue(v.valueAt(0).collapseInt())
            else -> APLArrayImpl.make(v.dimensions) { i -> v.valueAt(i).collapseInt() }
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
        val u = this.unwrapDeferredValue()
        if (u is APLSingleValue) {
            return u.compareEquals(reference)
        } else {
            val uRef = reference.unwrapDeferredValue()
            if (!u.dimensions.compareEquals(uRef.dimensions)) {
                return false
            }
            for (i in 0 until u.size) {
                val o1 = u.valueAt(i)
                val o2 = uRef.valueAt(i)
                if (!o1.compareEquals(o2)) {
                    return false
                }
            }
            return true
        }
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

    override fun disclose() = if (dimensions.size == 0) valueAt(0) else this

    override fun makeKey() = object : APLValue.APLValueKey(this) {
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

class LabelledArray(val value: APLValue, override val labels: DimensionLabels) : APLArray() {
    override val dimensions = value.dimensions
    override fun valueAt(p: Int) = value.valueAt(p)

    override fun collapseInt(): APLValue {
        return value.collapseInt()
    }

    companion object {
        fun make(value: APLValue, extraLabels: List<List<AxisLabel?>?>): LabelledArray {
            return LabelledArray(value, DimensionLabels.makeDerived(value.dimensions, value.labels, extraLabels))
        }
    }
}

class APLMap(val content: ImmutableMap2<Any, APLValue>) : APLSingleValue() {
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

    override fun makeKey(): APLValue.APLValueKey {
        return APLValue.APLValueKeyImpl(this, content)
    }

    fun lookupValue(key: APLValue): APLValue {
        return content[key.makeKey()] ?: APLNullValue()
    }

    fun updateValue(key: APLValue, value: APLValue): APLMap {
        return APLMap(content.copyAndPut(key.makeKey(), value))
    }

    fun updateValues(elements: List<Pair<APLValue, APLValue>>): APLValue {
        return APLMap(content.copyAndPutMultiple(*elements.map { v -> Pair(v.first.makeKey(), v.second) }.toTypedArray()))
    }

    fun elementCount(): Int {
        return content.size
    }

    fun removeValues(toRemove: ArrayList<APLValue>): APLMap {
        return APLMap(content.copyWithoutMultiple(toRemove.map { v -> v.makeKey() }.toTypedArray()))
    }

    companion object {
        fun makeEmptyMap() = APLMap(ImmutableMap2())
    }
}

class APLList(val elements: List<APLValue>) : APLSingleValue() {
    override val aplValueType: APLValueType get() = APLValueType.LIST

    override val dimensions get() = emptyDimensions()

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

    override fun collapseInt() = this

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

    override fun makeKey(): APLValue.APLValueKey {
        return APLValue.APLValueKeyImpl(this, ComparableList<Any>().apply { addAll(elements.map(APLValue::makeKey)) })
    }

    fun listSize() = elements.size
    fun listElement(index: Int) = elements[index]
}

fun APLValue.positionalArgument(n: Int): APLValue? {
    return if (this is APLList) {
        if (n >= 0 && n < listSize()) {
            listElement(n)
        } else {
            null
        }
    } else if (n == 0) {
        this
    } else {
        null
    }
}

fun APLValue.primaryPositionalArgument(): APLValue {
    return if (this is APLList) {
        listElement(0)
    } else {
        this
    }
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
    return encloseInBox(v, style)
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
            throw IncompatibleTypeException("Chars must be compared to chars", pos)
        }
    }

    override fun toString() = "APLChar['${asString()}' 0x${value.toString(16)}]"

    override fun makeKey() = APLValue.APLValueKeyImpl(this, value)
}

fun makeAPLString(s: String) = APLString(s)

class APLString(val content: IntArray) : APLArray() {
    constructor(string: String) : this(string.asCodepointList().toIntArray())

    override val dimensions = dimensionsOfSize(content.size)
    override fun valueAt(p: Int) = APLChar(content[p])

    override fun collapseInt() = this
}

private val NULL_DIMENSIONS = dimensionsOfSize(0)

class APLNullValue : APLArray() {
    override val dimensions get() = NULL_DIMENSIONS
    override fun valueAt(p: Int) = throw APLIndexOutOfBoundsException("Attempt to read a value from the null value")
}

/**
 * Special version of of the regular null value that is emitted by a blank expression.
 * This value acts like a regular null value in most cases. However, in certain contexts
 * it has different behaviour. The main case is for array indexing.
 */
class APLEmpty : APLArray() {
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
            FormatStyle.PLAIN -> "${value.namespace.name}:${value.symbolName}"
            FormatStyle.PRETTY -> "${value.namespace.name}:${value.symbolName}"
            FormatStyle.READABLE -> "'${value.namespace.name}:${value.symbolName}"
        }

    override fun compareEquals(reference: APLValue) = reference is APLSymbol && value == reference.value

    override fun compare(reference: APLValue, pos: Position?): Int {
        if (reference is APLSymbol) {
            return value.compareTo(reference.value)
        } else {
            throw IncompatibleTypeException("Symbols can't be compared to values with type: ${reference.aplValueType.typeName}", pos)
        }
    }

    override fun ensureSymbol(pos: Position?) = this

    override fun makeKey() = APLValue.APLValueKeyImpl(this, value)
}

/**
 * This class represents a closure. It wraps a function and a context to use when calling the closure.
 *
 * @param fn the function that is wrapped by the closure
 * @param previousContext the context to use when calling the function
 */
class LambdaValue(private val fn: APLFunction, private val previousContext: RuntimeContext) : APLSingleValue() {
    override val aplValueType: APLValueType get() = APLValueType.LAMBDA_FN
    override fun formatted(style: FormatStyle) =
        when (style) {
            FormatStyle.PLAIN -> "function"
            FormatStyle.READABLE -> throw IllegalArgumentException("Functions can't be printed in readable form")
            FormatStyle.PRETTY -> "function"
        }

    override fun compareEquals(reference: APLValue) = this === reference

    override fun makeKey() = APLValue.APLValueKeyImpl(this, fn)

    fun makeClosure(): APLFunction {
        return object : APLFunction(fn.pos) {
            override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
                return fn.eval1Arg(previousContext, a, axis)
            }

            override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
                return fn.eval2Arg(previousContext, a, b, axis)
            }

            override fun identityValue() = fn.identityValue()
        }
    }
}

class IntArrayValue private constructor(
    srcDimensions: Dimensions,
    val values: IntArray
) : APLArray() {

    constructor(srcDimensions: IntArray, initFn: (Int) -> Int) :
            this(Dimensions(srcDimensions), IntArray(srcDimensions.reduce { a, b -> a * b }, initFn))

    override val dimensions = srcDimensions

    override fun valueAt(p: Int) = values[p].makeAPLNumber()

    fun intValueAt(p: Int) = values[p]

    companion object {
        fun fromAPLValue(src: APLValue, pos: Position? = null): IntArrayValue {
            return if (src is IntArrayValue) {
                src
            } else {
                val dimensions = src.dimensions
                val values = IntArray(dimensions.contentSize()) { i ->
                    src.valueAt(i).ensureNumber(pos).asInt()
                }
                IntArrayValue(dimensions, values)
            }
        }
    }
}
