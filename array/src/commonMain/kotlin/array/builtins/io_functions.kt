package array.builtins

import array.APLValue
import array.NoAxisAPLFunction
import array.RuntimeContext

class PrintAPLFunction : NoAxisAPLFunction() {
    override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
        println(a.formatted())
        return a
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
        TODO("not implemented")
    }
}
