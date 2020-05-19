package array.builtins

import array.*

class PowerAPLOperator : APLOperatorTwoArg {
    override fun combineFunction(
        fn1: APLFunctionDescriptor,
        fn2: APLFunctionDescriptor,
        operatorAxis: Instruction?,
        opPos: Position,
        fn1Pos: Position,
        fn2Pos: Position
    ): APLFunctionDescriptor {
        if (operatorAxis != null) {
            throw AxisNotSupported(opPos)
        }
        return PowerAPLFunctionDescriptor(fn1, fn2)
    }

    class PowerAPLFunctionDescriptor(
        val fn1Descriptor: APLFunctionDescriptor,
        val fn2Descriptor: APLFunctionDescriptor,
    ) : APLFunctionDescriptor {
        override fun make(pos: Position): APLFunction {
            val fn1 = fn1Descriptor.make(pos)
            val fn2 = fn2Descriptor.make(pos)
            return object : APLFunction(pos) {
                override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
                    var curr = a
                    while (true) {
                        val next = fn1.eval1Arg(context, curr, null).collapse()
                        val checkResult = fn2.eval2Arg(context, next, curr, null).collapse()
                        curr = next
                        if (checkResult.asBoolean()) {
                            break
                        }
                    }
                    return curr
                }
            }
        }
    }
}
