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

class IotaArray(private val numElements: Int, private val start: Int = 0) : APLArray() {
    override val dimensions get() = dimensionsOfSize(numElements)

    override fun valueAt(p: Int): APLValue {
        if (p < 0 || p >= size) {
            throw APLIndexOutOfBoundsException("Position in array: ${p}, size: ${size}")
        }
        return (p + start).makeAPLNumber()
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
            return IotaArray(a.unwrapDeferredValue().ensureNumber(pos).asInt())
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            if (a.rank > 1) {
                throw InvalidDimensionsException("Left argument must be rank 0 or 1", pos)
            }
            return FindIndexArray(a.arrayify(), b, context)
        }
    }

    override fun make(pos: Position) = IotaAPLFunctionImpl(pos)
}

class ResizedArray(override val dimensions: Dimensions, private val value: APLValue) : APLArray() {
    override fun valueAt(p: Int) = if (value is APLSingleValue) value else value.valueAt(p % value.size)
}

class RhoAPLFunction : APLFunctionDescriptor {
    class RhoAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val argDimensions = a.dimensions
            return APLArrayImpl.make(dimensionsOfSize(argDimensions.size)) { argDimensions[it].makeAPLNumber() }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            if (a.dimensions.size > 1) {
                throw InvalidDimensionsException("Left side of rho must be scalar or a one-dimensional array", pos)
            }

            val v = a.unwrapDeferredValue()
            val d1 = if (v.isScalar()) {
                dimensionsOfSize(v.ensureNumber(pos).asInt())
            } else {
                Dimensions(IntArray(v.size) { v.valueAt(it).ensureNumber(pos).asInt() })
            }
            val d2 = b.dimensions
            return if (d1.compareEquals(d2)) {
                // The array already has the correct dimensions, simply return the old one
                b
            } else {
                ResizedArray(d1, b)
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

class EncloseAPLFunction : APLFunctionDescriptor {
    class EncloseAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val v = a.unwrapDeferredValue()
            return if (v is APLSingleValue) v else EnclosedAPLValue(v)
        }
    }

    override fun make(pos: Position) = EncloseAPLFunctionImpl(pos)
}

class DisclosedArrayValue(val value: APLValue, pos: Position) : APLArray() {
    override val dimensions: Dimensions

    private val cutoffMultiplier: Int
    private val newDimensionsMultipliers: IntArray

    init {
        val d = value.dimensions
        assertx(d.size > 0)

        val m = maxShapeOf(value, pos)
        val resultDimension = Dimensions(IntArray(d.size + m.size) { i ->
            if (i < d.size) {
                d[i]
            } else {
                m[i - d.size]
            }
        })

        val multipliers = resultDimension.multipliers()
        dimensions = resultDimension
        cutoffMultiplier = multipliers[d.size - 1]
        newDimensionsMultipliers = m.multipliers()
    }

    override fun valueAt(p: Int): APLValue {
        val index = p / cutoffMultiplier
        val v = value.valueAt(index)

        val innerIndex = p % cutoffMultiplier
        return if (v.isScalar() && innerIndex == 0) {
            v
        } else {
            val d = v.dimensions
            val position = Dimensions.positionFromIndexWithMultipliers(innerIndex, newDimensionsMultipliers)
            for (i in position.indices) {
                if (position[i] >= d[i]) {
                    return v.defaultValue()
                }
            }
            return v.valueAt(d.indexFromPosition(position))
        }
    }

    private fun maxShapeOf(v: APLValue, pos: Position? = null): Dimensions {
        var elements: IntArray? = null
        v.iterateMembers { value ->
            val dimensions = value.dimensions
            if (elements == null) {
                elements = IntArray(dimensions.size) { i -> dimensions[i] }
            } else {
                if (dimensions.size != elements!!.size) {
                    throw InvalidDimensionsException("Not all elements in array have the same dimensions", pos)
                }
                for (i in 0 until elements!!.size) {
                    if (elements!![i] < dimensions[i]) {
                        elements!![i] = dimensions[i]
                    }
                }
            }
        }
        return Dimensions(elements!!) // TODO: This will crash with an empty argument
    }
}

class DiscloseAPLFunction : APLFunctionDescriptor {
    class DiscloseAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val v = a.unwrapDeferredValue()
            return when {
                v is APLSingleValue -> a
                v.isScalar() -> v.valueAt(0)
                else -> DisclosedArrayValue(v, pos)
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            if (a.dimensions.size !in 0..1) {
                throw InvalidDimensionsException("Left argument to pick should be rank 0 or 1", pos)
            }
            var curr = b
            a.arrayify().iterateMembers { v ->
                val d = v.dimensions
                if (d.size !in 0..1) {
                    throw InvalidDimensionsException("Selection should be rank 0 or 1", pos)
                }
                val index = if (d.size == 0) {
                    if (curr.dimensions.size != 1) {
                        throw InvalidDimensionsException("Mismatched dimensions for selection", pos)
                    }
                    v.ensureNumber(pos).asInt()
                } else {
                    curr.dimensions.indexFromPosition(v.toIntArray(pos), pos = pos)
                }
                if (index !in (0 until curr.size)) {
                    throw APLIndexOutOfBoundsException("Selection index out of bounds", pos)
                }
                curr = curr.valueAt(index)
            }
            return curr
        }
    }

    override fun make(pos: Position) = DiscloseAPLFunctionImpl(pos)
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
            ResizedArray(b.dimensions, a.disclose())
        } else {
            a
        }
        val b1 = if (b.isScalar()) {
            ResizedArray(a.dimensions, b.disclose())
        } else {
            b
        }
        if (a1.rank != b1.rank) {
            throw InvalidDimensionsException("Ranks of A and B are different", pos)
        }
        val aDimensions = a1.dimensions
        if (axis < 0 || axis > aDimensions.size) {
            throw IllegalAxisException("Axis must be between 0 and ${aDimensions.size} inclusive. Found: ${axis}", pos)
        }
        val bDimensions = b1.dimensions
        if (!aDimensions.compareEquals(bDimensions)) {
            throw InvalidDimensionsException(aDimensions, bDimensions, pos)
        }
        val rd = aDimensions.insert(axis, 1)
        val a2 = ResizedArray(rd, a1)
        val b2 = ResizedArray(rd, b1)
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
            throw InvalidDimensionsException("Both a and b are scalar", pos)
        }

        val a1 = if (a.rank == 0) {
            val bDimensions = b.dimensions
            ConstantArray(Dimensions(IntArray(bDimensions.size) { index -> if (index == axis) 1 else bDimensions[index] }), a.disclose())
        } else {
            a
        }

        val b1 = if (b.rank == 0) {
            val aDimensions = a.dimensions
            ConstantArray(Dimensions(IntArray(aDimensions.size) { index -> if (index == axis) 1 else aDimensions[index] }), b.disclose())
        } else {
            b
        }

        val a2 = if (b1.rank - a1.rank == 1) {
            // Reshape a1, inserting a new dimension at the position of the axis
            ResizedArray(a1.dimensions.insert(axis, 1), a1)
        } else {
            a1
        }

        val b2 = if (a1.rank - b1.rank == 1) {
            ResizedArray(b1.dimensions.insert(axis, 1), b1)
        } else {
            b1
        }

        val da = a2.dimensions
        val db = b2.dimensions

        if (da.size != db.size) {
            throw InvalidDimensionsException("Different ranks: ${da.size} compared to ${db.size}", pos)
        }

        for (i in da.indices) {
            if (i != axis && da[i] != db[i]) {
                throw InvalidDimensionsException("Dimensions at axis $axis does not match: $da compared to $db", pos)
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
            return ResizedArray(c, a)
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
                else -> ResizedArray(dimensionsOfSize(a.size), a)
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
                throw InvalidDimensionsException("Position argument is not rank 1", pos)
            }
            val bd = b.dimensions
            if (ad[0] != bd.size) {
                throw InvalidDimensionsException("Number of values in position argument must match the number of dimensions", pos)
            }
            val posList = IntArray(ad[0]) { i -> aFixed.valueAt(i).ensureNumber(pos).asInt() }
            val p = bd.indexFromPosition(posList)
            return b.valueAt(p)
        }
    }

    override fun make(pos: Position) = AccessFromIndexAPLFunctionImpl(pos)
}

class TakeArrayValue(val selection: IntArray, val source: APLValue) : APLArray() {
    override val dimensions = Dimensions(selection.map { v -> v.absoluteValue }.toIntArray())
    private val sourceDimensions = source.dimensions

    override fun valueAt(p: Int): APLValue {
        val coords = dimensions.positionFromIndex(p)
        val adjusted = IntArray(coords.size) { i ->
            val d = selection[i]
            val v = coords[i]
            if (d >= 0) {
                v
            } else {
                sourceDimensions[i] + d + v
            }
        }
        return source.valueAt(sourceDimensions.indexFromPosition(adjusted))
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
            if (!((aDimensions.size == 0 && b.rank == 1) ||
                        (aDimensions.size == 1 && aDimensions[0] == b.rank))
            ) {
                throw InvalidDimensionsException("Size of A must match the rank of B", pos)
            }
            return TakeArrayValue(if (aDimensions.size == 0) intArrayOf(a.ensureNumber(pos).asInt()) else a.toIntArray(pos), b)
        }
    }

    override fun make(pos: Position) = TakeAPLFunctionImpl(pos)
}

class DropArrayValue(val selection: IntArray, val source: APLValue) : APLArray() {
    override val dimensions: Dimensions
    private val sourceDimensions: Dimensions

    init {
        sourceDimensions = source.dimensions
        dimensions = Dimensions(selection.mapIndexed { index, v -> sourceDimensions[index] - v.absoluteValue }.toIntArray())
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
                throw InvalidDimensionsException("Size of A must match the rank of B", pos)
            }
            return DropArrayValue(if (aDimensions.size == 0) intArrayOf(a.ensureNumber(pos).asInt()) else a.toIntArray(pos), b)
        }
    }

    override fun make(pos: Position) = DropAPLFunctionImpl(pos)
}

class RandomAPLFunction : APLFunctionDescriptor {
    class RandomAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val v = a.unwrapDeferredValue()
            return if (v is APLSingleValue) {
                makeRandom(v.ensureNumber(pos))
            } else {
                APLArrayImpl.make(v.dimensions) { index -> makeRandom(v.valueAt(index).ensureNumber(pos)) }
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val aInt = a.ensureNumber(pos).asInt()
            val bLong = b.ensureNumber(pos).asLong()
            if (aInt < 0) {
                throw APLIncompatibleDomainsException("A should not be negative, was: ${aInt}", pos)
            }
            if (bLong < 0) {
                throw APLIncompatibleDomainsException("B should not be negative, was: ${aInt}", pos)
            }
            if (aInt > bLong) {
                throw APLIncompatibleDomainsException("A should not be greater than B. A: ${aInt}, B: ${bLong}", pos)
            }

            // TODO: We don't have a tree set available in Kotlin Native, so we're using two sets here. Very much suboptimal.
            val picked = HashSet<Long>()
            val result = ArrayList<Long>()
            val count = bLong + 1
            val n = aInt
            for (i in count - n until count) {
                val item = (0 until i + 1).random()
                if (picked.contains(item)) {
                    picked.add(i)
                    result.add(i)
                } else {
                    picked.add(item)
                    result.add(item)
                }
            }
            return APLArrayImpl.make(dimensionsOfSize(result.size)) { index -> result[index].makeAPLNumber() }
        }

        private fun makeRandom(limit: APLNumber): APLNumber {
            val limitLong = limit.asLong()
            return ((0 until limitLong).random()).makeAPLNumber()
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

class InverseAPLValue private constructor(val source: APLValue, val axis: Int) : APLArray() {
    private val axisActionFactors = AxisActionFactors(source.dimensions, axis)

    override val dimensions get() = source.dimensions

    override val labels by lazy { resolveLabels() }

    override fun valueAt(p: Int): APLValue {
        return axisActionFactors.withFactors(p) { highVal, lowVal, axisCoord ->
            val coord = axisActionFactors.dimensions[axis] - axisCoord - 1
            source.valueAt((highVal * axisActionFactors.highValFactor) + (coord * axisActionFactors.multipliers[axis]) + lowVal)
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
        val numShifts = a.ensureNumber(pos).asLong()
        val axisInt = if (axis == null) defaultAxis(b) else axis.ensureNumber(pos).asInt()
        return RotatedAPLValue.make(b, axisInt, numShifts)
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
    private val inverseTransposedAxis: IntArray

    override val labels by lazy { resolveLabels() }

    init {
        bDimensions = b.dimensions
        inverseTransposedAxis = IntArray(bDimensions.size) { index ->
            var res = -1
            for (i in transposeAxis.indices) {
                if (transposeAxis[i] == index) {
                    res = i
                    break
                }
            }
            if (res == -1) {
                throw InvalidDimensionsException("Not all axis represented in transpose definition", pos)
            }
            res
        }
        dimensions = Dimensions(IntArray(bDimensions.size) { index -> bDimensions[inverseTransposedAxis[index]] })
        multipliers = dimensions.multipliers()
    }

    override fun valueAt(p: Int): APLValue {
        val c = dimensions.positionFromIndex(p)
        val newPos = IntArray(dimensions.size) { index -> c[transposeAxis[index]] }
        return b.valueAt(bDimensions.indexFromPosition(newPos))
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
                throw InvalidDimensionsException("Transpose arguments have wrong dimensions", pos)
            }

            if (b.isScalar()) {
                if (aDimensions[0] == 0) {
                    return b
                } else {
                    throw InvalidDimensionsException("Transpose of scalar values requires empty left argument", pos)
                }
            }

            val transposeAxis = IntArray(aDimensions[0]) { index -> a1.valueAt(index).ensureNumber(pos).asInt() }
            return TransposedAPLValue(transposeAxis, b, pos)
        }
    }

    override fun make(pos: Position) = TransposeFunctionImpl(pos)
}

class CompareFunction : APLFunctionDescriptor {
    class CompareFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            return makeBoolean(a.compareEquals(b))
        }
    }

    override fun make(pos: Position) = CompareFunctionImpl(pos)
}

class CompareNotEqualFunction : APLFunctionDescriptor {
    class CompareFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            return makeBoolean(!a.compareEquals(b))
        }
    }

    override fun make(pos: Position) = CompareFunctionImpl(pos)
}

class MemberResultValue(val context: RuntimeContext, val a: APLValue, val b: APLValue) : APLArray() {
    override val dimensions = a.dimensions

    override fun valueAt(p: Int): APLValue {
        return findInArray(a.valueAt(p).unwrapDeferredValue())
    }

    override fun unwrapDeferredValue(): APLValue {
        return if (dimensions.isEmpty()) {
            findInArray(a)
        } else {
            this
        }
    }

    private fun findInArray(target: APLValue): APLValue {
        b.iterateMembers { value ->
            if (target.compareEquals(value)) {
                return APLLONG_1
            }
        }
        return APLLONG_0
    }
}

class MemberFunction : APLFunctionDescriptor {
    class MemberFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            return MemberResultValue(context, a, b)
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
                return a.valueAtWithScalarCheck(aCurr).compareEquals(b.valueAt(bCurr))
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
        val sizeAlongAxis = selectIndexes.reduce { a, b -> a + b }
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
        if (!(aDimensions.size == 0 || (aDimensions.size == 1 && aDimensions[0] == bDimensions[axisInt]))
        ) {
            throw InvalidDimensionsException(
                "A must be a single-dimensional array of the same size as the dimension of B along the selected axis.", pos)
        }
        val selectIndexes = if (a.isScalar()) {
            a.ensureNumber(pos).asInt().let { v ->
                IntArray(bDimensions[axisInt]) { v }
            }
        } else {
            a.toIntArray(pos)
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
            return makeAPLString(a.formatted(FormatStyle.PLAIN))
        }
    }

    override fun make(pos: Position) = FormatAPLFunctionImpl(pos)
}
