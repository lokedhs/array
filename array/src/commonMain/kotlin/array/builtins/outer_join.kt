package array.builtins

import array.*

class OuterJoinResult(
    val context: RuntimeContext,
    val a: APLValue,
    val b: APLValue,
    val fn: APLFunction,
    val pos: Position
) : APLArray() {
    override val dimensions: Dimensions
    private val divisor: Int

    init {
        val aDimensions = a.dimensions
        val bDimensions = b.dimensions
        dimensions = Dimensions(IntArray(aDimensions.size + bDimensions.size) { index ->
            if (index < aDimensions.size) aDimensions[index] else bDimensions[index - aDimensions.size]
        })

        divisor = b.size
    }

    override fun valueAt(p: Int): APLValue {
        val aPosition = p / divisor
        val bPosition = p % divisor
        return fn.eval2Arg(context, a.valueAt(aPosition), b.valueAt(bPosition), null)
    }
}

class InnerJoinResult(
    val context: RuntimeContext,
    val a: APLValue,
    val b: APLValue,
    val fn1: APLFunction,
    val fn2: APLFunction,
    val pos: Position
) : APLArray() {

    override val dimensions: Dimensions
    private val aDimensions: Dimensions
    private val bDimensions: Dimensions
    private val highFactor: Int
    private val axisSize: Int
    private val axisDimensions: Dimensions
    private val bStepSize: Int

    init {
        aDimensions = a.dimensions
        bDimensions = b.dimensions
        val leftSize = aDimensions.size - 1
        val rightSize = bDimensions.size - 1
        dimensions = Dimensions(IntArray(leftSize + rightSize) { index ->
            if (index < leftSize) aDimensions[index] else bDimensions[index - leftSize + 1]
        })

        axisSize = aDimensions[aDimensions.size - 1]
        axisDimensions = dimensionsOfSize(axisSize)
        bStepSize = bDimensions.multipliers()[0]

        val m = dimensions.multipliers()
        highFactor = (if (leftSize == 0) dimensions.contentSize() else m[leftSize - 1])
    }

    override fun valueAt(p: Int): APLValue {
        val posInA = (p / highFactor) * axisSize
        val posInB = p % highFactor

        var pa = posInA
        val leftArg = APLArrayImpl.make(axisDimensions) { a.valueAt(pa++) }

        var pb = posInB
        val rightArg = APLArrayImpl.make(axisDimensions) { b.valueAt(pb).also { pb += bStepSize } }

        val v = fn2.eval2Arg(context, leftArg, rightArg, null)
        return ReduceResult1Arg(context, fn1, v, 0, pos)
    }
}

class OuterJoinOp : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunctionDescriptor, operatorAxis: Instruction?, pos: Position): APLFunctionDescriptor {
        return OuterInnerJoinOp.OuterJoinFunctionDescriptor(fn)
    }
}

class OuterInnerJoinOp : APLOperatorTwoArg {
    override fun combineFunction(
        fn1: APLFunctionDescriptor,
        fn2: APLFunctionDescriptor,
        operatorAxis: Instruction?,
        opPos: Position,
        fn1Pos: Position,
        fn2Pos: Position): APLFunctionDescriptor {
        if (operatorAxis != null) {
            throw AxisNotSupported(opPos)
        }
        return if (fn1 is NullFunction) {
            OuterJoinFunctionDescriptor(fn2)
        } else {
            InnerJoinFunctionDescriptor(fn1, fn2)
        }
    }


    class OuterJoinFunctionDescriptor(val fnDescriptor: APLFunctionDescriptor) : APLFunctionDescriptor {
        override fun make(pos: Position): APLFunction {
            val fn = fnDescriptor.make(pos)

            return object : APLFunction(pos) {
                override fun eval2Arg(
                    context: RuntimeContext,
                    a: APLValue,
                    b: APLValue,
                    axis: APLValue?
                ): APLValue {
                    if (axis != null) {
                        throw AxisNotSupported(pos)
                    }
                    return OuterJoinResult(context, a, b, fn, pos)
                }
            }
        }
    }

    class InnerJoinFunctionDescriptor(val fn1Descriptor: APLFunctionDescriptor, val fn2Descriptor: APLFunctionDescriptor) :
        APLFunctionDescriptor {
        override fun make(pos: Position): APLFunction {
            val fn1 = fn1Descriptor.make(pos)
            val fn2 = fn2Descriptor.make(pos)
            return object : APLFunction(fn1.pos) {
                override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
                    if (axis != null) {
                        throw APLIllegalArgumentException("inner join does not support axis arguments", pos)
                    }
                    val aDimensions = a.dimensions
                    val bDimensions = b.dimensions
                    val a1 = when {
                        a.size == 1 && b.size == 1 -> a.arrayify()
                        a.size == 1 -> ConstantArray(dimensionsOfSize(bDimensions[0]), a.singleValueOrError())
                        else -> a
                    }
                    val b1 = when {
                        a.size == 1 && b.size == 1 -> b.arrayify()
                        b.size == 1 -> ConstantArray(
                            dimensionsOfSize(aDimensions[aDimensions.size - 1]),
                            b.singleValueOrError()
                        )
                        else -> b
                    }
                    val a1Dimensions = a1.dimensions
                    val b1Dimensions = b1.dimensions
                    if (a1Dimensions[a1Dimensions.size - 1] != b1Dimensions[0]) {
                        throw InvalidDimensionsException("a and b dimensions are incompatible", pos)
                    }
                    return if (a1Dimensions.size == 1 && b1Dimensions.size == 1) {
                        val v = fn2.eval2Arg(context, a1, b1, null)
                        ReduceResult1Arg(context, fn1, v, 0, pos)
                    } else {
                        InnerJoinResult(context, a1, b1, fn1, fn2, pos)
                    }
                }
            }
        }
    }
}

class NullFunction : APLFunctionDescriptor {
    class NullFunctionImpl(pos: Position) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            throw APLEvalException("null function cannot be called", pos)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            throw APLEvalException("null function cannot be called", pos)
        }
    }

    override fun make(pos: Position): APLFunction {
        // TODO: Should an error be thrown here?
        return NullFunctionImpl(pos)
    }

}
