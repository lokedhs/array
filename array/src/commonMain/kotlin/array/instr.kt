package array

import array.complex.Complex

abstract class Instruction(val pos: Position) {
    abstract fun evalWithContext(context: RuntimeContext): APLValue
}

class DummyInstr(pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        throw IllegalStateException("Attempt to call dummy instruction")
    }
}

class RootEnvironmentInstruction(val environment: Environment, val instr: Instruction, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        return evalWithNewContext(context.engine)
    }

    fun evalWithNewContext(engine: Engine): APLValue {
        return instr.evalWithContext(RuntimeContext(engine, environment, engine.rootContext))
    }
}

class InstructionList(val instructions: List<Instruction>) : Instruction(instructions[0].pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        for (i in 0 until instructions.size - 1) {
            val instr = instructions[i]
            instr.evalWithContext(context).collapse()
        }
        return instructions.last().evalWithContext(context)
    }
}

class ParsedAPLList(val instructions: List<Instruction>) : Instruction(instructions[0].pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val resultList = ArrayList<APLValue>()
        instructions.forEach { instr ->
            resultList.add(instr.evalWithContext(context))
        }
        return APLList(resultList)
    }
}

class FunctionCall1Arg(
    val fn: APLFunction,
    val rightArgs: Instruction,
    val axis: Instruction?,
    pos: Position
) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) =
        fn.eval1Arg(context, rightArgs.evalWithContext(context), axis?.evalWithContext(context))

    override fun toString() = "FunctionCall1Arg(fn=${fn}, rightArgs=${rightArgs})"
}

class FunctionCall2Arg(
    val fn: APLFunction,
    val leftArgs: Instruction,
    val rightArgs: Instruction,
    val axis: Instruction?,
    pos: Position
) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val rightValue = rightArgs.evalWithContext(context)
        val axisValue = axis?.evalWithContext(context)
        val leftValue = leftArgs.evalWithContext(context)
        return fn.eval2Arg(context, leftValue, rightValue, axisValue)
    }

    override fun toString() = "FunctionCall2Arg(fn=${fn}, leftArgs=${leftArgs}, rightArgs=${rightArgs})"
}

class DynamicFunctionDescriptor(val instr: Instruction) : APLFunctionDescriptor {
    inner class DynamicFunctionImpl(pos: Position) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            return resolveFn(context).eval1Arg(context, a, axis)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            return resolveFn(context).eval2Arg(context, a, b, axis)
        }

        private fun resolveFn(context: RuntimeContext): APLFunction {
            val result = instr.evalWithContext(context)
            val v = result.unwrapDeferredValue()
            if (v !is LambdaValue) {
                throwAPLException(IncompatibleTypeException("Cannot evaluate values of type: ${v.aplValueType.typeName}", pos))
            }
            return v.makeClosure()
        }
    }

    override fun make(pos: Position): APLFunction {
        return DynamicFunctionImpl(pos)
    }
}

class VariableRef(val name: Symbol, val binding: EnvironmentBinding, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        return context.getVar(binding) ?: throwAPLException(VariableNotAssigned(binding.name, pos))
    }

    override fun toString() = "Var(${name})"
}

class Literal1DArray private constructor(val values: List<Instruction>, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val size = values.size
        val result = Array<APLValue?>(size) { null }
        for (i in (size - 1) downTo 0) {
            result[i] = values[i].evalWithContext(context)
        }
        return APLArrayImpl.make(dimensionsOfSize(size)) { result[it]!! }
    }

    override fun toString() = "Literal1DArray(${values})"

    companion object {
        fun make(values: List<Instruction>): Instruction {
            assertx(values.isNotEmpty())
            return when (val firstElement = values[0]) {
                is LiteralInteger -> {
                    collectLongValues(firstElement.value, values, firstElement.pos)
                }
                is LiteralDouble -> {
                    collectDoubleValues(firstElement.value, values, firstElement.pos)
                }
                else -> Literal1DArray(values, firstElement.pos)
            }
        }

        private fun collectLongValues(firstValue: Long, values: List<Instruction>, pos: Position): Instruction {
            val result = ArrayList<Long>()
            result.add(firstValue)
            for (i in 1 until values.size) {
                val v = values[i]
                if (v is LiteralInteger) {
                    result.add(v.value)
                } else {
                    return Literal1DArray(values, pos)
                }
            }
            return LiteralLongArray(result.toLongArray(), pos)
        }

        private fun collectDoubleValues(firstValue: Double, values: List<Instruction>, pos: Position): Instruction {
            val result = ArrayList<Double>()
            result.add(firstValue)
            for (i in 1 until values.size) {
                val v = values[i]
                if (v is LiteralDouble) {
                    result.add(v.value)
                } else {
                    return Literal1DArray(values, pos)
                }
            }
            return LiteralDoubleArray(result.toDoubleArray(), pos)
        }
    }
}

class LiteralScalarValue(val value: Instruction) : Instruction(value.pos) {
    override fun evalWithContext(context: RuntimeContext) = value.evalWithContext(context)
    override fun toString() = "LiteralScalarValue(${value})"
}

class LiteralInteger(val value: Long, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) = value.makeAPLNumber()
    override fun toString() = "LiteralInteger[value=$value]"
}

class LiteralDouble(val value: Double, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) = value.makeAPLNumber()
    override fun toString() = "LiteralDouble[value=$value]"
}

class LiteralComplex(val value: Complex, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) = value.makeAPLNumber()
    override fun toString() = "LiteralComplex[value=$value]"
}

class LiteralCharacter(val value: Int, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) = APLChar(value)
    override fun toString() = "LiteralCharacter[value=$value]"
}

class LiteralSymbol(name: Symbol, pos: Position) : Instruction(pos) {
    private val value = APLSymbol(name)
    override fun evalWithContext(context: RuntimeContext): APLValue = value
}

class LiteralAPLNullValue(pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) = APLNullValue.APL_NULL_INSTANCE
}

class EmptyValueMarker(pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) = APLEmpty()
}

class LiteralStringValue(val s: String, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) = APLString.make(s)
}

class LiteralLongArray(val value: LongArray, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue = APLArrayLong(dimensionsOfSize(value.size), value)
}

class LiteralDoubleArray(val value: DoubleArray, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue = APLArrayDouble(dimensionsOfSize(value.size), value)
}

class AssignmentInstruction(val variableList: Array<EnvironmentBinding>, val instr: Instruction, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val res = instr.evalWithContext(context)
        val v = res.collapse()
        when {
            variableList.size == 1 -> context.setVar(variableList[0], v)
            v.dimensions.size != 1 -> throwAPLException(APLEvalException("Destructuring assignment requires rank-1 value", pos))
            variableList.size != v.size -> throwAPLException(
                APLEvalException(
                    "Destructuring assignment expected ${variableList.size} results, got: ${v.size}",
                    pos))
            else -> {
                variableList.forEachIndexed { i, binding ->
                    context.setVar(binding, v.valueAt(i))
                }
            }
        }
        return v
    }
}

class UserFunction(
    private val name: Symbol,
    private var leftFnArgs: List<EnvironmentBinding>,
    private var rightFnArgs: List<EnvironmentBinding>,
    var instr: Instruction,
    private var env: Environment
) : APLFunctionDescriptor {
    inner class UserFunctionImpl(pos: Position) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            return context.withLinkedContext(env, name.nameWithNamespace(), pos) { inner ->
                inner.assignArgs(rightFnArgs, a, pos)
                instr.evalWithContext(inner)
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            return context.withLinkedContext(env, name.nameWithNamespace(), pos) { inner ->
                inner.assignArgs(leftFnArgs, a, pos)
                inner.assignArgs(rightFnArgs, b, pos)
                instr.evalWithContext(inner)
            }
        }
    }

    override fun make(pos: Position) = UserFunctionImpl(pos)
}

sealed class FunctionCallChain(pos: Position) : APLFunction(pos) {
    class Chain2(pos: Position, val fn0: APLFunction, val fn1: APLFunction) : FunctionCallChain(pos) {
        override val optimisationFlags = computeOptimisationFlags()

        private fun computeOptimisationFlags(): OptimisationFlags {
            return OptimisationFlags(0)
        }

        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            if (axis != null) throw AxisNotSupported(pos)
            val res = fn1.eval1Arg(context, a, null)
            return fn0.eval1Arg(context, res, null)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            if (axis != null) throw AxisNotSupported(pos)
            val res = fn1.eval2Arg(context, a, b, null)
            return fn0.eval1Arg(context, res, null)
        }
    }

    class Chain3(pos: Position, val fn0: APLFunction, val fn1: APLFunction, val fn2: APLFunction) : FunctionCallChain(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            if (axis != null) throw AxisNotSupported(pos)
            val right = fn2.eval1Arg(context, a, null)
            val left = fn0.eval1Arg(context, a, null)
            return fn1.eval2Arg(context, left, right, null)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            if (axis != null) throw AxisNotSupported(pos)
            val right = fn2.eval2Arg(context, a, b, null)
            val left = fn0.eval2Arg(context, a, b, null)
            return fn1.eval2Arg(context, left, right, null)
        }
    }

    companion object {
        fun make(pos: Position, fn0: APLFunction, fn1: APLFunction): FunctionCallChain {
            return when (fn1) {
                is Chain2 -> Chain3(pos, fn0, fn1.fn0, fn1.fn1)
                else -> Chain2(pos, fn0, fn1)
            }
        }
    }
}
