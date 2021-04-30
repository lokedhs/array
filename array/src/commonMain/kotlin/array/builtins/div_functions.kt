package array.builtins

import array.*

class TypeofFunction : APLFunctionDescriptor {
    class TypeofFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val v = a.unwrapDeferredValue()
            return APLSymbol(context.engine.internSymbol(v.aplValueType.typeName, context.engine.coreNamespace))
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
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return a.collapse()
        }
    }

    override fun make(pos: Position): APLFunction {
        return CompFunctionImpl(pos)
    }
}

/**
 * This value represents the result of a 1-arg function call which will only be performed
 * after the value is actually needed.
 */
class DeferredAPLValue1Arg(val fn: APLFunction, val context: RuntimeContext, val a: APLValue) : APLArray() {
    override val dimensions get() = a.dimensions
    override fun valueAt(p: Int) = fn.eval1Arg(context, a, null).valueAt(p)

    override fun unwrapDeferredValue(): APLValue {
        return fn.eval1Arg(context, a, null).unwrapDeferredValue()
    }
}

/**
 * This value represents the result of a 2-arg function call which will only be performed
 * after the value is actually needed.
 */
class DeferredAPLValue2Arg(val fn: APLFunction, val context: RuntimeContext, val a: APLValue, val b: APLValue) : APLArray() {
    override val dimensions get() = a.dimensions
    override fun valueAt(p: Int) = fn.eval2Arg(context, a, b, null).valueAt(p)

    override fun unwrapDeferredValue(): APLValue {
        return fn.eval2Arg(context, a, b, null).unwrapDeferredValue()
    }
}

class DeferAPLOperator : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, operatorAxis: Instruction?, pos: Position): APLFunctionDescriptor {
        return DeferAPLFunction(fn)
    }

    class DeferAPLFunction(val fn: APLFunction) : APLFunctionDescriptor {
        override fun make(pos: Position): APLFunction {
            return object : NoAxisAPLFunction(pos) {
                override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
                    return DeferredAPLValue1Arg(fn, context, a)
                }

                override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
                    return DeferredAPLValue2Arg(fn, context, a, b)
                }
            }
        }

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

class TagCatch(val tag: APLValue, val data: APLValue, pos: Position? = null) : APLEvalException(data.formatted(FormatStyle.PLAIN), pos)

class UnwindProtectAPLFunction : APLFunctionDescriptor {
    class UnwindProtectAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val aDimensions = a.dimensions
            if (aDimensions.size != 1 || aDimensions[0] != 2) {
                throw APLEvalException("Invalid dimensions in unwindProtect call", pos)
            }
            val fn = a.valueAt(0).collapseFirstLevel()
            if (fn !is LambdaValue) {
                throw APLEvalException("Handler function is not a lambda", pos)
            }
            val finallyHandler = a.valueAt(1).collapseFirstLevel()
            if (finallyHandler !is LambdaValue) {
                throw APLEvalException("Unwind handler is not a lambda", pos)
            }
            var thrownException: APLEvalException? = null
            var result: APLValue? = null
            try {
                result = fn.makeClosure().eval1Arg(context, APLNullValue.APL_NULL_INSTANCE, null)
            } catch (e: APLEvalException) {
                thrownException = e
            }
            finallyHandler.makeClosure().eval1Arg(context, APLNullValue.APL_NULL_INSTANCE, null)
            if (thrownException != null) {
                throw thrownException
            }
            return result!!
        }
    }

    override fun make(pos: Position) = UnwindProtectAPLFunctionImpl(pos)
}

class AtLeaveScopeOperator : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, operatorAxis: Instruction?, pos: Position): APLFunctionDescriptor {
        if (operatorAxis != null) {
            throw OperatorAxisNotSupported(pos)
        }
        return AtLeaveScopeFunctionDescriptor(fn)
    }

    class AtLeaveScopeFunctionDescriptor(val fn1Descriptor: APLFunction) : APLFunctionDescriptor {
        override fun make(pos: Position): APLFunction {
            val fn = fn1Descriptor
            return object : APLFunction(pos) {
                override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
                    context.pushReleaseCallback {
                        fn.eval1Arg(context, a, null)
                    }
                    return a
                }
            }
        }
    }
}

class ThrowFunction : APLFunctionDescriptor {
    class ThrowFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val engine = context.engine
            throwAPLException(TagCatch(APLSymbol(engine.internSymbol("error", engine.coreNamespace)), a, pos))
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            throwAPLException(TagCatch(a, b, pos))
        }
    }

    override fun make(pos: Position) = ThrowFunctionImpl(pos)
}

class CatchOperator : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, operatorAxis: Instruction?, pos: Position): APLFunctionDescriptor {
        if (operatorAxis != null) {
            throw OperatorAxisNotSupported(pos)
        }
        return CatchFunctionDescriptor(fn)
    }

    class CatchFunctionDescriptor(
        val fn1Descriptor: APLFunction
    ) : APLFunctionDescriptor {

        override fun make(pos: Position): APLFunction {
            val fn = fn1Descriptor
            return object : NoAxisAPLFunction(pos) {
                override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
                    val dimensions = a.dimensions
                    unless(dimensions.size == 2 && dimensions[1] == 2) {
                        throwAPLException(
                            APLIllegalArgumentException(
                                "Catch argument must be a two-dimensional array with two columns",
                                pos
                                                       )
                                         )
                    }
                    try {
                        return fn.eval1Arg(context, APLNullValue.APL_NULL_INSTANCE, null)
                    } catch (e: TagCatch) {
                        val sentTag = e.tag
                        val multipliers = dimensions.multipliers()
                        for (rowIndex in 0 until dimensions[0]) {
                            val checked = a.valueAt(dimensions.indexFromPosition(intArrayOf(rowIndex, 0), multipliers))
                            if (sentTag.compareEquals(checked)) {
                                val handlerFunction =
                                    a.valueAt(dimensions.indexFromPosition(intArrayOf(rowIndex, 1), multipliers)).unwrapDeferredValue()
                                if (handlerFunction !is LambdaValue) {
                                    throwAPLException(
                                        APLIllegalArgumentException(
                                            "The handler is not callable, this is currently an error.",
                                            pos))
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
                    throwAPLException(InvalidDimensionsException("Left argument must be an array of labels", pos))
                }
                if (aDimensions[0] != bDimensions[axisInt]) {
                    throwAPLException(InvalidDimensionsException("Label list has incorrect length", pos))
                }
                val extraLabels = ArrayList<List<AxisLabel?>?>(bDimensions.size)
                repeat(bDimensions.size) { i ->
                    val v = if (i == axisInt) {
                        val labelsList = ArrayList<AxisLabel?>(bDimensions[axisInt])
                        repeat(bDimensions[axisInt]) { i2 ->
                            val value = a.valueAt(i2)
                            if (value.dimensions.size != 1) {
                                throwAPLException(InvalidDimensionsException("Label should be a single-dimensional array", pos))
                            }
                            labelsList.add(if (value.dimensions[0] == 0) null else AxisLabel(value.toStringValue()))
                        }
                        labelsList
                    } else {
                        null
                    }
                    extraLabels.add(v)
                }
                return LabelledArray.make(b, extraLabels)
            } else {
                throwAPLException(APLIncompatibleDomainsException("Unable to set labels on non-array instances", pos))
            }
        }
    }

    override fun make(pos: Position) = LabelsFunctionImpl(pos)
}

class TimeMillisFunction : APLFunctionDescriptor {
    class TimeMillisFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            unless(a.ensureNumber(pos).asInt() == 0) {
                throwAPLException(APLIllegalArgumentException("Argument to timeMillis must be 0", pos))
            }
            return currentTime().makeAPLNumber()
        }
    }

    override fun make(pos: Position) = TimeMillisFunctionImpl(pos)
}

class ForcedElementTypeArray(val inner: APLValue, val overrideType: ArrayMemberType) : DelegatedValue(inner) {
    override val specialisedType get() = overrideType

    private fun maybeWrapValue(value: APLValue): APLValue {
        return if (value.specialisedType === overrideType) {
            value
        } else {
            object : DelegatedValue(value) {
                override val specialisedType get() = overrideType
            }
        }
    }

    override fun collapseInt() = maybeWrapValue(inner.collapseInt())
    override fun unwrapDeferredValue() = maybeWrapValue(inner.unwrapDeferredValue())
}

class EnsureTypeFunction(val overrideType: ArrayMemberType) : APLFunctionDescriptor {
    inner class EnsureTypeFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return ForcedElementTypeArray(a, overrideType)
        }
    }

    override fun make(pos: Position) = EnsureTypeFunctionImpl(pos)
}
