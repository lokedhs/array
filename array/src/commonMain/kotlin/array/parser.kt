package array

data class InstrTokenHolder(val instruction: Instruction, val lastToken: Token, val pos: Position)

fun parseValueToplevel(tokeniser: TokenGenerator, endToken: Token): Instruction {
    val statementList = ArrayList<Instruction>()
    while (true) {
        val holder = parseList(tokeniser)
        statementList.add(holder.instruction)
        if (holder.lastToken == endToken) {
            assertx(!statementList.isEmpty())
            return if (statementList.size == 1) holder.instruction else InstructionList(statementList)
        } else if (holder.lastToken != StatementSeparator) {
            throw UnexpectedToken(holder.lastToken, holder.pos)
        }
    }
}

fun parseList(tokeniser: TokenGenerator): InstrTokenHolder {
    val statementList = ArrayList<Instruction>()
    while (true) {
        val holder = parseValue(tokeniser)
        if (holder.lastToken == ListSeparator) {
            statementList.add(holder.instruction)
        } else {
            val list = if (statementList.isEmpty()) {
                holder.instruction
            } else {
                statementList.add(holder.instruction)
                ParsedAPLList(statementList)
            }
            return InstrTokenHolder(list, holder.lastToken, holder.pos)
        }
    }
}

fun parseValue(tokeniser: TokenGenerator): InstrTokenHolder {
    val engine = tokeniser.engine
    val leftArgs = ArrayList<Instruction>()

    fun makeResultList(): Instruction {
        if (leftArgs.isEmpty()) {
            throw ParseException("Argument list should not be empty")
        }
        return if (leftArgs.size == 1) {
            LiteralScalarValue(leftArgs[0])
        } else {
            Literal1DArray(leftArgs)
        }
    }

    fun processFn(fn: APLFunctionDescriptor, pos: Position): InstrTokenHolder {
        val axis = parseAxis(tokeniser)
        val parsedFn = parseOperator(fn, tokeniser)
        val (rightValue, lastToken) = parseValue(tokeniser)
        return if (leftArgs.isEmpty()) {
            InstrTokenHolder(FunctionCall1Arg(parsedFn.make(pos), rightValue, axis, pos), lastToken, pos)
        } else {
            InstrTokenHolder(FunctionCall2Arg(parsedFn.make(pos), makeResultList(), rightValue, axis, pos), lastToken, pos)
        }
    }

    fun processAssignment(tokeniser: TokenGenerator, pos: Position): InstrTokenHolder {
        // Ensure that the left argument to leftarrow is a single symbol
        unless(leftArgs.size == 1) {
            throw IncompatibleTypeParseException("Can only assign to a single variable", pos)
        }
        val dest = leftArgs[0]
        if (dest !is VariableRef) {
            throw IncompatibleTypeParseException("Attempt to assign to a type which is not a variable", pos)
        }
        val (rightValue, lastToken) = parseValue(tokeniser)
        return InstrTokenHolder(AssignmentInstruction(dest.name, rightValue, pos), lastToken, pos)
    }

    fun parseFnArgs(): List<Symbol> {
        val initial = tokeniser.nextToken()
        if (initial != OpenParen) {
            tokeniser.pushBackToken(initial)
            return emptyList()
        }

        val result = ArrayList<Symbol>()

        val (token, pos) = tokeniser.nextTokenWithPosition()
        when (token) {
            is CloseParen -> return result
            is Symbol -> result.add(token)
            else -> throw ParseException("Token is not a symbol: ${token}", pos)
        }
        while (true) {
            val (newToken, newPos) = tokeniser.nextTokenWithPosition()
            when {
                newToken == CloseParen -> return result
                newToken != ListSeparator -> throw ParseException("Expected separator or end of list, got ${newToken}", newPos)
                else -> {
                    val symbolToken = tokeniser.nextTokenWithType<Symbol>()
                    result.add(symbolToken)
                }
            }
        }
    }

    fun processFunctionDefinition(engine: Engine, tokeniser: TokenGenerator, pos: Position): Instruction {
        if (!leftArgs.isEmpty()) {
            throw ParseException("Function definition with non-null left argument", pos)
        }

        val leftFnArgs = parseFnArgs()
        val name = tokeniser.nextTokenWithType<Symbol>()
        val rightFnArgs = parseFnArgs()

        // Ensure that no arguments are duplicated
        val argNames = HashSet<Symbol>()
        fun checkArgs(list: List<Symbol>) {
            list.forEach { element ->
                if (argNames.contains(element)) {
                    throw ParseException("Symbol ${element.symbolName} in multiple positions", pos)
                }
                argNames.add(element)
            }
        }
        checkArgs(leftFnArgs)
        checkArgs(rightFnArgs)

        // Read the opening brace
        tokeniser.nextTokenWithType<OpenFnDef>()
        // Parse like a normal function definition
        val instr = parseValueToplevel(tokeniser, CloseFnDef)

        val obj = UserFunction(leftFnArgs, rightFnArgs, instr)

        engine.registerFunction(name, obj)
        return LiteralSymbol(name, pos)
    }

    fun addLeftArg(instr: Instruction) {
        val (token, pos) = tokeniser.nextTokenWithPosition()
        val instrWithIndex = if (token == OpenBracket) {
            val indexInstr = parseValueToplevel(tokeniser, CloseBracket)
            ArrayIndex(instr, indexInstr, pos)
        } else {
            tokeniser.pushBackToken(token)
            instr
        }
        leftArgs.add(instrWithIndex)
    }

    while (true) {
        val (token, pos) = tokeniser.nextTokenWithPosition()
        if (listOf(CloseParen, EndOfFile, StatementSeparator, CloseFnDef, CloseBracket, ListSeparator).contains(token)) {
            return InstrTokenHolder(makeResultList(), token, pos)
        }

        when (token) {
            is Symbol -> {
                val fn = engine.getFunction(token)
                if (fn != null) {
                    return processFn(fn, pos)
                } else {
                    addLeftArg(VariableRef(token, pos))
                }
            }
            is OpenParen -> addLeftArg(parseValueToplevel(tokeniser, CloseParen))
            is OpenFnDef -> return processFn(parseFnDefinition(tokeniser, pos), pos)
            is ParsedLong -> leftArgs.add(LiteralInteger(token.value, pos))
            is ParsedDouble -> leftArgs.add(LiteralDouble(token.value, pos))
            is ParsedComplex -> leftArgs.add(LiteralComplex(token.value, pos))
            is ParsedCharacter -> leftArgs.add(LiteralCharacter(token.value, pos))
            is LeftArrow -> return processAssignment(tokeniser, pos)
            is FnDefSym -> leftArgs.add(processFunctionDefinition(engine, tokeniser, pos))
            is APLNullSym -> leftArgs.add(LiteralAPLNullValue(pos))
            is StringToken -> leftArgs.add(LiteralStringValue(token.value, pos))
            is QuotePrefix -> leftArgs.add(LiteralSymbol(tokeniser.nextTokenWithType(), pos))
            is LambdaToken -> leftArgs.add(processLambda(tokeniser, pos))
            is ApplyToken -> return processFn(parseApplyDefinition(tokeniser), pos)
            else -> throw UnexpectedToken(token, pos)
        }
    }
}

fun parseApplyDefinition(tokeniser: TokenGenerator): APLFunctionDescriptor {
    val (token, firstPos) = tokeniser.nextTokenWithPosition()
    val ref = when (token) {
        is Symbol -> VariableRef(token, firstPos)
        is OpenParen -> parseValueToplevel(tokeniser, CloseParen)
        else -> throw UnexpectedToken(token, firstPos)
    }
    return DynamicFunctionDescriptor(ref)
}

class EvalLambdaFnx(val fn: APLFunction, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        return LambdaValue(fn)
    }
}

fun processLambda(tokeniser: TokenGenerator, pos: Position): EvalLambdaFnx {
    val (token, pos2) = tokeniser.nextTokenWithPosition()
    return when (token) {
        is OpenFnDef -> {
            val fnDefinition = parseFnDefinition(tokeniser, pos)
            EvalLambdaFnx(fnDefinition.make(pos), pos)
        }
        else -> throw UnexpectedToken(token, pos2)
    }
}

fun parseFnDefinition(
    tokeniser: TokenGenerator,
    pos: Position,
    leftArgName: Symbol? = null,
    rightArgName: Symbol? = null
): DeclaredFunction {
    val engine = tokeniser.engine
    val instruction = parseValueToplevel(tokeniser, CloseFnDef)
    return DeclaredFunction(instruction, leftArgName ?: engine.internSymbol("⍺"), rightArgName ?: engine.internSymbol("⍵"), pos)
}

fun parseTwoArgOperatorArgument(
    tokeniser: TokenGenerator
): APLFunctionDescriptor {
    val (token, pos) = tokeniser.nextTokenWithPosition()
    return when (token) {
        is Symbol -> {
            val fn = tokeniser.engine.getFunction(token)
            if (fn == null) throw ParseException("Symbol is not a function", pos)
            parseOperator(fn, tokeniser)
        }
        is OpenFnDef -> {
            parseFnDefinition(tokeniser, pos)
        }
        else -> throw ParseException("Expected function, got: ${token}", pos)
    }
}

fun parseOperator(fn: APLFunctionDescriptor, tokeniser: TokenGenerator): APLFunctionDescriptor {
    var currentFn = fn
    var token: Token
    loop@ while (true) {
        val (readToken, pos) = tokeniser.nextTokenWithPosition()
        token = readToken
        if (token is Symbol) {
            val op = tokeniser.engine.getOperator(token) ?: break
            val axis = parseAxis(tokeniser)
            when (op) {
                is APLOperatorOneArg -> currentFn = op.combineFunction(currentFn, axis)
                is APLOperatorTwoArg -> currentFn = op.combineFunction(fn, parseTwoArgOperatorArgument(tokeniser), axis)
                else -> throw IllegalStateException("Operators must be either one or two arg")
            }

        } else {
            break
        }
    }
    tokeniser.pushBackToken(token)
    return currentFn
}

fun parseAxis(tokeniser: TokenGenerator): Instruction? {
    val token = tokeniser.nextToken()
    if (token != OpenBracket) {
        tokeniser.pushBackToken(token)
        return null
    }

    return parseValueToplevel(tokeniser, CloseBracket)
}
