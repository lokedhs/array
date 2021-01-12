package array.builtins

import array.*
import kotlin.math.max

class PowerAPLOperator : APLOperatorTwoArg {
    override fun combineFunction(
        fn1: APLFunction,
        fn2: APLFunction,
        operatorAxis: Instruction?,
        opPos: Position
    ): APLFunctionDescriptor {
        if (operatorAxis != null) {
            throwAPLException(AxisNotSupported(opPos))
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

/*
Implementation note:

From my presentation at the Dyalog '19 user meeting, (f⍤k)x ←→ ↑f¨⊂[(-k)↑⍳≢⍴x]x. Slides to download here.
For two ranks, you have to find which ranks correspond to which arguments, then use the ⊂[…] thing on each argument separately.
It also assumes a positive rank; to convert a negative rank to positive I think the formula is k←0⌊(≢⍴x)+k.
There's also the reference implementation in BQN, which has the same functionality
as APL (it supports leading axis agreement, but only because Each does)

once you have the monadic case working, the dyadic one is basically free (maybe after extracting some stuff to functions,
and changing laziness behavior)
@dzaima (in fact, {a b c←⌽3⍴⌽⍵⍵ ⋄ ↑(⊂⍤b⊢⍺) ⍺⍺¨ ⊂⍤c⊢⍵} is an impl ofthe dyadic case from
the monadic one (with Dyalog's ↑ meaning))
 */

class RankOperator : APLOperatorValueRightArg {
    override fun combineFunction(fn: APLFunction, instr: Instruction, opPos: Position): APLFunction {
        return object : APLFunction(opPos) {
            override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
                val aReduced = a.collapseFirstLevel()
                val aDimensions = aReduced.dimensions
                val index = computeRankFromOpArg(context)
                val k = max(0, if (index < 0) aDimensions.size + index else index)
                val enclosedResult = AxisMultiDimensionEnclosedValue(aReduced, k)
                val applyRes = ForEachResult1Arg(context, fn, enclosedResult, null, pos)
                val v = DiscloseAPLFunction.discloseValue(applyRes, pos)
                return v
            }

            private fun raiseArgumentException(): Nothing {
                throwAPLException(APLIllegalArgumentException("Operator argument must be scalar or an array of 1 to 3 elements", pos))
            }

            private fun computeRankFromOpArg(context: RuntimeContext): Int {
                val opArg = instr.evalWithContext(context)

                val d = opArg.dimensions
                val res = when (d.size) {
                    0 -> {
                        opArg
                    }
                    1 -> {
                        when (d[0]) {
                            1 -> opArg.valueAt(0)
                            2 -> opArg.valueAt(1)
                            3 -> opArg.valueAt(0)
                            else -> raiseArgumentException()
                        }
                    }
                    else -> {
                        raiseArgumentException()
                    }
                }
                return res.ensureNumber(pos).asInt()
            }

            override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
                val aReduced = a.collapseFirstLevel()
                val aDimensions = aReduced.dimensions

                val bReduced = b.collapseFirstLevel()
                val bDimensions = bReduced.dimensions

                val opArg = instr.evalWithContext(context)
                val d = opArg.dimensions
                val index0: Int
                val index1: Int
                when (d.size) {
                    0 -> {
                        opArg.ensureNumber(pos).asInt().let { v ->
                            index0 = v
                            index1 = v
                        }
                    }
                    1 ->
                        when (d[0]) {
                            1 -> opArg.valueAt(0).ensureNumber(pos).asInt().let { v ->
                                index0 = v
                                index1 = v
                            }
                            2 -> {
                                index0 = opArg.valueAt(0).ensureNumber(pos).asInt()
                                index1 = opArg.valueAt(1).ensureNumber(pos).asInt()
                            }
                            3 -> {
                                index0 = opArg.valueAt(1).ensureNumber(pos).asInt()
                                index1 = opArg.valueAt(2).ensureNumber(pos).asInt()
                            }
                            else -> raiseArgumentException()
                        }
                    else -> raiseArgumentException()
                }
                val k0 = max(0, if (index0 < 0) aDimensions.size + index0 else index0)
                val enclosedResult0 = AxisMultiDimensionEnclosedValue(aReduced, k0)

                val k1 = max(0, if (index1 < 0) bDimensions.size + index1 else index1)
                val enclosedResult1 = AxisMultiDimensionEnclosedValue(bReduced, k1)

                val applyRes = ForEachFunctionDescriptor.compute2Arg(context, fn, enclosedResult0, enclosedResult1, null, pos)
                val v = DiscloseAPLFunction.discloseValue(applyRes, pos)
                return v
            }

        }
    }
}
