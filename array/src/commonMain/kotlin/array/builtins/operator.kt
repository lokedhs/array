package array.builtins

import array.*
import kotlin.math.min

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

class RankOperator : APLOperatorValueRightArg {
    override fun combineFunction(fn: APLFunction, instr: Instruction, opPos: Position): APLFunction {
        return object : APLFunction(opPos) {
            override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
                val aInt = a.collapseFirstLevel()
                val opArg = findAndCheckOpArg(context)
                val aDimensions = aInt.dimensions
                val newRank = (if (opArg.isScalar()) opArg else opArg.valueAt(0)).ensureNumber().asInt().let { index ->
                    min(if (index < 0) -index else aDimensions.size - index, aDimensions.size)
                }
                val newDimensions = Dimensions(IntArray(newRank) { i ->
                    aDimensions[i]
                })
                val innerDimensions = Dimensions(IntArray(aDimensions.size - newRank) { i ->
                    aDimensions[newRank + i]
                })
                val m = aDimensions.multipliers()[aDimensions.size - innerDimensions.size - 1]
                val resultArray = APLArrayImpl(newDimensions, Array(newDimensions.contentSize()) { i ->
                    val offset = i * m
                    fn.eval1Arg(context, APLArrayImpl(innerDimensions, Array(innerDimensions.contentSize()) { i2 ->
                        aInt.valueAt(offset + i2)
                     }), null)
                })
                return DisclosedArrayValue(resultArray, pos)
            }

            override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
                TODO("Not implemented")
            }

            private fun findAndCheckOpArg(context: RuntimeContext): APLValue {
                val opArg = instr.evalWithContext(context)
                val opArgDimension = opArg.dimensions
                unless(opArgDimension.size == 0 || (opArgDimension.size == 1 && opArgDimension[0] <= 2)) {
                    throw InvalidDimensionsException("Operator argument must be scalar or an array with at most 3 elements", pos)
                }
                return opArg
            }
        }
    }
}
