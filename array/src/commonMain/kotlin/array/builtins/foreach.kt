package array.builtins

import array.*

class ForEachResult1Arg(
    val context: RuntimeContext,
    val fn: APLFunction,
    val value: APLValue,
    val axis: APLValue?,
    val pos: Position
) : APLArray() {
    override val dimensions: Dimensions
        get() = value.dimensions
    override val rank get() = value.rank
    override fun valueAt(p: Int) = fn.eval1Arg(context, value.valueAt(p), axis)
    override val size get() = value.size
}

class ForEachResult2Arg(
    val context: RuntimeContext,
    val fn: APLFunction,
    val arg1: APLValue,
    val arg2: APLValue,
    val axis: APLValue?,
    val pos: Position
) : APLArray() {
    init {
        unless(arg1.dimensions.compareEquals(arg2.dimensions)) {
            throw IncompatibleTypeException("Arguments to foreach does not have the same dimensions", pos)
        }
    }

    override val dimensions: Dimensions
        get() = arg1.dimensions
    override val rank get() = arg1.rank
    override fun valueAt(p: Int) = fn.eval2Arg(context, arg1.valueAt(p), arg2.valueAt(p), axis)
    override val size get() = arg1.size
}

class ForEachOp : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunctionDescriptor, operatorAxis: Instruction?, pos: Position): APLFunctionDescriptor {
        if (operatorAxis != null) {
            throw AxisNotSupported(pos)
        }
        return ForEachFunctionDescriptor(fn)
    }

    class ForEachFunctionDescriptor(val fnDescriptor: APLFunctionDescriptor) : APLFunctionDescriptor {
        override fun make(pos: Position): APLFunction {
            return object : APLFunction(pos) {
                private val fn = fnDescriptor.make(pos)

                override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
                    return ForEachResult1Arg(context, fn, a, axis, pos)
                }

                override fun eval2Arg(
                    context: RuntimeContext,
                    a: APLValue,
                    b: APLValue,
                    axis: APLValue?
                ): APLValue {
                    return ForEachResult2Arg(context, fn, a, b, axis, pos)
                }
            }
        }
    }
}
