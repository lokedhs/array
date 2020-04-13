package array

import array.complex.Complex

abstract class Instruction(val pos: Position) {
    abstract fun evalWithContext(context: RuntimeContext): APLValue
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
        val leftValue = rightArgs.evalWithContext(context)
        val rightValue = leftArgs.evalWithContext(context)
        val axisValue = axis?.evalWithContext(context)
        return fn.eval2Arg(context, rightValue, leftValue, axisValue)
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
            return v.fn
        }
    }

    override fun make(pos: Position): APLFunction {
        return DynamicFunctionImpl(pos)
    }
}

class VariableRef(val name: Symbol, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        return context.lookupVar(name) ?: throw VariableNotAssigned(name)
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
    override fun evalWithContext(context: RuntimeContext) = APLLong(value)
    override fun toString() = "LiteralInteger[value=$value]"
}

class LiteralDouble(val value: Double, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) = APLDouble(value)
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

class LiteralStringValue(val s: String, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) = makeAPLString(s)
}

class AssignmentInstruction(val name: Symbol, val instr: Instruction, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val res = instr.evalWithContext(context)
        context.setVar(name, res)
        return res
    }
}

class UserFunction(
    private val leftFnArgs: List<Symbol>,
    private val rightFnArgs: List<Symbol>,
    private val instr: Instruction
) : APLFunctionDescriptor {
    inner class UserFunctionImpl(
        pos: Position
    ) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            if (leftFnArgs.isNotEmpty()) {
                throw APLIllegalArgumentException("Left argument is empty", pos)
            }
            val inner = context.link().apply {
                assignArgs(rightFnArgs, a)
            }
            return instr.evalWithContext(inner)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            val inner = context.link().apply {
                assignArgs(leftFnArgs, a)
                assignArgs(rightFnArgs, b)
            }
            return instr.evalWithContext(inner)
        }
    }

    override fun make(pos: Position) = UserFunctionImpl(pos)
}
