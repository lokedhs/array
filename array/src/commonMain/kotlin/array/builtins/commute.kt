package array.builtins

import array.*

class CommuteOp : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunctionDescriptor, operatorAxis: Instruction?, pos: Position): APLFunctionDescriptor {
        if (operatorAxis != null) {
            throw AxisNotSupported(pos)
        }
        return CommuteFunctionDescriptor(fn)
    }

    class CommuteFunctionDescriptor(val fnDescriptor: APLFunctionDescriptor) : APLFunctionDescriptor {
        override fun make(pos: Position): APLFunction {
            return object : APLFunction(pos) {
                private val fn = fnDescriptor.make(pos)

                override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
                    return fn.eval2Arg(context, a, a, axis)
                }

                override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
                    return fn.eval2Arg(context, b, a, axis)
                }
            }
        }
    }
}
