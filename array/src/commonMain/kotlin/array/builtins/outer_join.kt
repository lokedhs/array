package array.builtins

import array.*

class OuterJoinResult(val context: RuntimeContext, val a: APLValue, val b: APLValue, val fn: APLFunction) : APLArray() {
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
        return fn.eval2Arg(context, a.valueAt(aPosition), b.valueAt(bPosition), null);
    }
}

class OuterJoinOp : APLOperator {
    override fun combineFunction(fn: APLFunction, operatorAxis: Instruction?): APLFunction {
        return object : APLFunction {
            override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
                TODO("not implemented")
            }

            override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
                if (axis != null) {
                    throw APLIllegalArgumentException("outer join does not support axis arguments")
                }
                return OuterJoinResult(context, a, b, fn)
            }
        }
    }
}
