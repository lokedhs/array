package array.builtins

import array.*
import kotlin.math.absoluteValue

class ReduceResult1Arg(
    val context: RuntimeContext,
    val fn: APLFunction,
    val fnAxis: APLValue?,
    val arg: APLValue,
    axis: Int,
    val pos: Position
) : APLArray() {
    override val dimensions: Dimensions
    private val stepLength: Int
    private val sizeAlongAxis: Int
    private val fromSourceMul: Int
    private val toDestMul: Int

    init {
        val argDimensions = arg.dimensions
        val argMultipliers = argDimensions.multipliers()

        ensureValidAxis(axis, argDimensions, pos)

        stepLength = argMultipliers[axis]
        sizeAlongAxis = argDimensions[axis]
        dimensions = argDimensions.remove(axis)

        val multipliers = dimensions.multipliers()

        fromSourceMul = if (axis == 0) dimensions.contentSize() else multipliers[axis - 1]
        toDestMul = fromSourceMul * argDimensions[axis]
    }

    override fun valueAt(p: Int): APLValue {
        return if (sizeAlongAxis == 0) {
            fn.identityValue()
        } else {
            val highPosition = p / fromSourceMul
            val lowPosition = p % fromSourceMul
            val posInSrc = highPosition * toDestMul + lowPosition

            val specialisedType = arg.specialisedType
            when {
                specialisedType === ArrayMemberType.LONG && fn.optimised2ArgIntInt() -> {
                    var curr = arg.valueAtLong(posInSrc, pos)
                    for (i in 1 until sizeAlongAxis) {
                        curr = fn.eval2ArgLongLong(context, curr, arg.valueAtLong(i * stepLength + posInSrc, pos), fnAxis)
                    }
                    curr.makeAPLNumber()
                }
                else -> {
                    var curr = arg.valueAt(posInSrc)
                    for (i in 1 until sizeAlongAxis) {
                        curr = fn.eval2Arg(context, curr, arg.valueAt(i * stepLength + posInSrc), fnAxis).collapse()
                    }
                    curr
                }
            }
        }
    }

    override fun unwrapDeferredValue(): APLValue {
        // Hack warning: The current implementation of reduce is inconsistent.
        // Consider the following expression: +/1 2 3 4
        // It may be obvious that the result should simply be the scalar number 10.
        //
        // However, let's consider this expression: +/(1 2) (3 4)
        //
        // In this case, we want the result to be the array 4 6. But, since reduce is designed to return
        // a result which has one dimension less that its input, and the input has dimension one, that means
        // that the result must be scalar. The result must be wrapped by an enclose.
        //
        // That means that to preserve consistency the result of the first expression should be the scalar
        // number 10 wrapped by enclose.
        //
        // APL gets around this by specifying that an enclosed number is always the number itself.
        // J on the other hand does allow enclosed numbers, but it seems to get around this problem by
        // simply not allowing the second expression in the first place. Typing it into J gives you a
        // syntax error.
        //
        // Thus, we break consistency here by adopting the APL style, while still allowing enclosed
        // numbers.
        if (dimensions.isEmpty()) {
            val v = valueAt(0).unwrapDeferredValue()
            if (v is APLSingleValue) {
                return v
            }
        }
        return this
    }
}

class ReduceNWiseResultValue(
    val context: RuntimeContext,
    val fn: APLFunction,
    val axis: APLValue?,
    val reductionSize: Int,
    val b: APLValue,
    operatorAxis: Int) : APLArray() {
    override val dimensions: Dimensions

    private val axisActionFactors: AxisActionFactors
    private val highMultiplier: Int
    private val axisMultiplier: Int
    private val dir: Int
    private val reductionSizeAbsolute: Int

    init {
        val bDimensions = b.dimensions
        dimensions = Dimensions(IntArray(bDimensions.size) { i ->
            val s = bDimensions[i]
            if (i == operatorAxis) {
                s - reductionSize.absoluteValue + 1
            } else {
                s
            }
        })

        val bMultipliers = bDimensions.multipliers()
        axisMultiplier = bMultipliers[operatorAxis]
        highMultiplier = axisMultiplier * bDimensions[operatorAxis]
        dir = if (reductionSize < 0) -1 else 1
        reductionSizeAbsolute = reductionSize.absoluteValue

        axisActionFactors = AxisActionFactors(dimensions, operatorAxis)
    }

    override fun valueAt(p: Int): APLValue {
        axisActionFactors.withFactors(p) { high, low, axisCoord ->
            var pos = if (reductionSize < 0) reductionSizeAbsolute - 1 else 0
            var curr = b.valueAt((high * highMultiplier) + ((axisCoord + pos) * axisMultiplier) + low)
            repeat(reductionSizeAbsolute - 1) {
                pos += dir
                val value = b.valueAt((high * highMultiplier) + ((axisCoord + pos) * axisMultiplier) + low)
                curr = fn.eval2Arg(context, curr, value, axis)
            }
            return curr
        }
    }
}

abstract class ReduceFunctionImpl(val fn: APLFunction, val operatorAxis: Instruction?, pos: Position) : APLFunction(pos) {
    override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        val axisParam = findAxis(operatorAxis, context)
        return if (a.rank == 0) {
            if (axisParam != null && axisParam != 0) {
                throwAPLException(IllegalAxisException(axisParam, a.dimensions, pos))
            }
            a
        } else {
            val v = axisParam ?: defaultAxis(a)
            ensureValidAxis(v, a.dimensions, pos)
            ReduceResult1Arg(context, fn, axis, a, v, pos)
        }
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        val bDimensions = b.dimensions
        val axisParam = findAxis(operatorAxis, context)
        val size = a.ensureNumber(pos).asInt()
        if (bDimensions.size == 0) {
            if (axisParam != null && axisParam != 0) {
                throwAPLException(IllegalAxisException(axisParam, bDimensions, pos))
            }
            return when (size) {
                1 -> APLArrayImpl(dimensionsOfSize(1), arrayOf(b))
                0 -> APLNullValue()
                -1 -> APLArrayImpl(dimensionsOfSize(1), arrayOf(APLLONG_0))
                else -> throwAPLException(InvalidDimensionsException("Invalid left argument for scalar right arg", pos))
            }
        }
        val axisInt = axisParam ?: defaultAxis(b)
        ensureValidAxis(axisInt, bDimensions, pos)
        return when {
            size.absoluteValue > bDimensions[axisInt] + 1 -> {
                throwAPLException(InvalidDimensionsException("Left argument is too large", pos))
            }
            size.absoluteValue == bDimensions[axisInt] + 1 -> {
                val d = Dimensions(IntArray(bDimensions.size) { i ->
                    if (i == axisInt) 0 else bDimensions[i]
                })
                APLArrayImpl(d, emptyArray())
            }
            else -> {
                ReduceNWiseResultValue(context, fn, axis, size, b.collapse(), axisInt)
            }
        }
    }

    abstract fun defaultAxis(a: APLValue): Int

    @Suppress("IfThenToSafeAccess")
    private fun findAxis(operatorAxis: Instruction?, context: RuntimeContext): Int? {
        return if (operatorAxis != null) {
            operatorAxis.evalWithContext(context).ensureNumber(pos).asInt()
        } else {
            null
        }
    }
}

class ReduceFunctionImplLastAxis(fn: APLFunction, operatorAxis: Instruction?, pos: Position) :
    ReduceFunctionImpl(fn, operatorAxis, pos) {
    override fun defaultAxis(a: APLValue) = a.dimensions.size - 1
}

class ReduceFunctionImplFirstAxis(fn: APLFunction, operatorAxis: Instruction?, pos: Position) :
    ReduceFunctionImpl(fn, operatorAxis, pos) {
    override fun defaultAxis(a: APLValue) = 0
}

class ReduceOpLastAxis : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, operatorAxis: Instruction?, pos: Position): APLFunctionDescriptor {
        return ReduceOpFunctionDescriptor(fn, operatorAxis)
    }

    class ReduceOpFunctionDescriptor(val fn: APLFunction, val operatorAxis: Instruction?) : APLFunctionDescriptor {
        override fun make(pos: Position): APLFunction {
            return ReduceFunctionImplLastAxis(fn, operatorAxis, pos)
        }
    }
}

class ReduceOpFirstAxis : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, operatorAxis: Instruction?, pos: Position): APLFunctionDescriptor {
        return ReduceOpFunctionDescriptor(fn, operatorAxis)
    }

    class ReduceOpFunctionDescriptor(val fn: APLFunction, val operatorAxis: Instruction?) : APLFunctionDescriptor {
        override fun make(pos: Position): APLFunction {
            return ReduceFunctionImplFirstAxis(fn, operatorAxis, pos)
        }
    }
}

// Scan is similar in concept to reduce, so we'll keep it in this file

class ScanResult1Arg(val context: RuntimeContext, val fn: APLFunction, val fnAxis: APLValue?, val a: APLValue, axis: Int) : APLArray() {
    override val dimensions = a.dimensions

    private val cachedResults = makeAtomicRefArray<APLValue>(dimensions.contentSize())
    private val axisActionFactors = AxisActionFactors(dimensions, axis)

    override fun valueAt(p: Int): APLValue {
        axisActionFactors.withFactors(p) { high, low, axisCoord ->
            var currIndex = axisCoord
            var leftValue: APLValue
            while (true) {
                val index = axisActionFactors.indexForAxis(high, low, currIndex)
                if (currIndex == 0) {
                    leftValue = cachedResults.checkOrUpdate(index) { a.valueAt(index) }
                    break
                } else {
                    val cachedVal = cachedResults[index]
                    if (cachedVal != null) {
                        leftValue = cachedVal
                        break
                    }
                }
                currIndex--
            }

            if (currIndex < axisCoord) {
                for (i in (currIndex + 1)..axisCoord) {
                    val index = axisActionFactors.indexForAxis(high, low, i)
                    leftValue =
                        cachedResults.checkOrUpdate(index) { fn.eval2Arg(context, leftValue, a.valueAt(index), fnAxis).collapse() }
                }
            }

            return leftValue
        }
    }
}

abstract class ScanFunctionImpl(val fn: APLFunction, val operatorAxis: Instruction?, pos: Position) : APLFunction(pos) {
    override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        val axisParam = if (operatorAxis != null) operatorAxis.evalWithContext(context).ensureNumber(pos).asInt() else null
        return if (a.rank == 0) {
            if (axisParam != null && axisParam != 0) {
                throwAPLException(IllegalAxisException(axisParam, a.dimensions, pos))
            }
            a
        } else {
            val v = axisParam ?: defaultAxis(a)
            ensureValidAxis(v, a.dimensions, pos)
            ScanResult1Arg(context, fn, axis, a, v)
        }
    }

    abstract fun defaultAxis(a: APLValue): Int
}

class ScanLastAxisFunctionImpl(fn: APLFunction, operatorAxis: Instruction?, pos: Position) : ScanFunctionImpl(fn, operatorAxis, pos) {
    override fun defaultAxis(a: APLValue) = a.dimensions.size - 1
}

class ScanFirstAxisFunctionImpl(fn: APLFunction, operatorAxis: Instruction?, pos: Position) : ScanFunctionImpl(fn, operatorAxis, pos) {
    override fun defaultAxis(a: APLValue) = 0
}

class ScanLastAxisOp : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, operatorAxis: Instruction?, pos: Position): APLFunctionDescriptor {
        return ScanOpFunctionDescriptor(fn, operatorAxis)
    }

    class ScanOpFunctionDescriptor(val fn: APLFunction, val operatorAxis: Instruction?) : APLFunctionDescriptor {
        override fun make(pos: Position): APLFunction {
            return ScanLastAxisFunctionImpl(fn, operatorAxis, pos)
        }
    }
}

class ScanFirstAxisOp : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, operatorAxis: Instruction?, pos: Position): APLFunctionDescriptor {
        return ScanOpFunctionDescriptor(fn, operatorAxis)
    }

    class ScanOpFunctionDescriptor(val fn: APLFunction, val operatorAxis: Instruction?) : APLFunctionDescriptor {
        override fun make(pos: Position): APLFunction {
            return ScanFirstAxisFunctionImpl(fn, operatorAxis, pos)
        }
    }
}
