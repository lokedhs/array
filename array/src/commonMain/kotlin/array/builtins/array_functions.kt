package array.builtins

import array.*
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.max

class AxisActionFactors(val dimensions: Dimensions, axis: Int) {
    val multipliers = dimensions.multipliers()
    val multiplierAxis = multipliers[axis]
    val highValFactor = multiplierAxis * dimensions[axis]

    inline fun <T> withFactors(p: Int, fn: (high: Int, low: Int, axisCoord: Int) -> T): T {
        val highVal = p / highValFactor
        val lowVal = p % multiplierAxis
        val axisCoord = (p % highValFactor) / multiplierAxis
        return fn(highVal, lowVal, axisCoord)
    }

    fun indexForAxis(high: Int, low: Int, axisPosition: Int): Int {
        return (highValFactor * high) + (axisPosition * multiplierAxis) + low
    }
}

object IotaArrayImpls {
    interface GenericIotaArrayLong {
        fun resizeIotaArray(d: Dimensions, updatedOffset: Long): ResizedIotaArrayLong
    }

    class IotaArray(val indexes: IntArray) : APLArray() {
        override val dimensions = Dimensions(indexes)

        private val multipliers = dimensions.multipliers()

        init {
            assertx(indexes.isNotEmpty())
        }

        override fun valueAt(p: Int): APLValue {
            val index = Dimensions.positionFromIndexWithMultipliers(p, multipliers)
            return APLArrayLong(dimensionsOfSize(indexes.size), LongArray(index.size) { i -> index[i].toLong() })
        }
    }

    class IotaArrayLong(val length: Int) : APLArray(), GenericIotaArrayLong {
        override val dimensions = dimensionsOfSize(length)
        override val specialisedType get() = ArrayMemberType.LONG
        override fun collapseInt() = this

        override fun valueAtInt(p: Int, pos: Position?): Int {
            if (p < 0 || p >= length) {
                throwAPLException(APLIndexOutOfBoundsException("Position in array: ${p}, size: ${length}", pos))
            }
            return p
        }

        override fun valueAtLong(p: Int, pos: Position?): Long {
            return valueAtInt(p, pos).toLong()
        }

        override fun valueAt(p: Int): APLValue {
            return valueAtLong(p, null).makeAPLNumber()
        }

        override fun resizeIotaArray(d: Dimensions, updatedOffset: Long): ResizedIotaArrayLong {
            return ResizedIotaArrayLong(d, length, updatedOffset)
        }
    }

    class ResizedIotaArrayLong(
        override val dimensions: Dimensions,
        val width: Int,
        val offset: Long
    ) : APLArray(), GenericIotaArrayLong {
        val length = dimensions.contentSize()
        override val specialisedType get() = ArrayMemberType.LONG
        override fun collapseInt() = this

        override fun valueAtLong(p: Int, pos: Position?): Long {
            if (p < 0 || p >= length) {
                throwAPLException(APLIndexOutOfBoundsException("Position in array: ${p}, size: ${length}", pos))
            }
            return (p % width) + offset
        }

        override fun valueAt(p: Int): APLValue {
            return valueAtLong(p, null).makeAPLNumber()
        }

        override fun resizeIotaArray(d: Dimensions, updatedOffset: Long): ResizedIotaArrayLong {
            return ResizedIotaArrayLong(d, width, offset + updatedOffset)
        }
    }
}

class FindIndexArray(val a: APLValue, val b: APLValue, val context: RuntimeContext) : APLArray() {
    override val dimensions = b.dimensions

    override fun valueAt(p: Int): APLValue {
        val reference = b.valueAt(p).unwrapDeferredValue()
        return findFromRef(reference)
    }

    private fun findFromRef(reference: APLValue): APLLong {
        val elementCount = a.size
        for (i in 0 until elementCount) {
            val v = a.valueAt(i)
            if (v.compareEquals(reference)) {
                return i.makeAPLNumber()
            }
        }
        return elementCount.makeAPLNumber()
    }

    override fun unwrapDeferredValue(): APLValue {
        return if (dimensions.isEmpty()) {
            findFromRef(b)
        } else {
            this
        }
    }
}

class IotaAPLFunction : APLFunctionDescriptor {
    class IotaAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val aDimensions = a.dimensions
            return when (aDimensions.size) {
                0 -> IotaArrayImpls.IotaArrayLong(a.ensureNumber(pos).asInt())
                1 -> if (aDimensions[0] == 0) {
                    EnclosedAPLValue.make(APLNullValue.APL_NULL_INSTANCE)
                } else {
                    IotaArrayImpls.IotaArray(IntArray(aDimensions[0]) { i -> a.valueAtInt(i, pos) })
                }
                else -> throwAPLException(InvalidDimensionsException("Right argument must be rank 0 or 1", pos))
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            if (a.rank > 1) {
                throwAPLException(InvalidDimensionsException("Left argument must be rank 0 or 1", pos))
            }
            return FindIndexArray(a.arrayify(), b, context)
        }
    }

    override fun make(pos: Position) = IotaAPLFunctionImpl(pos)
}

object ResizedArrayImpls {
    class ResizedArray(override val dimensions: Dimensions, private val value: APLValue) : APLArray() {
        override val specialisedType = value.specialisedType
        override fun valueAt(p: Int) = value.valueAt(p % value.size)
        override fun valueAtInt(p: Int, pos: Position?) = value.valueAtInt(p % value.size, pos)
        override fun valueAtLong(p: Int, pos: Position?) = value.valueAtLong(p % value.size, pos)
        override fun valueAtDouble(p: Int, pos: Position?) = value.valueAtDouble(p % value.size, pos)
    }

    class ResizedSingleValueGeneric(override val dimensions: Dimensions, private val value: APLValue) : APLArray() {
        override val specialisedType get() = ArrayMemberType.GENERIC
        override fun valueAt(p: Int) = value
    }

    class ResizedArrayLong(override val dimensions: Dimensions, private val boxed: APLLong) : APLArray() {
        override val specialisedType get() = ArrayMemberType.LONG
        override fun valueAt(p: Int) = boxed
        override fun valueAtLong(p: Int, pos: Position?) = boxed.value
    }

    class ResizedArrayDouble(override val dimensions: Dimensions, private val boxed: APLDouble) : APLArray() {
        override val specialisedType get() = ArrayMemberType.DOUBLE
        override fun valueAt(p: Int) = boxed
        override fun valueAtDouble(p: Int, pos: Position?) = boxed.value
    }

    fun makeResizedArray(dimensions: Dimensions, value: APLValue): APLValue {
        assertx(dimensions.size >= 1)
        val v = value.unwrapDeferredValue()
        return when {
            dimensions.compareEquals(v.dimensions) -> v
            value is IotaArrayImpls.GenericIotaArrayLong -> value.resizeIotaArray(dimensions, 0)
            v is APLLong -> ResizedArrayLong(dimensions, v)
            v is APLDouble -> ResizedArrayDouble(dimensions, v)
            v is APLSingleValue -> ResizedSingleValueGeneric(dimensions, v)
            dimensions.size == 0 -> ResizedSingleValueGeneric(dimensions, v.disclose())
            else -> ResizedArray(dimensions, v)
        }
    }
}

class RhoAPLFunction : APLFunctionDescriptor {
    class RhoAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val argDimensions = a.dimensions
            return APLArrayImpl.make(dimensionsOfSize(argDimensions.size)) { argDimensions[it].makeAPLNumber() }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val d = a.dimensions

            if (d.size > 1) {
                throwAPLException(InvalidDimensionsException("Left side of rho must be scalar or a one-dimensional array", pos))
            }

            val v = a.unwrapDeferredValue()
            if (d.size == 1 && d[0] == 0) {
                return b.arrayify().valueAt(0)
            } else {
                val d1 = if (d.size == 0) {
                    dimensionsOfSize(v.ensureNumber(pos).asInt())
                } else {
                    val dimensionsArray = IntArray(v.size) { v.valueAtInt(it, pos) }
                    var calculatedIndex: Int? = null
                    dimensionsArray.forEachIndexed { i, sizeSpecValue ->
                        if (sizeSpecValue < 0) {
                            if (sizeSpecValue == -1) {
                                if (calculatedIndex != null) {
                                    throwAPLException(InvalidDimensionsException("Only one dimension may be set to -1", pos))
                                }
                                calculatedIndex = i
                            } else {
                                throwAPLException(
                                    InvalidDimensionsException(
                                        "Illegal value at index ${i} in dimensions: ${sizeSpecValue}",
                                        pos))
                            }
                        }
                    }
                    val updatedDimensionsArray = calculatedIndex.let { calcPos ->
                        if (calcPos == null) {
                            dimensionsArray
                        } else {
                            val bDimensions = b.dimensions
                            if (bDimensions.size == 0) {
                                throwAPLException(
                                    APLIllegalArgumentException(
                                        "Calculated dimensions can only be used with array arguments",
                                        pos))
                            }
                            val contentSize = bDimensions.contentSize()
                            val total = dimensionsArray.filter { it >= 0 }.reduceWithInitial(1) { o0, o1 -> o0 * o1 }
                            IntArray(v.size) { index ->
                                if (index == calcPos) {
                                    if (contentSize % total != 0) {
                                        throwAPLException(
                                            InvalidDimensionsException(
                                                "Invalid size of right argument: ${contentSize}. Should be divisible by ${total}.",
                                                pos))
                                    }
                                    contentSize / total
                                } else {
                                    dimensionsArray[index]
                                }
                            }
                        }
                    }
                    Dimensions(updatedDimensionsArray)
                }
                return ResizedArrayImpls.makeResizedArray(d1, b)
            }
        }
    }

    override fun make(pos: Position) = RhoAPLFunctionImpl(pos)
}

class IdentityAPLFunction : APLFunctionDescriptor {
    class IdentityAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue) = a
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue) = b
    }

    override fun make(pos: Position) = IdentityAPLFunctionImpl(pos)
}

class HideAPLFunction : APLFunctionDescriptor {
    class HideAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue) = a
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue) = a
    }

    override fun make(pos: Position) = HideAPLFunctionImpl(pos)
}

class Concatenated1DArrays(private val a: APLValue, private val b: APLValue) : APLArray() {
    init {
        assertx(a.rank == 1 && b.rank == 1)
    }

    private val aSize = a.dimensions[0]
    private val bSize = b.dimensions[0]
    override val dimensions = dimensionsOfSize(aSize + bSize)

    override val labels by lazy { resolveLabels() }

    override fun valueAt(p: Int): APLValue {
        return if (p >= aSize) b.valueAt(p - aSize) else a.valueAt(p)
    }

    override fun collapseInt(): APLValue {
        return if (a is APLString && b is APLString) {
            APLString(IntArray(dimensions[0]) { i ->
                if (i < aSize) {
                    a.content[i]
                } else {
                    b.content[i - aSize]
                }
            })
        } else {
            super.collapseInt()
        }
    }

    private fun resolveLabels(): DimensionLabels? {
        val aLabels = a.labels
        val bLabels = b.labels
        if (aLabels == null && bLabels == null) {
            return null
        }

        val newLabels = ArrayList<AxisLabel?>()
        fun addNulls(n: Int) {
            repeat(n) {
                newLabels.add(null)
            }
        }

        fun processArg(n: Int, labels: DimensionLabels?) {
            if (labels == null) {
                addNulls(n)
            } else {
                val labelsList = labels.labels[0]
                if (labelsList == null) {
                    addNulls(n)
                } else {
                    labelsList.forEach { l ->
                        newLabels.add(l)
                    }
                }
            }
        }

        processArg(aSize, aLabels)
        processArg(bSize, bLabels)

        val allLabels = ArrayList<List<AxisLabel?>?>()
        allLabels.add(newLabels)
        return DimensionLabels(allLabels)
    }
}

abstract class ConcatenateAPLFunctionImpl(pos: Position) : APLFunction(pos) {
    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        // The APL concept of using a non-integer axis to specify that you want to add a dimension (i.e. the laminate
        // function) is a bit confusing and this operation should really have a different syntax.
        //
        // The reason this method is confusing is because it relies on the concept of "near integer". This is an
        // unreliable concept that KAP tries to avoid. In this particular case, it's not too bad since the axis
        // is almost always specified explicitly (and usually .5). We choose the completely arbitrary value
        // of 0.01 for the check.
        //
        // Another problem with the APL syntax is that it chooses the axis as being the argument rounded up
        // to the nearest integer. That means that when the index offset is set to 0 (which it always is for KAP)
        // to extend the first dimension, the argument have to be -0.5. That's incredibly ugly.

        return if (axis == null) {
            joinNoAxis(a, b)
        } else {
            val (isLaminate, newAxis) = computeLaminateAxis(axis.ensureNumber(pos))
            if (isLaminate) {
                joinByLaminate(a, b, newAxis)
            } else {
                joinByAxis(a, b, newAxis)
            }
        }
    }

    private fun computeLaminateAxis(axis: APLNumber): Pair<Boolean, Int> {
        if (axis is APLLong) {
            return Pair(false, axis.asInt())
        }
        val d = axis.asDouble()
        if (d.rem(1).absoluteValue < 0.01) {
            return Pair(false, d.toInt())
        }
        return Pair(true, ceil(d).toInt())
    }

    private fun joinByLaminate(a: APLValue, b: APLValue, axis: Int): APLValue {
        val a1 = if (a.isScalar()) {
            ResizedArrayImpls.makeResizedArray(b.dimensions, a)
        } else {
            a
        }
        val b1 = if (b.isScalar()) {
            ResizedArrayImpls.makeResizedArray(a.dimensions, b)
        } else {
            b
        }
        if (a1.rank != b1.rank) {
            throwAPLException(InvalidDimensionsException("Ranks of A and B are different", pos))
        }
        val aDimensions = a1.dimensions
        if (axis < 0 || axis > aDimensions.size) {
            throwAPLException(
                IllegalAxisException(
                    "Axis must be between 0 and ${aDimensions.size} inclusive. Found: ${axis}",
                    pos))
        }
        val bDimensions = b1.dimensions
        if (!aDimensions.compareEquals(bDimensions)) {
            throwAPLException(InvalidDimensionsException(aDimensions, bDimensions, pos))
        }
        val rd = aDimensions.insert(axis, 1)
        val a2 = ResizedArrayImpls.makeResizedArray(rd, a1)
        val b2 = ResizedArrayImpls.makeResizedArray(rd, b1)
        return joinByAxis(a2, b2, axis)
    }

    private fun joinNoAxis(a: APLValue, b: APLValue): APLValue {
        return when {
            a.rank == 0 && b.rank == 0 -> APLArrayImpl.make(dimensionsOfSize(2)) { i -> if (i == 0) a.disclose() else b.disclose() }
            a.rank <= 1 && b.rank <= 1 -> Concatenated1DArrays(a.arrayify(), b.arrayify())
            else -> joinByAxis(a, b, defaultAxis(a, b))
        }
    }

    abstract fun defaultAxis(a: APLValue, b: APLValue): Int

    private fun joinByAxis(a: APLValue, b: APLValue, axis: Int): APLValue {
        if (a.rank == 0 && b.rank == 0) {
            throwAPLException(InvalidDimensionsException("Both a and b are scalar", pos))
        }

        val a1 = if (a.rank == 0) {
            val bDimensions = b.dimensions
            ConstantArray(
                Dimensions(IntArray(bDimensions.size) { index -> if (index == axis) 1 else bDimensions[index] }),
                a.disclose())
        } else {
            a
        }

        val b1 = if (b.rank == 0) {
            val aDimensions = a.dimensions
            ConstantArray(
                Dimensions(IntArray(aDimensions.size) { index -> if (index == axis) 1 else aDimensions[index] }),
                b.disclose())
        } else {
            b
        }

        val a2 = if (b1.rank - a1.rank == 1) {
            // Reshape a1, inserting a new dimension at the position of the axis
            ResizedArrayImpls.makeResizedArray(a1.dimensions.insert(axis, 1), a1)
        } else {
            a1
        }

        val b2 = if (a1.rank - b1.rank == 1) {
            ResizedArrayImpls.makeResizedArray(b1.dimensions.insert(axis, 1), b1)
        } else {
            b1
        }

        val da = a2.dimensions
        val db = b2.dimensions

        if (da.size != db.size) {
            throwAPLException(InvalidDimensionsException("Different ranks: ${da.size} compared to ${db.size}", pos))
        }

        for (i in da.indices) {
            if (i != axis && da[i] != db[i]) {
                throwAPLException(
                    InvalidDimensionsException(
                        "Dimensions at axis $axis does not match: $da compared to $db",
                        pos))
            }
        }

        if (a2.size == 0) {
            return b2
        }

        if (b2.size == 0) {
            return a2
        }

        if (da.size == 1 && db.size == 1) {
            return Concatenated1DArrays(a2, b2)
        }

        return ConcatenatedMultiDimensionalArrays(a2, b2, axis)
    }

    // This is an inner class since it's highly dependent on invariants that are established in the parent class
    class ConcatenatedMultiDimensionalArrays(val a: APLValue, val b: APLValue, val axis: Int) : APLArray() {
        override val dimensions: Dimensions
        private val axisA: Int
        private val multiplierAxis: Int
        private val highValFactor: Int
        private val aMultiplierAxis: Int
        private val bMultiplierAxis: Int
        private val aDimensionAxis: Int
        private val bDimensionAxis: Int

        override val labels by lazy { resolveLabels() }

        init {
            val ad = a.dimensions
            val bd = b.dimensions

            axisA = ad[axis]

            dimensions = Dimensions(IntArray(ad.size) { i -> if (i == axis) ad[i] + bd[i] else ad[i] })
            val multipliers = dimensions.multipliers()
            val aMultipliers = ad.multipliers()
            val bMultipliers = bd.multipliers()
            multiplierAxis = multipliers[axis]
            highValFactor = multiplierAxis * dimensions[axis]
            aMultiplierAxis = aMultipliers[axis]
            bMultiplierAxis = bMultipliers[axis]
            aDimensionAxis = a.dimensions[axis]
            bDimensionAxis = b.dimensions[axis]
        }

        override fun valueAt(p: Int): APLValue {
            val highVal = p / highValFactor
            val lowVal = p % multiplierAxis
            val axisCoord = (p % highValFactor) / multiplierAxis
            return if (axisCoord < axisA) {
                a.valueAt((highVal * aMultiplierAxis * aDimensionAxis) + (axisCoord * aMultiplierAxis) + lowVal)
            } else {
                b.valueAt((highVal * bMultiplierAxis * bDimensionAxis) + ((axisCoord - axisA) * bMultiplierAxis) + lowVal)
            }
        }

        private fun resolveLabels(): DimensionLabels? {
            val aLabels = a.labels
            val bLabels = b.labels
            if (aLabels == null && bLabels == null) {
                return null
            }
            val axisLabelsA = aLabels?.run { labels[axis] }
            val axisLabelsB = bLabels?.run { labels[axis] }
            val axisLabels = ArrayList<AxisLabel?>()
            val aSize = a.dimensions[axis]
            val bSize = b.dimensions[axis]
            var hasLabels = false
            val d = dimensions
            if (axisLabelsA == null) {
                repeat(aSize) {
                    axisLabels.add(null)
                }
            } else {
                axisLabelsA.forEach { label ->
                    axisLabels.add(label)
                    hasLabels = true
                }
            }
            if (axisLabelsB == null) {
                repeat(bSize) {
                    axisLabels.add(null)
                }
            } else {
                axisLabelsB.forEach { label ->
                    axisLabels.add(label)
                    hasLabels = true
                }
            }

            if (!hasLabels) {
                return null
            }

            val newLabels = ArrayList<List<AxisLabel?>?>()
            repeat(d.size) { i ->
                newLabels.add(if (i == axis) axisLabels else null)
            }
            return DimensionLabels(newLabels)
        }
    }
}

class ConcatenateAPLFunctionFirstAxis : APLFunctionDescriptor {
    class ConcatenateAPLFunctionFirstAxisImpl(pos: Position) : ConcatenateAPLFunctionImpl(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            val aDimensions = a.dimensions
            val c = if (aDimensions.size == 0) {
                dimensionsOfSize(1, 1)
            } else {
                dimensionsOfSize(
                    aDimensions[0],
                    aDimensions.dimensions.drop(1).reduceWithInitial(1) { v1, v2 -> v1 * v2 })
            }
            return ResizedArrayImpls.makeResizedArray(c, a)
        }

        override fun defaultAxis(a: APLValue, b: APLValue) = 0
    }

    override fun make(pos: Position) = ConcatenateAPLFunctionFirstAxisImpl(pos)
}

class ConcatenateAPLFunctionLastAxis : APLFunctionDescriptor {
    class ConcatenateAPLFunctionLastAxisImpl(pos: Position) : ConcatenateAPLFunctionImpl(pos) {
        private class DelegatedAPLArrayValue(override val dimensions: Dimensions, val value: APLValue) : APLArray() {
            override fun valueAt(p: Int): APLValue {
                return value.valueAt(0)
            }
        }

        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            return when {
                a is APLSingleValue -> APLArrayImpl.make(dimensionsOfSize(1)) { a }
                a.rank == 0 -> DelegatedAPLArrayValue(dimensionsOfSize(1), a)
                else -> ResizedArrayImpls.makeResizedArray(dimensionsOfSize(a.size), a)
            }
        }

        override fun defaultAxis(a: APLValue, b: APLValue) = max(a.rank, b.rank) - 1
    }

    override fun make(pos: Position) = ConcatenateAPLFunctionLastAxisImpl(pos)
}

class AccessFromIndexAPLFunction : APLFunctionDescriptor {
    class AccessFromIndexAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val aFixed = a.arrayify()
            val ad = aFixed.dimensions
            if (ad.size != 1) {
                throwAPLException(InvalidDimensionsException("Position argument is not rank 1", pos))
            }
            val bd = b.dimensions
            if (ad[0] != bd.size) {
                throwAPLException(
                    InvalidDimensionsException(
                        "Number of values in position argument must match the number of dimensions",
                        pos))
            }
            val posList = IntArray(ad[0]) { i -> aFixed.valueAtInt(i, pos) }
            val p = bd.indexFromPosition(posList)
            return b.valueAt(p)
        }
    }

    override fun make(pos: Position) = AccessFromIndexAPLFunctionImpl(pos)
}

class TakeArrayValue(val selection: IntArray, val source: APLValue, val pos: Position? = null) : APLArray() {
    override val dimensions = Dimensions(selection.map { v -> v.absoluteValue }.toIntArray())
    private val multipliers = dimensions.multipliers()
    private val sourceDimensions = source.dimensions
    private val sourceMultipliers = sourceDimensions.multipliers()

    private fun sourceOrDefaultWithPosition(p: IntArray): APLValue {
        var curr = 0
        for (i in p.indices) {
            val pi = p[i]
            val di = sourceDimensions[i]
            if (pi < 0 || pi >= di) {
                return source.defaultValue()
            }
            curr += pi * sourceMultipliers[i]
        }
        return source.valueAt(curr)
    }

    override fun valueAt(p: Int): APLValue {
        val coords = Dimensions.positionFromIndexWithMultipliers(p, multipliers)
        val adjusted = IntArray(coords.size) { i ->
            val d = selection[i]
            val v = coords[i]
            if (d >= 0) {
                v
            } else {
                sourceDimensions[i] + d + v
            }
        }
        return sourceOrDefaultWithPosition(adjusted)
    }
}

class TakeAPLFunction : APLFunctionDescriptor {
    class TakeAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val v = a.unwrapDeferredValue()
            return when {
                v is APLSingleValue -> v
                v.isScalar() -> v.valueAt(0)
                v.size == 0 -> v.defaultValue()
                else -> v.valueAt(0)
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val aDimensions = a.dimensions
            if (!((aDimensions.size == 0 && b.rank == 1) || (aDimensions.size == 1 && aDimensions[0] == b.rank))) {
                throwAPLException(InvalidDimensionsException("Size of A must match the rank of B", pos))
            }
            return TakeArrayValue(
                if (aDimensions.size == 0) intArrayOf(a.ensureNumber(pos).asInt()) else a.toIntArray(
                    pos), b, pos)
        }
    }

    override fun make(pos: Position) = TakeAPLFunctionImpl(pos)
}

class DropArrayValue(val selection: IntArray, val source: APLValue) : APLArray() {
    override val dimensions: Dimensions
    private val sourceDimensions: Dimensions

    init {
        sourceDimensions = source.dimensions
        dimensions =
            Dimensions(selection.mapIndexed { index, v -> sourceDimensions[index] - v.absoluteValue }.toIntArray())
    }

    override fun valueAt(p: Int): APLValue {
        val coords = dimensions.positionFromIndex(p)
        val adjusted = IntArray(coords.size) { i ->
            val d = selection[i]
            val v = coords[i]
            if (d >= 0) {
                d + v
            } else {
                v
            }
        }
        return source.valueAt(sourceDimensions.indexFromPosition(adjusted))
    }
}

class DropResultValueOneArg(val a: APLValue) : APLArray() {
    override val dimensions: Dimensions

    init {
        val d = a.dimensions
        if (d.size != 1) {
            TODO("One-argument drop is only supported for 1-dimensional arrays")
        }
        dimensions = dimensionsOfSize(d[0] - 1)
    }

    override fun valueAt(p: Int) = a.valueAt(p + 1)
}

class DropAPLFunction : APLFunctionDescriptor {
    class DropAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return DropResultValueOneArg(a)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val aDimensions = a.dimensions
            if (!((aDimensions.size == 0 && b.rank == 1) ||
                            (aDimensions.size == 1 && aDimensions[0] == b.rank))
            ) {
                throwAPLException(InvalidDimensionsException("Size of A must match the rank of B", pos))
            }
            return DropArrayValue(
                if (aDimensions.size == 0) intArrayOf(a.ensureNumber(pos).asInt()) else a.toIntArray(
                    pos), b)
        }
    }

    override fun make(pos: Position) = DropAPLFunctionImpl(pos)
}

class RandomAPLFunction : APLFunctionDescriptor {
    class RandomAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val v = a.unwrapDeferredValue()
            return if (v is APLSingleValue) {
                makeRandom(v.ensureNumber(pos).asLong()).makeAPLNumber()
            } else {
                APLArrayLong(v.dimensions, LongArray(v.dimensions.contentSize()) { index -> makeRandom(v.valueAtLong(index, pos)) })
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val aInt = a.ensureNumber(pos).asInt()
            val bLong = b.ensureNumber(pos).asLong()
            if (aInt < 0) {
                throwAPLException(APLIncompatibleDomainsException("A should not be negative, was: ${aInt}", pos))
            }
            if (bLong < 0) {
                throwAPLException(APLIncompatibleDomainsException("B should not be negative, was: ${aInt}", pos))
            }
            if (aInt > bLong) {
                throwAPLException(
                    APLIncompatibleDomainsException(
                        "A should not be greater than B. A: ${aInt}, B: ${bLong}",
                        pos))
            }
            if (aInt == 0) {
                return APLArrayLong(dimensionsOfSize(0), longArrayOf())
            }

            val result = randSubsetC2(aInt, bLong)
            return APLArrayLong(dimensionsOfSize(result.size), result)
        }

        private fun makeRandom(limit: Long): Long {
            return (0 until limit).random()
        }

        private fun randSubsetC2(a: Int, b: Long): LongArray {
            val rp = LongArray(a) { i -> i.toLong() }
            val map = HashMap<Long, Long>(0)
            repeat(a) { i ->
                val j = makeRandom(b - i) + i
                if (j < a) {
                    val jInt = j.toInt()
                    val c = rp[jInt]
                    rp[jInt] = rp[i]
                    rp[i] = c
                } else {
                    rp[i] = (map[j] ?: j).also { map[j] = rp[i] }
                }
            }
            return rp
        }
    }

    override fun make(pos: Position) = RandomAPLFunctionImpl(pos)
}

class RotatedAPLValue private constructor(val source: APLValue, val axis: Int, val numShifts: Long) : APLArray() {
    private val axisActionFactors = AxisActionFactors(source.dimensions, axis)

    override val dimensions get() = source.dimensions

    override fun valueAt(p: Int): APLValue {
        return axisActionFactors.withFactors(p) { highVal, lowVal, axisCoord ->
            val coord = (axisCoord + numShifts).plusMod(dimensions[axis].toLong()).toInt()
            source.valueAt((highVal * axisActionFactors.highValFactor) + (coord * axisActionFactors.multipliers[axis]) + lowVal)
        }
    }

    companion object {
        fun make(source: APLValue, axis: Int, numShifts: Long): APLValue {
            val dimensions = source.dimensions
            return if (dimensions.isEmpty() || numShifts % (dimensions[axis]) == 0L) {
                source
            } else {
                RotatedAPLValue(source, axis, numShifts)
            }
        }
    }
}

class MultiRotationRotatedAPLValue(
    val source: APLValue,
    val axis: Int,
    val selectionMultipliers: IntArray,
    val selection: IntArray
) : APLArray() {
    override val dimensions = source.dimensions

    private val axisActionFactors = AxisActionFactors(source.dimensions, axis)
    private val sourceMultipliers = dimensions.multipliers()

    override fun valueAt(p: Int): APLValue {
        val coords = Dimensions.positionFromIndexWithMultipliers(p, sourceMultipliers)
        var curr = 0
        repeat(selectionMultipliers.size) { i ->
            val dimensionIndex = coords[if (i < axis) i else i + 1]
            curr += selectionMultipliers[i] * dimensionIndex
        }
        val numShifts = selection[curr]
        return axisActionFactors.withFactors(p) { highVal, lowVal, axisCoord ->
            val coord = (axisCoord + numShifts.toLong()).plusMod(dimensions[axis].toLong()).toInt()
            source.valueAt((highVal * axisActionFactors.highValFactor) + (coord * axisActionFactors.multipliers[axis]) + lowVal)
        }
    }
}

class InverseAPLValue private constructor(val source: APLValue, val axis: Int) : APLArray() {
    private val axisActionFactors = AxisActionFactors(source.dimensions, axis)

    override val dimensions = source.dimensions
    override val specialisedType get() = source.specialisedType

    override val labels by lazy { resolveLabels() }

    override fun valueAt(p: Int): APLValue {
        return source.valueAt(destinationIndex(p))
    }

    override fun valueAtLong(p: Int, pos: Position?): Long {
        return source.valueAtLong(destinationIndex(p), pos)
    }

    override fun valueAtDouble(p: Int, pos: Position?): Double {
        return source.valueAtDouble(destinationIndex(p), pos)
    }

    private fun destinationIndex(p: Int): Int {
        return axisActionFactors.withFactors(p) { highVal, lowVal, axisCoord ->
            val coord = axisActionFactors.dimensions[axis] - axisCoord - 1
            (highVal * axisActionFactors.highValFactor) + (coord * axisActionFactors.multipliers[axis]) + lowVal
        }
    }

    private fun resolveLabels(): DimensionLabels? {
        val parent = source.labels ?: return null
        val parentList = parent.labels
        val newLabels = ArrayList<List<AxisLabel?>?>()
        parentList.forEachIndexed { i, axisLabels ->
            val newAxisLabels = when {
                axisLabels == null -> null
                i == axis -> axisLabels.asReversed()
                else -> axisLabels
            }
            newLabels.add(newAxisLabels)
        }
        return DimensionLabels(newLabels)
    }

    companion object {
        fun make(source: APLValue, axis: Int): APLValue {
            val dimensions = source.dimensions
            return if (dimensions.isEmpty() || dimensions[axis] <= 1) {
                source
            } else {
                InverseAPLValue(source, axis)
            }
        }
    }
}

abstract class RotateFunction(pos: Position) : APLFunction(pos) {
    override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        val axisInt = if (axis == null) defaultAxis(a) else axis.ensureNumber(pos).asInt()
        return InverseAPLValue.make(a, axisInt)
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        val axisInt = if (axis == null) defaultAxis(b) else axis.ensureNumber(pos).asInt()
        if (a.isScalar()) {
            val numShifts = a.ensureNumber(pos).asLong()
            return RotatedAPLValue.make(b, axisInt, numShifts)
        } else {
            val aCollapsed = a.collapse()
            val aDimensions = aCollapsed.dimensions
            val bDimensions = b.dimensions
            repeat(aDimensions.size) { i ->
                if (aDimensions[i] != bDimensions[if (i < axisInt) i else i + 1]) {
                    throwAPLException(InvalidDimensionsException("Invalid dimension", pos))
                }
            }
            return MultiRotationRotatedAPLValue(
                b,
                axisInt,
                aCollapsed.dimensions.multipliers(),
                aCollapsed.toIntArray(pos))
        }
    }

    abstract fun defaultAxis(value: APLValue): Int
}

class RotateHorizFunction : APLFunctionDescriptor {
    class RotateHorizFunctionImpl(pos: Position) : RotateFunction(pos) {
        override fun defaultAxis(value: APLValue) = value.rank - 1
    }

    override fun make(pos: Position) = RotateHorizFunctionImpl(pos)
}

class RotateVertFunction : APLFunctionDescriptor {
    class RotateVertFunctionImpl(pos: Position) : RotateFunction(pos) {
        override fun defaultAxis(value: APLValue) = 0
    }

    override fun make(pos: Position) = RotateVertFunctionImpl(pos)
}

class TransposedAPLValue(val transposeAxis: IntArray, val b: APLValue, pos: Position) : APLArray() {
    override val dimensions: Dimensions
    private val multipliers: IntArray
    private val bDimensions: Dimensions
    override val specialisedType get() = b.specialisedType

    override val labels by lazy { resolveLabels() }

    init {
        bDimensions = b.dimensions
        val inverseTransposedAxis = IntArray(bDimensions.size) { index ->
            var res = -1
            for (i in transposeAxis.indices) {
                if (transposeAxis[i] == index) {
                    res = i
                    break
                }
            }
            if (res == -1) {
                throwAPLException(InvalidDimensionsException("Not all axis represented in transpose definition", pos))
            }
            res
        }
        dimensions = Dimensions(IntArray(bDimensions.size) { index -> bDimensions[inverseTransposedAxis[index]] })
        multipliers = dimensions.multipliers()
    }

    override fun valueAt(p: Int): APLValue {
        return b.valueAt(destinationIndex(p))
    }

    override fun valueAtLong(p: Int, pos: Position?): Long {
        return b.valueAtLong(destinationIndex(p), pos)
    }

    override fun valueAtDouble(p: Int, pos: Position?): Double {
        return b.valueAtDouble(destinationIndex(p), pos)
    }

    private fun destinationIndex(p: Int): Int {
        val c = dimensions.positionFromIndex(p)
        val newPos = IntArray(dimensions.size) { index -> c[transposeAxis[index]] }
        return bDimensions.indexFromPosition(newPos)
    }

    private fun resolveLabels(): DimensionLabels? {
        val parent = b.labels ?: return null
        val parentList = parent.labels
        val newLabels = ArrayList<List<AxisLabel?>?>()
        for (origAxis in transposeAxis) {
            newLabels.add(parentList[origAxis])
        }
        return DimensionLabels(newLabels)
    }
}

class TransposeFunction : APLFunctionDescriptor {
    class TransposeFunctionImpl(pos: Position) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            return if (a.isScalar()) {
                a
            } else {
                val size = a.dimensions.size
                val axisArg = IntArray(size) { i -> size - i - 1 }
                TransposedAPLValue(axisArg, a, pos)
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            val a1 = a.arrayify()
            val aDimensions = a1.dimensions
            val bDimensions = b.dimensions
            if (aDimensions.size != 1 || aDimensions[0] != bDimensions.size) {
                throwAPLException(InvalidDimensionsException("Transpose arguments have wrong dimensions", pos))
            }

            if (b.isScalar()) {
                if (aDimensions[0] == 0) {
                    return b
                } else {
                    throwAPLException(
                        InvalidDimensionsException(
                            "Transpose of scalar values requires empty left argument",
                            pos))
                }
            }

            val transposeAxis = IntArray(aDimensions[0]) { index -> a1.valueAtInt(index, pos) }
            return TransposedAPLValue(transposeAxis, b, pos)
        }
    }

    override fun make(pos: Position) = TransposeFunctionImpl(pos)
}

class CompareFunction : APLFunctionDescriptor {
    class CompareFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            fun recurse(v: APLValue): Int {
                val d = v.dimensions
                return when {
                    d.size == 0 -> 0
                    d.contentSize() == 0 -> 1
                    else -> {
                        var first = true
                        var currentSize = 0
                        v.iterateMembers { inner ->
                            val size = recurse(inner)
                            if(first) {
                                currentSize = size
                                first = false
                            } else {
                                currentSize = max(currentSize, size)
                            }
                        }
                        currentSize + 1
                    }
                }
            }
            return recurse(a).makeAPLNumber()
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            return makeBoolean(a.compareEquals(b))
        }
    }

    override fun make(pos: Position) = CompareFunctionImpl(pos)
}

class CompareNotEqualFunction : APLFunctionDescriptor {
    class CompareFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val dimensions = a.dimensions
            val ret = if (dimensions.size == 0) 0 else dimensions[0]
            return ret.makeAPLNumber()
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            return makeBoolean(!a.compareEquals(b))
        }
    }

    override fun make(pos: Position) = CompareFunctionImpl(pos)
}

object MemberResultValueImpls {
    open class MemberResultValue(val context: RuntimeContext, val a: APLValue, val b: APLValue, val pos: Position) : APLArray() {
        override val dimensions = a.dimensions
        override val specialisedType get() = ArrayMemberType.LONG

        override fun valueAt(p: Int): APLValue {
            return valueAtLong(p, null).makeAPLNumber()
        }

        override fun valueAtLong(p: Int, pos: Position?): Long {
            return findInArray(a.valueAt(p).unwrapDeferredValue())
        }

        override fun unwrapDeferredValue(): APLValue {
            return if (dimensions.isEmpty()) {
                findInArray(a.disclose()).makeAPLNumber()
            } else {
                this
            }
        }

        protected open fun findInArray(target: APLValue): Long {
            b.iterateMembers { value ->
                if (target.compareEquals(value)) {
                    return 1
                }
            }
            return 0
        }
    }

    class MemberResultValueRightLong(
        context: RuntimeContext, a: APLValue, b: APLValue, pos: Position
    ) : MemberResultValue(context, a, b, pos) {
        override fun findInArray(target: APLValue): Long {
            val targetLong = target.ensureNumber(pos).asLong()
            repeat(b.size) { i ->
                if (b.valueAtLong(i, pos) == targetLong) {
                    return 1
                }
            }
            return 0
        }
    }

    fun make(context: RuntimeContext, a: APLValue, b: APLValue, pos: Position): MemberResultValue {
        val stb = b.specialisedType
        return when {
            stb === ArrayMemberType.LONG -> MemberResultValueRightLong(context, a, b, pos)
            else -> MemberResultValue(context, a, b, pos)
        }
    }
}

class MemberFunction : APLFunctionDescriptor {
    class MemberFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            return MemberResultValueImpls.make(context, a, b, pos)
        }
    }

    override fun make(pos: Position) = MemberFunctionImpl(pos)
}


class FindResultValue(val context: RuntimeContext, val a: APLValue, val b: APLValue) : APLArray() {
    override val dimensions = b.dimensions
    private val aDimensions = a.dimensions
    private val aMultipliers = aDimensions.multipliers()
    private val bMultipliers = dimensions.multipliers()

    override fun valueAt(p: Int): APLValue {
        val dimensionsDiff = dimensions.size - aDimensions.size
        val coord = dimensions.positionFromIndex(p)

        if (aDimensions.size > dimensions.size) {
            return makeBoolean(false)
        }

        aDimensions.dimensions.forEachIndexed { i, v ->
            if (coord[dimensionsDiff + i] > dimensions[dimensionsDiff + i] - v) {
                return makeBoolean(false)
            }
        }

        fun processOneLevel(level: Int, aCurr: Int, bCurr: Int): Boolean {
            if (level == aDimensions.size) {
                return a.valueAt(aCurr).compareEquals(b.valueAt(bCurr))
            } else {
                val axis = dimensionsDiff + level
                val aStride = aMultipliers[level]
                val bStride = bMultipliers[axis]
                val length = aDimensions[level]
                for (i in 0 until length) {
                    val ap = aCurr + i * aStride
                    val bp = bCurr + i * bStride
                    if (!processOneLevel(level + 1, ap, bp)) {
                        return false
                    }
                }
                return true
            }
        }

        return makeBoolean(processOneLevel(0, 0, p))
    }
}

class FindFunction : APLFunctionDescriptor {
    class FindFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            return if (b.dimensions.size == 0) {
                if (a.compareEquals(b)) APLLONG_1 else APLLONG_0
            } else {
                FindResultValue(context, a, b)
            }
        }
    }

    override fun make(pos: Position) = FindFunctionImpl(pos)
}

class SelectElementsValue(selectIndexes: IntArray, val b: APLValue, val axis: Int) : APLArray() {
    private val bDimensions = b.dimensions

    override val dimensions: Dimensions

    private val axisActionFactors: AxisActionFactors
    private val highMultiplier: Int
    private val bStride: Int
    private val aIndex: IntArray

    init {
        val sizeAlongAxis = selectIndexes.reduceWithInitial(0) { a, b -> a + b }
        dimensions = Dimensions(IntArray(bDimensions.size) { i ->
            if (i == axis) {
                sizeAlongAxis
            } else {
                bDimensions[i]
            }
        })

        val m = bDimensions.multipliers()
        highMultiplier = if (axis == 0) bDimensions.size else m[axis - 1]
        bStride = m[axis]

        aIndex = IntArray(sizeAlongAxis)
        var aIndexPos = 0
        var bIndexPos = 0
        for (dimensionsIndex in selectIndexes) {
            for (i2 in 0 until dimensionsIndex) {
                aIndex[aIndexPos++] = bIndexPos
            }
            bIndexPos++
        }

        axisActionFactors = AxisActionFactors(dimensions, axis)
    }

    override fun valueAt(p: Int): APLValue {
        axisActionFactors.withFactors(p) { high, low, axisCoord ->
            val bIndexPos = aIndex[axisCoord]
            val resultPos = high * highMultiplier + bIndexPos * bStride + low
            return b.valueAt(resultPos)
        }
    }
}

@Suppress("IfThenToElvis")
abstract class SelectElementsFunctionImpl(pos: Position) : APLFunction(pos) {
    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        val bFixed = b.arrayify()
        val aDimensions = a.dimensions
        val bDimensions = bFixed.dimensions
        val axisInt = if (axis == null) defaultAxis(bFixed) else axis.ensureNumber(pos).asInt()
        ensureValidAxis(axisInt, bDimensions, pos)
        if (!(aDimensions.size == 0 || (aDimensions.size == 1 && aDimensions[0] == bDimensions[axisInt]))) {
            throwAPLException(
                InvalidDimensionsException(
                    "A must be a single-dimensional array of the same size as the dimension of B along the selected axis.",
                    pos))
        }
        val selectIndexes = if (a.isScalar()) {
            a.ensureNumber(pos).asInt().let { v ->
                if (v < 0) {
                    throwAPLException(APLIncompatibleDomainsException("Selection index is negative", pos))
                }
                IntArray(bDimensions[axisInt]) { v }
            }
        } else {
            a.toIntArray(pos).onEach { v ->
                if (v < 0) {
                    throwAPLException(APLIncompatibleDomainsException("Selection index is negative", pos))
                }
            }
        }
        return SelectElementsValue(selectIndexes, bFixed, axisInt)
    }

    abstract fun defaultAxis(value: APLValue): Int
}

class SelectElementsFirstAxisFunction : APLFunctionDescriptor {
    class SelectElementsFirstAxisFunctionImpl(pos: Position) : SelectElementsFunctionImpl(pos) {
        override fun defaultAxis(value: APLValue): Int {
            return 0
        }
    }

    override fun make(pos: Position) = SelectElementsFirstAxisFunctionImpl(pos)
}

class SelectElementsLastAxisFunction : APLFunctionDescriptor {
    class SelectElementsLastAxisFunctionImpl(pos: Position) : SelectElementsFunctionImpl(pos) {
        override fun defaultAxis(value: APLValue): Int {
            return value.dimensions.lastAxis(pos)
        }
    }

    override fun make(pos: Position) = SelectElementsLastAxisFunctionImpl(pos)
}

class FormatAPLFunction : APLFunctionDescriptor {
    class FormatAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return APLString.make(a.formatted(FormatStyle.PLAIN))
        }
    }

    override fun make(pos: Position) = FormatAPLFunctionImpl(pos)
}

class ParseNumberFunction : APLFunctionDescriptor {
    class ParseNumberFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val s = a.toStringValue(pos)

            fun throwParseError(): Nothing = throwAPLException(APLEvalException("Value cannot be parsed as a number: '${s}", pos))

            val intMatch = INTEGER_PATTERN.matchEntire(s)
            if (intMatch != null) {
                return intMatch.groups.get(1)!!.value.toInt().makeAPLNumber()
            }
            val doubleMatch = DOUBLE_PATTERN.matchEntire(s)
            if (doubleMatch != null) {
                val doubleAsString = doubleMatch.groups.get(1)!!.value
                if (doubleAsString == ".") throwParseError()
                return doubleAsString.toDouble().makeAPLNumber()
            }
            throwParseError()
        }

        companion object {
            private val INTEGER_PATTERN = "^[ \t]*(-?[0-9]+)[ \t]*$".toRegex()
            private val DOUBLE_PATTERN = "^[ \t]*(-?[0-9]*\\.[0-9]*)[ \t]*$".toRegex()
        }
    }

    override fun make(pos: Position) = ParseNumberFunctionImpl(pos)
}


class WhereAPLFunction : APLFunctionDescriptor {
    class WhereAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return if (a.isScalar()) {
                val v = a.unwrapDeferredValue()
                if (v is APLNumber) {
                    APLNullValue.APL_NULL_INSTANCE
                } else {
                    throwAPLException(APLIncompatibleDomainsException("Argument must be a number", pos))
                }
            } else {
                val aDimensions = a.dimensions
                val multipliers = aDimensions.multipliers()
                val result = ArrayList<APLValue>()
                a.iterateMembersWithPosition { value, i ->
                    val n = value.ensureNumber(pos).asInt()
                    if (n > 0) {
                        val index = if (aDimensions.size == 1) {
                            i.makeAPLNumber()
                        } else {
                            val positionIndex = Dimensions.positionFromIndexWithMultipliers(i, multipliers)
                            val valueArray = Array<APLValue>(positionIndex.size) { v -> positionIndex[v].makeAPLNumber() }
                            APLArrayImpl(dimensionsOfSize(valueArray.size), valueArray)
                        }
                        repeat(n) {
                            result.add(index)
                        }
                    } else if (n < 0) {
                        throwAPLException(
                            APLIncompatibleDomainsException(
                                "Negative value found in right argument",
                                pos))
                    }
                }
                APLArrayImpl(dimensionsOfSize(result.size), result.toTypedArray())
            }
        }
    }

    override fun make(pos: Position) = WhereAPLFunctionImpl(pos)
}

class UniqueFunction : APLFunctionDescriptor {
    class UniqueFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        private fun iterateUnique(input: List<APLValue>): APLArrayImpl {
            val map = HashSet<APLValue.APLValueKey>()
            val result = ArrayList<APLValue>()
            input.forEach { a ->
                a.iterateMembers { v ->
                    val key = v.makeKey()
                    if (!map.contains(key)) {
                        result.add(v)
                        map.add(key)
                    }
                }
            }
            return APLArrayImpl(dimensionsOfSize(result.size), result.toTypedArray())
        }

        private fun collapseAndCheckRank(a: APLValue): APLValue {
            val a1 = a.arrayify().collapse()
            if (a1.rank != 1) {
                throwAPLException(
                    InvalidDimensionsException(
                        "Argument to unique must be a scalar or a 1-dimensional array",
                        pos))
            }
            return a1
        }

        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return iterateUnique(listOf(collapseAndCheckRank(a)))
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            return iterateUnique(listOf(collapseAndCheckRank(a), collapseAndCheckRank(b)))
        }
    }

    override fun make(pos: Position) = UniqueFunctionImpl(pos)
}

class IntersectionAPLFunction : APLFunctionDescriptor {
    class IntersectionAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val map = HashSet<APLValue.APLValueKey>()
            val leftKeys = HashSet<APLValue.APLValueKey>()
            val a0 = collapseAndCheckRank(a)
            val b0 = collapseAndCheckRank(b)
            b0.iterateMembers { v ->
                map.add(v.makeKey())
            }
            val result = ArrayList<APLValue>()
            a0.iterateMembers { v ->
                val key = v.makeKey()
                if (map.contains(key) && !leftKeys.contains(key)) {
                    result.add(v)
                    leftKeys.add(key)
                }
            }
            return APLArrayImpl(dimensionsOfSize(result.size), result.toTypedArray())
        }

        private fun collapseAndCheckRank(a: APLValue): APLValue {
            val a1 = a.arrayify().collapse()
            if (a1.rank != 1) {
                throwAPLException(
                    InvalidDimensionsException(
                        "Argument to intersection must be a scalar or a 1-dimensional array",
                        pos))
            }
            return a1
        }
    }

    override fun make(pos: Position) = IntersectionAPLFunctionImpl(pos)
}
