package array.builtins

import array.*

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
        dimensions = arg.dimensions().remove(axis)

        var currMult = 1
        for (i in dimensions.indices) {
            val d = dimensions[i]
            currMult *= d
        }
        fromSourceMul = currMult / stepLength
        toDestMul = fromSourceMul * argDimensions[axis]
    }

    override fun valueAt(p: Int): APLValue {
        val posInSrc = ((p / fromSourceMul) * toDestMul) + p % fromSourceMul
        var curr = fn.identityValue()
        for (i in 0 until reduceDepth) {
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
