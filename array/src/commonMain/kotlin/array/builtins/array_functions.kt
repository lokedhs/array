package array.builtins

import array.*
import kotlin.math.max

class AxisActionFactors(val dimensions: Dimensions, val axis: Int) {
    val multipliers: IntArray
    val multiplierAxis: Int
    val highValFactor: Int

    init {
        multipliers = dimensionsToMultipliers(dimensions)
        multiplierAxis = multipliers[axis]
        highValFactor = multiplierAxis * dimensions[axis]
    }

    inline fun <T> withFactors(p: Int, fn: (high: Int, low: Int, axisCoord: Int) -> T): T {
        val highVal = p / highValFactor
        val lowVal = p % multiplierAxis
        val axisCoord = (p % highValFactor) / multiplierAxis
        return fn(highVal, lowVal, axisCoord)
    }
}

class IotaArray(private val size: Int, private val start: Int = 0) : APLArray() {
    override fun dimensions() = oneDimensionalDimensions(size)

    override fun valueAt(p: Int): APLValue {
        return APLLong((p + start).toLong())
    }
}

class IotaAPLFunction : NoAxisAPLFunction() {
    override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
        return IotaArray(a.ensureNumber().asInt())
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
        TODO("not implemented")
    }
}

class ResizedArray(private val dimensions: Dimensions, private val value: APLValue) : APLArray() {
    override fun dimensions() = dimensions
    override fun valueAt(p: Int) = if (value.isScalar()) value else value.valueAt(p % value.size())
}

class RhoAPLFunction : NoAxisAPLFunction() {
    override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
        val argDimensions = a.dimensions()
        return APLArrayImpl(oneDimensionalDimensions(argDimensions.size)) { APLLong(argDimensions[it].toLong()) }
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
        if (a.dimensions().size > 1) {
            throw InvalidDimensionsException("Left side of rho must be scalar or a one-dimensional array")
        }

        val v = a.unwrapDeferredValue()
        val d1 = if (v.isScalar()) {
            oneDimensionalDimensions(v.ensureNumber().asInt())
        } else {
            Dimensions(IntArray(v.size()) { v.valueAt(it).ensureNumber().asInt() })
        }
        val d2 = b.dimensions()
        return if (d1.compare(d2)) {
            // The array already has the correct dimensions, simply return the old one
            b
        } else {
            ResizedArray(d1, b)
        }
    }
}

class IdentityAPLFunction : NoAxisAPLFunction() {
    override fun eval1Arg(context: RuntimeContext, a: APLValue) = a
    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue) = b
}

class HideAPLFunction : NoAxisAPLFunction() {
    override fun eval1Arg(context: RuntimeContext, a: APLValue) = a
    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue) = a
}

class EncloseAPLFunction : NoAxisAPLFunction() {
    override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
        val v = a.unwrapDeferredValue()
        return if (a is APLSingleValue) {
            a
        } else {
            return EnclosedAPLValue(a)
        }
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
        TODO("not implemented")
    }
}

class DiscloseAPLFunction : NoAxisAPLFunction() {
    override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
        val v = a.unwrapDeferredValue()
        return when {
            v is APLSingleValue -> a
            v.isScalar() -> v.valueAt(0)
            else -> v
        }
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
        TODO("not implemented")
    }
}

class Concatenated1DArrays(private val a: APLValue, private val b: APLValue) : APLArray() {
    init {
        assertx(a.rank() == 1 && b.rank() == 1)
    }

    private val aSize = a.dimensions()[0]
    private val bSize = b.dimensions()[0]
    private val dimensions = oneDimensionalDimensions(aSize + bSize)

    override fun dimensions() = dimensions

    override fun valueAt(p: Int): APLValue {
        return if (p >= aSize) b.valueAt(p - aSize) else a.valueAt(p)
    }
}

class ConcatenateAPLFunction : APLFunction {
    override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        return if (a.isScalar()) {
            a
        } else {
            ResizedArray(oneDimensionalDimensions(a.size()), a)
        }
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        // This is pretty much a step-by-step reimplementation of the catenate function in the ISO spec.
        return if (axis == null) {
            joinNoAxis(a, b)
        } else {
            joinByAxis(a, b, axis.ensureNumber().asInt())
        }
    }

    private fun joinNoAxis(a: APLValue, b: APLValue): APLValue {
        return when {
            a.rank() == 0 && b.rank() == 0 -> APLArrayImpl(oneDimensionalDimensions(2)) { i -> if (i == 0) a else b }
            a.rank() <= 1 && b.rank() <= 1 -> Concatenated1DArrays(a.arrayify(), b.arrayify())
            else -> joinByAxis(a, b, max(a.rank(), b.rank()) - 1)
        }
    }

    private fun joinByAxis(a: APLValue, b: APLValue, axis: Int): APLArray {
        if (a.rank() == 0 && b.rank() == 0) {
            throw InvalidDimensionsException("Both a and b are scalar")
        }

        val a1 = if (a.rank() == 0) {
            val bDimensions = b.dimensions()
            ConstantArray(Dimensions(IntArray(bDimensions.size) { index -> if (index == axis) 1 else bDimensions[index] }), a)
        } else {
            a
        }

        val b1 = if (b.rank() == 0) {
            val aDimensions = a.dimensions()
            ConstantArray(Dimensions(IntArray(aDimensions.size) { index -> if (index == axis) 1 else aDimensions[index] }), b)
        } else {
            b
        }

        val a2 = if (b1.rank() - a1.rank() == 1) {
            // Reshape a1, inserting a new dimension at the position of the axis
            ResizedArray(a1.dimensions().insert(axis, 1), a1)
        } else {
            a1
        }

        val b2 = if (a1.rank() - b1.rank() == 1) {
            ResizedArray(b1.dimensions().insert(axis, 1), b1)
        } else {
            b1
        }

        val da = a2.dimensions()
        val db = b2.dimensions()

        if (da.size != db.size) {
            throw InvalidDimensionsException("different ranks: ${da.size} compared to ${db.size}")
        }

        for (i in da.indices) {
            if (i != axis && da[i] != db[i]) {
                throw InvalidDimensionsException("dimensions at axis $axis does not match: $da compared to $db")
            }
        }

        if (a2.size() == 0 || b2.size() == 0) {
            // Catenating an empty array, this needs an implementation
            throw RuntimeException("a2.size = ${a2.size()}, b2.size = ${b2.size()}")
        }

        if (da.size == 1 && db.size == 1) {
            return Concatenated1DArrays(a2, b2)
        }

        return ConcatenatedMultiDimensionalArrays(a2, b2, axis)
    }

    // This is an inner class since it's highly dependent on invariants that are established in the parent class
    class ConcatenatedMultiDimensionalArrays(val a: APLValue, val b: APLValue, val axis: Int) : APLArray() {
        private val dimensions: Dimensions
        private val axisA: Int
        private val multiplierAxis: Int
        private val highValFactor: Int
        private val aMultiplierAxis: Int
        private val bMultiplierAxis: Int
        private val aDimensionAxis: Int
        private val bDimensionAxis: Int

        init {
            val ad = a.dimensions()
            val bd = b.dimensions()

            axisA = ad[axis]

            dimensions = Dimensions(IntArray(ad.size) { i -> if (i == axis) ad[i] + bd[i] else ad[i] })
            val multipliers = dimensionsToMultipliers(dimensions)
            val aMultipliers = dimensionsToMultipliers(ad)
            val bMultipliers = dimensionsToMultipliers(bd)
            multiplierAxis = multipliers[axis]
            highValFactor = multiplierAxis * dimensions[axis]
            aMultiplierAxis = aMultipliers[axis]
            bMultiplierAxis = bMultipliers[axis]
            aDimensionAxis = a.dimensions()[axis]
            bDimensionAxis = b.dimensions()[axis]
        }

        override fun dimensions() = dimensions

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
    }
}

class AccessFromIndexAPLFunction : NoAxisAPLFunction() {
    override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
        TODO("not implemented")
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
        val aFixed = a.arrayify()
        val ad = aFixed.dimensions()
        if (ad.size != 1) {
            throw InvalidDimensionsException("position argument is not rank 1")
        }
        val bd = b.dimensions()
        if (ad[0] != bd.size) {
            throw InvalidDimensionsException("number of values in position argument must match the number of dimensions")
        }
        val posList = Array(ad[0]) { i -> aFixed.valueAt(i).ensureNumber().asInt() }
        val pos = indexFromDimensions(bd, posList)
        return b.valueAt(pos)
    }
}

class TakeAPLFunction : APLFunction {
    override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        val v = a.unwrapDeferredValue()
        return when {
            v is APLSingleValue -> v
            v.isScalar() -> v.valueAt(0)
            v.size() == 0 -> v.defaultValue()
            else -> v.valueAt(0)
        }
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        TODO("not implemented")
    }

}

class RandomAPLFunction : NoAxisAPLFunction() {
    override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
        val v = a.unwrapDeferredValue()
        return if (v is APLSingleValue) {
            makeRandom(v.ensureNumber())
        } else {
            APLArrayImpl(v.dimensions()) { index -> makeRandom(v.valueAt(index).ensureNumber()) }
        }
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
        TODO("not implemented")
    }

    private fun makeRandom(limit: APLNumber): APLNumber {
        val limitLong = limit.asLong()
        return APLLong((0 until limitLong).random())
    }
}

class RotatedAPLValue private constructor(val source: APLValue, val axis: Int, val numShifts: Long) : APLArray() {
    private val axisActionFactors = AxisActionFactors(source.dimensions(), axis)

    override fun dimensions() = source.dimensions()

    override fun valueAt(p: Int): APLValue {
        return axisActionFactors.withFactors(p) { highVal, lowVal, axisCoord ->
            val coord = plusMod(axisCoord + numShifts, dimensions()[axis].toLong()).toInt()
            println("coord = $coord")
            source.valueAt((highVal * axisActionFactors.highValFactor) + (coord * axisActionFactors.multipliers[axis]) + lowVal)
        }
    }

    companion object {
        fun make(value: APLValue, axis: Int, numShifts: Long): APLValue {
            val dimensions = value.dimensions()
            return if (dimensions.isEmpty() || numShifts % (dimensions[axis]) == 0L) {
                value
            } else {
                RotatedAPLValue(value, axis, numShifts)
            }
        }
    }
}

abstract class RotateFunction : APLFunction {
    override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        val axisInt = if (axis == null) defaultAxis(a) else axis.ensureNumber().asInt()
        return RotatedAPLValue.make(a, axisInt, 1)
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        val numShifts = a.ensureNumber().asLong()
        val axisInt = if (axis == null) defaultAxis(b) else axis.ensureNumber().asInt()
        return RotatedAPLValue.make(b, axisInt, numShifts)
    }

    abstract fun defaultAxis(value: APLValue): Int
}

class RotateHorizFunction : RotateFunction() {
    override fun defaultAxis(value: APLValue) = value.rank() - 1
}

class RotateVertFunction : RotateFunction() {
    override fun defaultAxis(value: APLValue) = 0
}
