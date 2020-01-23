package array.builtins

import array.APLValue
import array.APLFunction

class AddAPLFunction : APLFunction {
    override fun eval1Arg(arg: APLValue): APLValue {
        // This is supposed to do a complex conjugate, but since we don't support
        // complex numbers yet we can simply return the original value
        return arg
    }

    override fun eval2Arg(arg1: APLValue, arg2: APLValue): APLValue {
        return arg1.add(arg2)
    }
}

class SubAPLFunction : APLFunction {
    override fun eval1Arg(arg: APLValue): APLValue {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun eval2Arg(arg1: APLValue, arg2: APLValue): APLValue {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
