package array.builtins

import array.*

class PowerAPLOperator : APLOperatorTwoArg {
    override fun combineFunction(
        fn1: APLFunction,
        fn2: APLFunction,
        operatorAxis: Instruction?,
        opPos: Position
    ): APLFunctionDescriptor {
        if (operatorAxis != null) {
            throw AxisNotSupported(opPos)
        }
        return PowerAPLFunctionDescriptor(fn1, fn2)
    }

    class PowerAPLFunctionDescriptor(
        val fn1: APLFunction,
        val fn2: APLFunction,
    ) : APLFunctionDescriptor {
        override fun make(pos: Position): APLFunction {
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
