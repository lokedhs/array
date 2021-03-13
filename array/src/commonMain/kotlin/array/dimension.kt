package array

import kotlin.jvm.JvmInline
import kotlin.native.concurrent.SharedImmutable

@JvmInline
value class Dimensions(val dimensions: IntArray) {
    val size: Int
        get() = dimensions.size

    val indices: IntRange
        get() = dimensions.indices

    operator fun get(i: Int) = dimensions[i]
    fun contentSize() = if (dimensions.isEmpty()) 1 else dimensions.reduce { a, b -> a * b }
    fun isEmpty() = dimensions.isEmpty()
    fun compareEquals(other: Dimensions) = Arrays.equals(dimensions, other.dimensions)

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
        if (toRemove < 0 || toRemove >= dimensions.size) {
            throw IndexOutOfBoundsException("Index does not fit in array. index=${toRemove}, size=${dimensions.size}")
        }
        val v = IntArray(dimensions.size - 1) { index ->
            if (index < toRemove) dimensions[index] else dimensions[index + 1]
        }
        return Dimensions(v)
    }

    fun indexFromPosition(p: IntArray, multipliers: IntArray? = null, pos: Position? = null): Int {
        if (p.size != dimensions.size) {
            throwAPLException(InvalidDimensionsException("Dimensions does not match", pos))
        }
        val sizes = multipliers ?: multipliers()
        var curr = 0
        for (i in p.indices) {
            val pi = p[i]
            val di = dimensions[i]
            if (pi < 0 || pi >= di) {
                throwAPLException(APLIndexOutOfBoundsException("Index out of range: pi=$pi, di=$di", pos))
            }
            curr += pi * sizes[i]
        }
        return curr
    }

    fun multipliers(): IntArray {
        var curr = 1
        val a = IntArray(dimensions.size) { 0 }
        for (i in dimensions.size - 1 downTo 0) {
            a[i] = curr
            curr *= dimensions[i]
        }
        return a
    }

    fun positionFromIndex(p: Int): IntArray {
        return positionFromIndexWithMultipliers(p, multipliers())
    }

    fun lastDimension(pos: Position? = null): Int {
        return if (dimensions.isEmpty()) {
            throwAPLException(InvalidDimensionsException("Can't take dimension from scalar", pos))
        } else {
            dimensions[dimensions.size - 1]
        }
    }

    fun lastAxis(pos: Position? = null): Int {
        if (dimensions.isEmpty()) {
            throwAPLException(InvalidDimensionsException("No axis available", pos))
        } else {
            return dimensions.size - 1
        }
    }

    override fun toString(): String {
        val buf = StringBuilder()
        buf.append("Dimensions[")
        for (i in dimensions.indices) {
            if (i > 0) {
                buf.append(", ")
            }
            buf.append(dimensions[i])
        }
        buf.append("]")
        return buf.toString()
    }

    companion object {
        fun positionFromIndexWithMultipliers(p: Int, multipliers: IntArray): IntArray {
            var curr = p
            return IntArray(multipliers.size) { i ->
                val multiplier = multipliers[i]
                val result = curr / multiplier
                curr %= multiplier
                result
            }
        }
    }
}

@SharedImmutable
private val EMPTY_DIMENSIONS = Dimensions(intArrayOf())

fun emptyDimensions() = EMPTY_DIMENSIONS
fun dimensionsOfSize(vararg values: Int) = Dimensions(values)
