package array

interface APLOperator {
    fun parseAndCombineFunctions(aplParser: APLParser, currentFn: APLFunction, opPos: Position): APLFunction
}

interface APLOperatorOneArg : APLOperator {
    override fun parseAndCombineFunctions(aplParser: APLParser, currentFn: APLFunction, opPos: Position): APLFunction {
        val axis = aplParser.parseAxis()
        return combineFunction(currentFn, axis, opPos).make(opPos)
    }

    fun combineFunction(fn: APLFunction, operatorAxis: Instruction?, pos: Position): APLFunctionDescriptor
}

private fun parseFunctionOrNull(parser: APLParser): Either<APLFunction, Pair<Token, Position>> {
    val (token, pos) = parser.tokeniser.nextTokenWithPosition()
    return when (token) {
        is Symbol -> {
            val fn = parser.tokeniser.engine.getFunction(token)
            if (fn == null) {
                parser.tokeniser.pushBackToken(token)
                Either.Right(Pair(token, pos))
            } else {
                Either.Left(fn.make(pos))
            }
        }
        is OpenFnDef -> {
            Either.Left(parser.parseFnDefinition().make(pos))
        }
        is OpenParen -> {
            val holder = parser.parseExprToplevel(CloseParen)
            if (holder !is ParseResultHolder.FnParseResult) {
                throw ParseException("Expected function", pos)
            }
            Either.Left(holder.fn)
        }
        is ApplyToken -> {
            Either.Left(parser.parseApplyDefinition().make(pos))
        }
        else -> {
            parser.tokeniser.pushBackToken(token)
            Either.Right(Pair(token, pos))
        }
    }
}

interface APLOperatorTwoArg : APLOperator {
    override fun parseAndCombineFunctions(aplParser: APLParser, currentFn: APLFunction, opPos: Position): APLFunction {
        val axis = aplParser.parseAxis()
        return when (val fn = parseFunctionOrNull(aplParser)) {
            is Either.Left -> {
                combineFunction(currentFn, fn.value, axis, opPos).make(opPos)
            }
            is Either.Right -> {
                val (symbol, pos) = fn.value
                throw ParseException("Expected function, got: ${symbol}", pos)
            }
        }
    }

    fun combineFunction(fn1: APLFunction, fn2: APLFunction, operatorAxis: Instruction?, opPos: Position): APLFunctionDescriptor
}

interface APLOperatorValueRightArg : APLOperator {
    override fun parseAndCombineFunctions(aplParser: APLParser, currentFn: APLFunction, opPos: Position): APLFunction {
        val axis = aplParser.parseAxis()
        if (axis != null) {
            throw ParseException("Axis argument not supported", opPos)
        }
        val rightArg = aplParser.parseValue()
        if (rightArg !is ParseResultHolder.InstrParseResult) {
            throw ParseException("Right argument is not a value", rightArg.pos)
        }
        aplParser.tokeniser.pushBackToken(rightArg.lastToken)
        return combineFunction(currentFn, rightArg.instr, opPos)
    }

    fun combineFunction(fn: APLFunction, instr: Instruction, opPos: Position): APLFunction
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
        return when (val fn = parseFunctionOrNull(aplParser)) {
            is Either.Left -> FnCall(currentFn, fn.value, opPos)
            is Either.Right -> {
                val valueArg = aplParser.parseValue()
                aplParser.tokeniser.pushBackToken(valueArg.lastToken)
                when (valueArg) {
                    is ParseResultHolder.FnParseResult -> throw ParseException("Function not allowed", valueArg.pos)
                    is ParseResultHolder.InstrParseResult -> ValueCall(currentFn, valueArg.instr, opPos)
                    is ParseResultHolder.EmptyParseResult -> throw ParseException("No right argument given", opPos)
                }
            }
        }
    }

    abstract inner class APLUserDefinedOperatorFunction(val leftFn: APLFunction, pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val inner = context.link(env).apply {
                assignArgs(rightArgs, a, pos)
                setVar(leftOpBinding, LambdaValue(leftFn, context))
                setVar(rightOpBinding, mkArg(context))
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
                setVar(rightOpBinding, mkArg(context))
            }
            return inner.withCallStackElement(name.nameWithNamespace(), pos) {
                instr.evalWithContext(inner)
            }
        }

        abstract fun mkArg(context: RuntimeContext): APLValue
    }

    inner class FnCall(leftFn: APLFunction, val rightFn: APLFunction, pos: Position) : APLUserDefinedOperatorFunction(leftFn, pos) {
        override fun mkArg(context: RuntimeContext) = LambdaValue(rightFn, context)
    }

    inner class ValueCall(leftFn: APLFunction, val argInstr: Instruction, pos: Position) : APLUserDefinedOperatorFunction(leftFn, pos) {
        override fun mkArg(context: RuntimeContext) = argInstr.evalWithContext(context)
    }
}
