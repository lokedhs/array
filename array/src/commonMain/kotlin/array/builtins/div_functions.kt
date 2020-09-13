package array.builtins

import array.*

class TypeofFunction : APLFunctionDescriptor {
    class TypeofFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val v = a.unwrapDeferredValue()
            return APLSymbol(context.engine.internSymbol(v.aplValueType.typeName))
        }
    }

    override fun make(pos: Position) = TypeofFunctionImpl(pos)
}

class IsLocallyBoundFunction : APLFunctionDescriptor {
    class IsLocallyBoundFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val v = a.unwrapDeferredValue()
            return makeBoolean(context.isLocallyBound(v.ensureSymbol(pos).value))
        }
    }

    override fun make(pos: Position) = IsLocallyBoundFunctionImpl(pos)
}

class CompFunction : APLFunctionDescriptor {
    class CompFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            return a.collapse()
        }
    }

    override fun make(pos: Position): APLFunction {
        return CompFunctionImpl(pos)
    }
}

class SleepFunction : APLFunctionDescriptor {
    class SleepFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val sleepTimeSeconds = a.ensureNumber(pos).asDouble()
            sleepMillis((sleepTimeSeconds * 1000).toLong())
            return sleepTimeSeconds.makeAPLNumber()
        }
    }

    override fun make(pos: Position) = SleepFunctionImpl(pos)
}

class TagCatch(val tag: APLValue, val data: APLValue) : RuntimeException()

class ThrowFunction : APLFunctionDescriptor {
    class ThrowFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            throw TagCatch(b, a)
        }
    }

    override fun make(pos: Position) = ThrowFunctionImpl(pos)
}

class CatchOperator : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunctionDescriptor, operatorAxis: Instruction?, pos: Position): APLFunctionDescriptor {
        return CatchFunctionDescriptor(fn)
    }

    class CatchFunctionDescriptor(
        val fn1Descriptor: APLFunctionDescriptor
    ) : APLFunctionDescriptor {

        override fun make(pos: Position): APLFunction {
            val fn = fn1Descriptor.make(pos)
            return object : APLFunction(pos) {
                override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
                    val dimensions = a.dimensions
                    unless(dimensions.size == 2 && dimensions[1] == 2) {
                        throw APLIllegalArgumentException("Catch argument must be a two-dimensional array with two columns", pos)
                    }
                    try {
                        return fn.eval1Arg(context, APLNullValue(), null)
                    } catch (e: TagCatch) {
                        val sentTag = e.tag
                        val multipliers = dimensions.multipliers()
                        for (rowIndex in 0 until dimensions[0]) {
                            val checked = a.valueAt(dimensions.indexFromPosition(intArrayOf(rowIndex, 0), multipliers))
                            if (sentTag.compareEquals(checked)) {
                                val handlerFunction =
                                    a.valueAt(dimensions.indexFromPosition(intArrayOf(rowIndex, 1), multipliers)).unwrapDeferredValue()
                                if (handlerFunction !is LambdaValue) {
                                    throw APLIllegalArgumentException("The handler is not callable, this is currently an error.", pos)
                                }
                                return handlerFunction.makeClosure().eval2Arg(context, e.data, sentTag, null)
                            }
                        }
                        throw e
                    }
                }
            }
        }
    }
}

class LabelsFunction : APLFunctionDescriptor {
    class LabelsFunctionImpl(pos: Position) : APLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            if (!b.isScalar()) {
                val bDimensions = b.dimensions
                val axisInt = if (axis == null) bDimensions.lastAxis(pos) else axis.ensureNumber(pos).asInt()
                ensureValidAxis(axisInt, bDimensions, pos)

                val aDimensions = a.dimensions
                if (aDimensions.size != 1) {
                    throw InvalidDimensionsException("Left argument must be an array of labels", pos)
                }
                if (aDimensions[0] != bDimensions[axisInt]) {
                    throw InvalidDimensionsException("Label list has incorrect length", pos)
                }
                val extraLabels = ArrayList<List<AxisLabel?>?>(bDimensions.size)
                repeat(bDimensions.size) { i ->
                    val v = if (i == axisInt) {
                        val labelsList = ArrayList<AxisLabel?>(bDimensions[axisInt])
                        repeat(bDimensions[axisInt]) { i2 ->
                            val value = a.valueAt(i2)
                            if (value.dimensions.size != 1) {
                                throw InvalidDimensionsException("Label should be a single-dimensional array", pos)
                            }
                            labelsList.add(if (value.dimensions[0] == 0) null else AxisLabel(arrayAsStringValue(value)))
                        }
                        labelsList
                    } else {
                        null
                    }
                    extraLabels.add(v)
                }
                return LabelledArray.make(b, extraLabels)
            } else {
                throw APLIncompatibleDomainsException("Unable to set labels on non-array instances", pos)
            }
        }
    }

    override fun make(pos: Position) = LabelsFunctionImpl(pos)
}
