package array.builtins

import array.*
import kotlin.math.max

class IotaArray(private val size: Int, private val start: Int = 0) : APLArray() {
    override fun dimensions() = intArrayOf(size)

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
        return APLArrayImpl(intArrayOf(argDimensions.size)) { APLLong(argDimensions[it].toLong()) }
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
        if (a.dimensions().size > 1) {
            throw InvalidDimensionsException("Left side of rho must be scalar or a one-dimensional array")
        }

        val d1 = if (a is APLSingleValue) {
            intArrayOf(a.ensureNumber().asInt())
        } else {
            IntArray(a.size()) { a.valueAt(it).ensureNumber().asInt() }
        }
        val d2 = b.dimensions()
        return if (Arrays.equals(d1, d2)) {
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
        val rank = a.rank()
        return when {
            a is APLSingleValue -> a
            rank == 0 -> a.valueAt(0)
            else -> a
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
    private val dimensions = intArrayOf(aSize + bSize)

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
            ResizedArray(intArrayOf(a.size()), a)
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
            a.rank() == 0 && b.rank() == 0 -> APLArrayImpl(intArrayOf(2)) { i -> if (i == 0) a else b }
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
            ConstantArray(IntArray(bDimensions.size) { index -> if (index == axis) 1 else bDimensions[index] }, a)
        } else {
            a
        }

        val b1 = if (b.rank() == 0) {
            val aDimensions = a.dimensions()
            ConstantArray(IntArray(aDimensions.size) { index -> if (index == axis) 1 else aDimensions[index] }, b)
        } else {
            b
        }

        val a2 = if (b1.rank() - a1.rank() == 1) {
            // Reshape a1, inserting a new dimension at the position of the axis
            ResizedArray(copyArrayAndInsert(a1.dimensions(), axis, 1), a1)
        } else {
            a1
        }

        val b2 = if (a1.rank() - b1.rank() == 1) {
            ResizedArray(copyArrayAndInsert(b1.dimensions(), axis, 1), b1)
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
        private val multipliers: IntArray
        private val aMultipliers: IntArray
        private val bMultipliers: IntArray
        private val axisA: Int

        init {
            val ad = a.dimensions()
            val bd = b.dimensions()

            axisA = ad[axis]

            dimensions = IntArray(ad.size) { i -> if (i == axis) ad[i] + bd[i] else ad[i] }
            multipliers = dimensionsToMultipliers(dimensions)
            aMultipliers = dimensionsToMultipliers(ad)
            bMultipliers = dimensionsToMultipliers(bd)
        }

        override fun dimensions() = dimensions

        override fun valueAt(p: Int): APLValue {
            val highVal = p / (multipliers[axis] * dimensions[axis])
            val lowVal = p % (multipliers[axis])
            val axisCoord = (p % (multipliers[axis] * dimensions[axis])) / multipliers[axis]
            return if (axisCoord < axisA) {
                a.valueAt((highVal * aMultipliers[axis] * a.dimensions()[axis]) + (axisCoord * aMultipliers[axis]) + lowVal)
            } else {
                b.valueAt((highVal * bMultipliers[axis] * b.dimensions()[axis]) + ((axisCoord - axisA) * bMultipliers[axis]) + lowVal)
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
        return when {
            a.isAtom() -> a
            a.isScalar() -> a.valueAt(0)
            a.size() == 0 -> a.defaultValue()
            else -> a.valueAt(0)
        }
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        TODO("not implemented")
    }

}

class RandomAPLFunction : NoAxisAPLFunction() {
    override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
        return if (a.isAtom()) {
            makeRandom(a.ensureNumber())
        } else {
            APLArrayImpl(a.dimensions()) { index -> makeRandom(a.valueAt(index).ensureNumber()) }
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
