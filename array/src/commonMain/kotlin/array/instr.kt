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
    override fun evalWithContext(context: RuntimeContext) = APLString.make(s)
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
            val inner = context.link(env).apply {
                assignArgs(rightFnArgs, a, pos)
            }
            return inner.withCallStackElement(name.nameWithNamespace(), pos) {
                instr.evalWithContext(inner)
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            val inner = context.link(env).apply {
                assignArgs(leftFnArgs, a, pos)
                assignArgs(rightFnArgs, b, pos)
            }
            return inner.withCallStackElement(name.nameWithNamespace(), pos) {
                instr.evalWithContext(inner)
            }
        }
    }

    override fun make(pos: Position) = UserFunctionImpl(pos)

    fun replaceFunctionDefinition(newFn: UserFunction) {
        assertx(newFn.name === name)
        leftFnArgs = newFn.leftFnArgs
        rightFnArgs = newFn.rightFnArgs
        instr = newFn.instr
        env = newFn.env
    }
}

class UserDefinedOperatorOneArg(
    val name: Symbol,
    val opBinding: EnvironmentBinding,
    val leftArgs: List<EnvironmentBinding>,
    val rightArgs: List<EnvironmentBinding>,
    val instr: Instruction,
    val env: Environment
) : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, operatorAxis: Instruction?, pos: Position): APLFunctionDescriptor {
        return object : APLFunctionDescriptor {
            override fun make(pos: Position): APLFunction {
                return UserDefinedOperatorFn(fn, pos)
            }
        }
    }

    inner class UserDefinedOperatorFn(val opFn: APLFunction, pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val inner = context.link(env).apply {
                assignArgs(rightArgs, a, pos)
                setVar(opBinding, LambdaValue(opFn, context))
            }
            return inner.withCallStackElement(name.nameWithNamespace(), pos) {
                instr.evalWithContext(inner)
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val inner = context.link(env).apply {
                assignArgs(leftArgs, a, pos)
                assignArgs(rightArgs, b, pos)
                setVar(opBinding, LambdaValue(opFn, context))
            }
            return inner.withCallStackElement(name.nameWithNamespace(), pos) {
                instr.evalWithContext(inner)
            }
        }
    }
}

class UserDefinedOperatorTwoArg(
    val name: Symbol,
    val leftOpBinding: EnvironmentBinding,
    val rightOpBinding: EnvironmentBinding,
    val leftArgs: List<EnvironmentBinding>,
    val rightArgs: List<EnvironmentBinding>,
    val instr: Instruction,
    val env: Environment) : APLOperator {
    override fun parseAndCombineFunctions(aplParser: APLParser, currentFn: APLFunction, opPos: Position): APLFunction {
        val axis = aplParser.parseAxis()
        if (axis != null) {
            throw ParseException("Axis argument not supported", opPos)
        }
        val rightArg = aplParser.parseValue()
        val handlerFn = when (rightArg) {
            is ParseResultHolder.FnParseResult -> OperatorFn(currentFn, rightArg.fn, opPos)
            is ParseResultHolder.InstrParseResult -> ValueFn(currentFn, rightArg.instr, opPos)
            is ParseResultHolder.EmptyParseResult -> throw ParseException("No right argument given", opPos)
        }
        aplParser.tokeniser.pushBackToken(rightArg.lastToken)
        return handlerFn
    }

    inner class OperatorFn(val leftFn: APLFunction, val rightFn: APLFunction, pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val inner = context.link(env).apply {
                assignArgs(rightArgs, a, pos)
                setVar(leftOpBinding, LambdaValue(leftFn, context))
                setVar(rightOpBinding, LambdaValue(rightFn, context))
            }
            return inner.withCallStackElement(name.nameWithNamespace(), pos) {
                instr.evalWithContext(inner)
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val inner = context.link(env).apply {
                assignArgs(leftArgs, a, pos)
                assignArgs(rightArgs, b, pos)
                setVar(leftOpBinding, LambdaValue(leftFn, context))
                setVar(rightOpBinding, LambdaValue(rightFn, context))
            }
            return inner.withCallStackElement(name.nameWithNamespace(), pos) {
                instr.evalWithContext(inner)
            }
        }
    }

    inner class ValueFn(leftFn: APLFunction, argInstr: Instruction, pos: Position) : NoAxisAPLFunction(pos) {

    }
}
