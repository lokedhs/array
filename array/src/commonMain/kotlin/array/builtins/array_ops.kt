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
            override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
                return ForEachResult1Arg(context, fn, a, axis)
            }

            override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
                return ForEachResult2Arg(context, fn, a, b, axis)
            }
        }
    }
}

class ReduceResult1Arg(
    val context: RuntimeContext,
    val fn: APLFunction,
    val arg: APLValue,
    axis: Int
) : DeferredResultArray() {
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
        for (d in dimensions) {
            currMult *= d
        }
        fromSourceMul = currMult / stepLength
        toDestMul = fromSourceMul * argDimensions[axis]
    }

    override fun valueAt(p: Int): APLValue {
        val posInSrc = ((p / fromSourceMul) * toDestMul) + p % fromSourceMul
        var curr = arg.valueAt(posInSrc)
        for (i in 1 until reduceDepth) {
            curr = fn.eval2Arg(context, curr, arg.valueAt(i * stepLength + posInSrc), null).collapse()
        }
        return curr
    }
}

class ReduceOp : APLOperator {
    override fun combineFunction(fn: APLFunction, operatorAxis: Instruction?): APLFunction {
        return object : APLFunction {
            override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
                val axisParam = if (operatorAxis != null) operatorAxis.evalWithContext(context).ensureNumber().asInt() else null
                return if (a.rank() == 0) {
                    if (axisParam != null && axisParam != 0) {
                        throw IllegalAxisException(axisParam, a.dimensions())
                    }
                    a
                } else {
                    val v = axisParam ?: (a.dimensions().size - 1)
                    ensureValidAxis(v, a.dimensions())
                    ReduceResult1Arg(context, fn, a, v)
                }
            }

            override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
                TODO("not implemented")
            }
        }
    }
}
