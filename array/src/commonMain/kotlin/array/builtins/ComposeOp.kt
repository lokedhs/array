package array.builtins

import array.*

class ComposedFunctionDescriptor(val fn1: APLFunction, val fn2: APLFunction) : APLFunctionDescriptor {
    inner class ComposedFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override val optimisationFlags: OptimisationFlags
            get() = fn1.optimisationFlags.combineWith(fn2.optimisationFlags)

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val res = fn1.eval2Arg(context, a, b, null)
            return fn2.eval1Arg(context, res, null)
        }
    }

    override fun make(pos: Position) = ComposedFunctionImpl(pos)
}


class ComposeOp : APLOperatorTwoArg {
    override fun combineFunction(fn1: APLFunction, fn2: APLFunction, operatorAxis: Instruction?, opPos: Position): APLFunctionDescriptor {
        if(operatorAxis != null) {
            throw AxisNotSupported(opPos)
        }
        return ComposedFunctionDescriptor(fn1, fn2)
    }
}
