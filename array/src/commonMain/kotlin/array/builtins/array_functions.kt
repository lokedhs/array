package array.builtins

import array.*

class IotaArray(private val size: Int, private val start: Int = 0) : APLArray() {
    override fun dimensions() = arrayOf(size)

    override fun valueAt(p: Int): APLValue {
        return APLLong((p + start).toLong())
    }
}

class IotaAPLFunction : NoAxisAPLFunction() {
    override fun eval1Arg(context: RuntimeContext, arg: APLValue): APLValue {
        if (arg is APLNumber) {
            return IotaArray(arg.asDouble().toInt())
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
    override fun valueAt(p: Int) = if (value is APLSingleValue) value else value.valueAt(p % value.size())
}

class RhoAPLFunction : NoAxisAPLFunction() {
    override fun eval1Arg(context: RuntimeContext, arg: APLValue): APLValue {
        val argDimensions = arg.dimensions()
        return APLArrayImpl(arrayOf(argDimensions.size)) { APLLong(argDimensions[it].toLong()) }
    }

    override fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue): APLValue {
        if (arg1.dimensions().size > 1) {
            throw InvalidDimensionsException("Left side of rho must be scalar or a one-dimensional array")
        }

        val d1 = Array(arg1.size()) { arg1.valueAt(it).asDouble().toInt() }
        val d2 = arg2.dimensions()
        return if (Arrays.equals(d1, d2)) {
            // The array already has the correct dimensions, simply return the old one
            arg2
        } else {
            ResizedArray(d1, arg2)
        }
    }
}

class IdentityAPLFunction : NoAxisAPLFunction() {
    override fun eval1Arg(context: RuntimeContext, arg: APLValue) = arg
    override fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue) = arg2
}

class HideAPLFunction : NoAxisAPLFunction() {
    override fun eval1Arg(context: RuntimeContext, arg: APLValue) = arg
    override fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue) = arg1
}

class EncloseAPLFunction : NoAxisAPLFunction() {
    override fun eval1Arg(context: RuntimeContext, arg: APLValue): APLValue {
        return EnclosedAPLValue(arg)
    }

    override fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue): APLValue {
        TODO("not implemented")
    }
}

class DiscloseAPLFunction : NoAxisAPLFunction() {
    override fun eval1Arg(context: RuntimeContext, arg: APLValue): APLValue {
        val rank = arg.rank()
        return when {
            arg is APLSingleValue -> arg
            rank == 0 -> arg.valueAt(0)
            else -> arg
        }
    }

    override fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue): APLValue {
        TODO("not implemented")
    }
}
