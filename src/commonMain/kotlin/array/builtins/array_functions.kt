package array.builtins

import array.*

class IotaArray(val size: Int, val start: Int = 0) : APLArray() {
    override val dimensions: Dimensions
        get() = arrayOf(size)

    override fun valueAt(vararg p: Int): APLValue {
        return APLLong((p[0] + start).toLong())
    }

}

class IotaAPLFunction : APLFunction {
    override fun eval1Arg(arg: APLValue): APLValue {
        if(arg is APLNumber) {
            return IotaArray(arg.asNumber().toInt())
        }
        else {
            throw IllegalStateException("Needs to be rewritten once the new clas shierarchy is in place")
        }
    }

    override fun eval2Arg(arg1: APLValue, arg2: APLValue): APLValue {
        TODO("not implemented")
    }
}

class RhoAPLFunction : APLFunction {
    override fun eval1Arg(arg: APLValue): APLValue {
        TODO("not implemented")
    }

    override fun eval2Arg(arg1: APLValue, arg2: APLValue): APLValue {
        TODO("not implemented")
    }
}