package array.builtins

import array.APLFunction
import array.APLValue
import array.RuntimeContext

class AddAPLFunction : APLFunction {
    override fun eval1Arg(context: RuntimeContext, arg: APLValue): APLValue {
        // This is supposed to do a complex conjugate, but since we don't support
        // complex numbers yet we can simply return the original value
        return arg
    }

    override fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue): APLValue {
        return arg1.add(arg2)
    }
}

class SubAPLFunction : APLFunction {
    override fun eval1Arg(context: RuntimeContext, arg: APLValue): APLValue {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue): APLValue {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class MulAPLFunction : APLFunction {
    override fun eval1Arg(context: RuntimeContext, arg: APLValue): APLValue {
        TODO("not implemented")
    }

    override fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue): APLValue {
        TODO("not implemented")
    }
}

class DivAPLFunction : APLFunction {
    override fun eval1Arg(context: RuntimeContext, arg: APLValue): APLValue {
        TODO("not implemented")
    }

    override fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue): APLValue {
        TODO("not implemented")
    }
}
