package array.builtins

import array.*

class OuterJoinResult(
    val context: RuntimeContext,
    val a: APLValue,
    val b: APLValue,
    val fn: APLFunction,
    val pos: Position
) : APLArray() {
    private val dimensions: Dimensions
    private val divisor: Int

    init {
        val aDimensions = a.dimensions()
        val bDimensions = b.dimensions()
        dimensions = Dimensions(IntArray(aDimensions.size + bDimensions.size) { index ->
            if (index < aDimensions.size) aDimensions[index] else bDimensions[index - aDimensions.size]
        })

        divisor = b.size()
    }

    override fun dimensions() = dimensions

    override fun valueAt(p: Int): APLValue {
        val aPosition = p / divisor
        val bPosition = p % divisor
        return fn.eval2Arg(context, a.valueAt(aPosition), b.valueAt(bPosition), null)
    }
}

class OuterJoinOp : APLOperator {
    override fun combineFunction(fn: APLFunctionDescriptor, operatorAxis: Instruction?): APLFunctionDescriptor {
        return OuterJoinFunctionDescriptor(fn)
    }

    class OuterJoinFunctionDescriptor(val fnDescriptor: APLFunctionDescriptor) : APLFunctionDescriptor {
        override fun make(pos: Position): APLFunction {
            val fn = fnDescriptor.make(pos)

            return object : APLFunction(fn.pos) {
                override fun eval2Arg(
                    context: RuntimeContext,
                    a: APLValue,
                    b: APLValue,
                    axis: APLValue?
                ): APLValue {
                    if (axis != null) {
                        throw APLIllegalArgumentException("outer join does not support axis arguments")
                    }
                    return OuterJoinResult(context, a, b, fn, pos)
                }
            }
        }
    }
}
