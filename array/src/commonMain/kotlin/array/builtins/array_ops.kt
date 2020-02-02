package array.builtins

import array.*

class ForEachResult1Arg(val context: RuntimeContext, val fn: APLFunction, val value: APLValue, val axis: APLValue?) : APLArray() {
    override fun dimensions(): Dimensions = value.dimensions()
    override fun rank() = value.rank()
    override fun valueAt(p: Int) = fn.eval1Arg(context, value.valueAt(p), axis)
    override fun size() = value.size()
}

class ForEachResult2Arg(val context: RuntimeContext, val fn: APLFunction, val arg1: APLValue, val arg2: APLValue, val axis: APLValue?) :
    APLArray() {
    init {
        unless(Arrays.equals(arg1.dimensions(), arg2.dimensions())) {
            throw IncompatibleTypeException("Arguments to foreach does not have the same dimensions")
        }
    }

    override fun dimensions(): Dimensions = arg1.dimensions()
    override fun rank() = arg1.rank()
    override fun valueAt(p: Int) = fn.eval2Arg(context, arg1.valueAt(p), arg2.valueAt(p), axis)
    override fun size() = arg1.size()
}

class ForEachOp : APLOperator {
    override fun combineFunction(fn: APLFunction, operatorAxis: Instruction?): APLFunction {
        return object : APLFunction {
            override fun eval1Arg(context: RuntimeContext, arg: APLValue, axis: APLValue?): APLValue {
                return ForEachResult1Arg(context, fn, arg, axis)
            }

            override fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue, axis: APLValue?): APLValue {
                return ForEachResult2Arg(context, fn, arg1, arg2, axis)
            }
        }
    }
}

inline fun <reified T> copyArrayAndRemove(array: Array<T>, toRemove: Int): Array<T> {
    return Array(array.size - 1) { index ->
        if (index < toRemove) array[index] else array[index - 1]
    }
}

class ReduceResult1Arg(
    val context: RuntimeContext,
    val fn: APLFunction,
    val arg: APLValue,
    val axis: Int
) : APLArray() {
    private val dimensions: Dimensions
    private val stepLength: Int
    private val reduceDepth: Int

    override fun dimensions() = dimensions

    init {
        val argDimensions = arg.dimensions()

        ensureValidAxis(axis, argDimensions)

        var sl = 1
        for(i in 0 until axis) {
            sl *= argDimensions[i]
        }
        stepLength = sl

        reduceDepth = argDimensions[axis]
        dimensions = copyArrayAndRemove(arg.dimensions(), axis)
    }

    override fun valueAt(p: Int): APLValue {
        TODO("not implemented")
//        else if(rank == 1) {
//            val dimensions = arg.dimensions()
//            val length = dimensions[0]
//            if(length == 0) {
//                return APLDouble(0.0)
//            }
//            var curr = arg.valueAt(0)
//            for(i in 1 until dimensions[0]) {
//                curr = fn.eval2Arg(context, curr, arg.valueAt(i)).collapse()
//            }
//            return curr
//        }
//        else {
//            TODO("Reduce is currently only supported for rank 1 arrays")
//        }
    }
}

class ReduceOp : APLOperator {
    override fun combineFunction(fn: APLFunction, operatorAxis: Instruction?): APLFunction {
        return object : APLFunction {
            override fun eval1Arg(context: RuntimeContext, arg: APLValue, axis: APLValue?): APLValue {
                val axisInt = if (operatorAxis == null) arg.rank() - 1 else operatorAxis.evalWithContext(context).asDouble().toInt()
                return ReduceResult1Arg(context, fn, arg, axisInt)
            }

            override fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue, axis: APLValue?): APLValue {
                TODO("not implemented")
            }
        }
    }
}
