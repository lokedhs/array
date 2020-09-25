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
        var result: APLValue? = null
        for (instr in instructions) {
            result = instr.evalWithContext(context)
        }
        if (result == null) {
            throw IllegalStateException("Empty instruction list")
        }
        return result
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
                throw IncompatibleTypeException("Cannot evaluate values of type: ${v.aplValueType.typeName}", pos)
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
        return context.getVar(binding) ?: throw VariableNotAssigned(binding.name, pos)
    }

    override fun toString() = "Var(${name})"
}

class Literal1DArray(val values: List<Instruction>) : Instruction(values[0].pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val size = values.size
        val result = Array<APLValue?>(size) { null }
        for (i in (size - 1) downTo 0) {
            result[i] = values[i].evalWithContext(context)
        }
        return APLArrayImpl.make(dimensionsOfSize(size)) { result[it]!! }
    }

    override fun toString() = "Literal1DArray(${values})"
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
    override fun evalWithContext(context: RuntimeContext) = APLNullValue()
}

class EmptyValueMarker(pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) = APLEmpty()
}

class LiteralStringValue(val s: String, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) = makeAPLString(s)
}

class AssignmentInstruction(val binding: EnvironmentBinding, val instr: Instruction, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val res = instr.evalWithContext(context)
        val v = res.collapse()
        context.setVar(binding, v)
        return v
    }
}

class UserFunction(
    private val leftFnArgs: List<EnvironmentBinding>,
    private val rightFnArgs: List<EnvironmentBinding>,
    var instr: Instruction,
    private val env: Environment
) : APLFunctionDescriptor {
    inner class UserFunctionImpl(pos: Position) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            if (leftFnArgs.isNotEmpty()) {
                throw APLIllegalArgumentException("Left argument expected", pos)
            }
            val inner = context.link(env).apply {
                assignArgs(rightFnArgs, a, pos)
            }
            return instr.evalWithContext(inner)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            val inner = context.link(env).apply {
                assignArgs(leftFnArgs, a, pos)
                assignArgs(rightFnArgs, b, pos)
            }
            return instr.evalWithContext(inner)
        }
    }

    override fun make(pos: Position) = UserFunctionImpl(pos)
}
