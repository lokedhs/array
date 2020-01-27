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
    override fun combineFunction(fn: APLFunction, axis: APLValue?): APLFunction {
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

inline fun <reified T> copyArrayAndRemove(array: Array<T>, toRemove: Int): Array<T> {
    return Array(array.size - 1) { index ->
        if(index < toRemove) array[index] else array[index - 1]
    }
}

class ReduceResult1Arg(val context: RuntimeContext, val fn: APLFunction, val arg: APLValue, val axis: Int) : APLValue {
    private val dimensions = copyArrayAndRemove(arg.dimensions(), axis)

    override fun dimensions() = dimensions

    override fun valueAt(p: Int): APLValue {
        TODO("not implemented")
    }
}

class ReduceOp : APLOperator {
    override fun combineFunction(fn: APLFunction, axis: APLValue?): APLFunction {
        return object : APLFunction {
            override fun eval1Arg(context: RuntimeContext, arg: APLValue): APLValue {
                val axisInt = if(axis == null) arg.rank() - 1 else axis.asDouble().toInt()
                return ReduceResult1Arg(context, fn, arg, axisInt)
            }

            override fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue): APLValue {
                TODO("not implemented")
            }
        }
    }
}
