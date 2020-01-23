package array

class PrintAPLFunction : APLFunction {
    override fun eval1Arg(arg: APLValue): APLValue {
        println(arg.formatted())
        return arg
    }

    override fun eval2Arg(arg1: APLValue, arg2: APLValue): APLValue {
        TODO("not implemented")
    }
}
