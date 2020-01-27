package array.builtins

import array.*

class ForEachResult1Arg(val context: RuntimeContext, val fn: APLFunction, val value: APLValue) : APLValue {
    override fun dimensions(): Dimensions = value.dimensions()
    override fun rank() = value.rank()
    override fun valueAt(p: Int) = fn.eval1Arg(context, value.valueAt(p))
    override fun size() = value.size()
}

class ForEachResult2Arg(val context: RuntimeContext, val fn: APLFunction, val arg1: APLValue, val arg2: APLValue) : APLValue {
    init {
        unless(Arrays.equals(arg1.dimensions(), arg2.dimensions())) {
            throw IncompatibleTypeException("Arguments to foreach does not have the same dimensions")
        }
    }

    override fun dimensions(): Dimensions = arg1.dimensions()
    override fun rank() = arg1.rank()
    override fun valueAt(p: Int) = fn.eval2Arg(context, arg1.valueAt(p), arg2.valueAt(p))
    override fun size() = arg1.size()
}

class ForEachOp : APLOperator {
    override fun combineFunction(fn: APLFunction): APLFunction {
        return object : APLFunction {
            override fun eval1Arg(context: RuntimeContext, arg: APLValue): APLValue {
                return ForEachResult1Arg(context, fn, arg)
            }

            override fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue): APLValue {
                return ForEachResult2Arg(context, fn, arg1, arg2)
            }
        }
    }
}
