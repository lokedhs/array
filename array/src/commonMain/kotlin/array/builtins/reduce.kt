package array.builtins

import array.*

class ReduceResult1Arg(
    val context: RuntimeContext,
    val fn: APLFunction,
    val arg: APLValue,
    axis: Int,
    val pos: Position
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
        return if (reduceDepth == 0) {
            fn.identityValue()
        } else {
            var curr = arg.valueAt(posInSrc)
            for (i in 1 until reduceDepth) {
                curr = fn.eval2Arg(context, curr, arg.valueAt(i * stepLength + posInSrc), null, pos).collapse()
            }
            curr
        }
    }

    override fun unwrapDeferredValue(): APLValue {
        // Hack warning: The current implementation of reduce is inconsistent.
        // Consider the following expression: +/1 2 3 4
        // It may be obvious that the result should simply be the scalar number 10.
        //
        // However, let's consider this expression: +/(1 2) (3 4)
        //
        // In this case, we want the result to be the array 4 6. But, since reduce is designed to return
        // a result which has one dimension less that its input, and the input has dimension one, that means
        // that the result must be scalar. The result must be wrapped by an enclose.
        //
        // That means that to preserve consistency the result of the first expression should be the scalar
        // number 10 wrapped by enclose.
        //
        // APL gets around this by specifying that an enclosed number is always the number itself.
        // J on the other hand does allow enclosed numbers, but it seems to get around this problem by
        // simply not allowing the second expression in the first place. Typing it into J gives you a
        // syntax error.
        //
        // Thus, we break consistency here by adopting the APL style, while still allowing enclosed
        // numbers.
        if (dimensions.isEmpty()) {
            val v = valueAt(0).unwrapDeferredValue()
            if (v is APLSingleValue) {
                return v
            }
        }
        return this
    }
}

class ReduceOp : APLOperator {
    override fun combineFunction(fn: APLFunction, operatorAxis: Instruction?): APLFunction {
        return object : APLFunction {
            override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?, pos: Position): APLValue {
                val axisParam = if (operatorAxis != null) operatorAxis.evalWithContext(context).ensureNumber().asInt() else null
                return if (a.rank() == 0) {
                    if (axisParam != null && axisParam != 0) {
                        throw IllegalAxisException(axisParam, a.dimensions())
                    }
                    a
                } else {
                    val v = axisParam ?: (a.dimensions().size - 1)
                    ensureValidAxis(v, a.dimensions())
                    ReduceResult1Arg(context, fn, a, v, pos)
                }
            }
        }
    }
}
