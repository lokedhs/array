package array.builtins

import array.APLFunction
import array.APLValue
import array.NoAxisAPLFunction
import array.RuntimeContext

class PrintAPLFunction : NoAxisAPLFunction() {
    override fun eval1Arg(context: RuntimeContext, arg: APLValue): APLValue {
        println(arg.formatted())
        return arg
    }

    override fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue): APLValue {
        TODO("not implemented")
    }
}
