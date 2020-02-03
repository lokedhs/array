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
        if (index < toRemove) array[index] else array[index + 1]
    }
}

class ReduceResult1Arg(
    val context: RuntimeContext,
    val fn: APLFunction,
    val arg: APLValue,
    axis: Int
) : APLArray() {
    private val argDimensions: Dimensions
    private val dimensions: Dimensions
    private val stepLength: Int
    private val reduceDepth: Int
    private val fromSourceMul: Int
    private val toDestMul: Int

    override fun dimensions() = dimensions

    init {
        argDimensions = arg.dimensions()

        ensureValidAxis(axis, argDimensions)

        var sl = 1
        for (i in axis + 1 until argDimensions.size) {
            sl *= argDimensions[i]
        }
        stepLength = sl

        reduceDepth = argDimensions[axis]
        dimensions = copyArrayAndRemove(arg.dimensions(), axis)

        var currMult = 1
        for(d in dimensions) {
            currMult *= d
        }
        fromSourceMul = currMult / stepLength
        toDestMul = fromSourceMul * argDimensions[axis]
    }

    override fun valueAt(p: Int): APLValue {
        val posInSrc = ((p / fromSourceMul) * toDestMul) + p % fromSourceMul
        var curr = arg.valueAt(posInSrc)
        for (i in 1 until reduceDepth) {
            curr = fn.eval2Arg(context, curr, arg.valueAt(i * stepLength + posInSrc), null)
        }
        return curr
    }
}

class ReduceOp : APLOperator {
    override fun combineFunction(fn: APLFunction, operatorAxis: Instruction?): APLFunction {
        return object : APLFunction {
            override fun eval1Arg(context: RuntimeContext, arg: APLValue, axis: APLValue?): APLValue {
                val axisParam = if (operatorAxis != null) operatorAxis.evalWithContext(context).ensureNumber().asInt() else null
                if (arg.rank() == 0) {
                    if (axisParam != null && axisParam != 0) {
                        throw IllegalAxisException(axisParam, arg.dimensions())
                    }
                    return arg

                } else {
                    val v = axisParam ?: (arg.dimensions().size - 1)
                    ensureValidAxis(v, arg.dimensions())
                    return ReduceResult1Arg(context, fn, arg, v)
                }
            }

            override fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue, axis: APLValue?): APLValue {
                TODO("not implemented")
            }
        }
    }
}
