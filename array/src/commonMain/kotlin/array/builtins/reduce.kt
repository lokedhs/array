package array.builtins

import array.*

class ReduceResult1Arg(
    val context: RuntimeContext,
    val fn: APLFunction,
    val arg: APLValue,
    val axis: Int,
    val pos: Position
) : APLArray() {
    override val dimensions: Dimensions
    private val stepLength: Int
    private val sizeAlongAxis: Int
    private val fromSourceMul: Int
    private val toDestMul: Int

    init {
        val argDimensions = arg.dimensions
        val argMultipliers = argDimensions.multipliers()

        ensureValidAxis(axis, argDimensions)

        stepLength = argMultipliers[axis]
        sizeAlongAxis = argDimensions[axis]
        dimensions = arg.dimensions.remove(axis)

        val multipliers = dimensions.multipliers()

        fromSourceMul = if (axis == 0) dimensions.contentSize() else multipliers[axis - 1]
        toDestMul = fromSourceMul * argDimensions[axis]
    }

    override fun valueAt(p: Int): APLValue {
        return if (sizeAlongAxis == 0) {
            fn.identityValue()
        } else {
            val highPosition = p / fromSourceMul
            val lowPosition = p % fromSourceMul
            val posInSrc = highPosition * toDestMul + lowPosition

            var curr = arg.valueAt(posInSrc)
            for (i in 1 until sizeAlongAxis) {
                curr = fn.eval2Arg(context, curr, arg.valueAt(i * stepLength + posInSrc), null).collapse()
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

class ReduceOp : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunctionDescriptor, operatorAxis: Instruction?): APLFunctionDescriptor {
        return ReduceOpFunctionDescriptor(fn, operatorAxis)
    }

    class ReduceOpFunctionDescriptor(val fnDescriptor: APLFunctionDescriptor, val operatorAxis: Instruction?) : APLFunctionDescriptor {
        override fun make(pos: Position): APLFunction {
            return object : APLFunction(pos) {
                val fn = fnDescriptor.make(pos)

                override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
                    val axisParam = if (operatorAxis != null) operatorAxis.evalWithContext(context).ensureNumber(pos).asInt() else null
                    return if (a.rank == 0) {
                        if (axisParam != null && axisParam != 0) {
                            throw IllegalAxisException(axisParam, a.dimensions, pos)
                        }
                        a
                    } else {
                        val v = axisParam ?: (a.dimensions.size - 1)
                        ensureValidAxis(v, a.dimensions)
                        ReduceResult1Arg(context, fn, a, v, pos)
                    }
                }
            }
        }
    }
}
