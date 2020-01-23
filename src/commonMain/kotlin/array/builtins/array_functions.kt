package array.builtins

import array.*

class IotaArray(private val size: Int, private val start: Int = 0) : APLArray() {
    override fun dimensions() = arrayOf(size)

    override fun valueAt(p: Int): APLValue {
        return APLLong((p + start).toLong())
    }

}

class IotaAPLFunction : APLFunction {
    override fun eval1Arg(context: RuntimeContext, arg: APLValue): APLValue {
        if (arg is APLNumber) {
            return IotaArray(arg.asNumber().toInt())
        } else {
            throw IllegalStateException("Needs to be rewritten once the new class hierarchy is in place")
        }
    }

    override fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue): APLValue {
        TODO("not implemented")
    }
}

class ResizedArray(private val dimensions: Dimensions, private val value: APLValue) : APLArray() {
    override fun dimensions() = dimensions
    override fun valueAt(p: Int) = value.valueAt(p % value.size())
}

class RhoAPLFunction : APLFunction {
    override fun eval1Arg(context: RuntimeContext, arg: APLValue): APLValue {
        val argDimensions = arg.dimensions()
        return APLArrayImpl(arrayOf(argDimensions.size), { APLLong(argDimensions[it].toLong()) })
    }

    override fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue): APLValue {
        if (arg1.dimensions().size > 1) {
            throw InvalidDimensionsException("Left side of rho must be scalar or a one-dimensional array")
        }

        val d1 = Array(arg1.size()) { arg1.valueAt(it).asNumber().toInt() }
        val d2 = arg2.dimensions()
        return if (Arrays.equals(d1, d2)) {
            // The array already has the correct dimensions, simply return the old one
            arg2
        } else {
            ResizedArray(d1, arg2)
        }
    }
}
