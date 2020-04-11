package array

fun parseValueToplevel(engine: Engine, tokeniser: TokenGenerator, endToken: Token): Instruction {
    val statementList = ArrayList<Instruction>()
    while (true) {
        val (instr, lastToken) = parseList(engine, tokeniser)
        statementList.add(instr)
        if (lastToken == endToken) {
            assertx(!statementList.isEmpty())
            return if (statementList.size == 1) instr else InstructionList(statementList)
        } else if (lastToken != StatementSeparator) {
            throw UnexpectedToken(lastToken)
        }
    }
}

fun parseList(engine: Engine, tokeniser: TokenGenerator): Pair<Instruction, Token> {
    val statementList = ArrayList<Instruction>()
    while (true) {
        val (instr, lastToken) = parseValue(engine, tokeniser)
        if (lastToken == ListSeparator) {
            statementList.add(instr)
        } else {
            val list = if (statementList.isEmpty()) {
                instr
            } else {
                statementList.add(instr)
                ParsedAPLList(statementList)
            }
            return Pair(list, lastToken)
        }
    }
}

fun parseValue(engine: Engine, tokeniser: TokenGenerator): Pair<Instruction, Token> {
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

    fun processFn(fn: APLFunctionDescriptor, pos: Position): Pair<Instruction, Token> {
        val axis = parseAxis(engine, tokeniser)
        val parsedFn = parseOperator(fn, engine, tokeniser)
        val (rightValue, lastToken) = parseValue(engine, tokeniser)
        return if (leftArgs.isEmpty()) {
            Pair(FunctionCall1Arg(parsedFn.make(pos), rightValue, axis, pos), lastToken)
        } else {
            Pair(FunctionCall2Arg(parsedFn.make(pos), makeResultList(), rightValue, axis, pos), lastToken)
        }
    }

    fun processAssignment(engine: Engine, tokeniser: TokenGenerator, pos: Position): Pair<Instruction, Token> {
        // Ensure that the left argument to leftarrow is a single symbol
        unless(leftArgs.size == 1) {
            throw IncompatibleTypeParseException("Can only assign to a single variable")
        }
        val dest = leftArgs[0]
        if (dest !is VariableRef) {
            throw IncompatibleTypeParseException("Attempt to assign to a type which is not a variable")
        }
        val (rightValue, lastToken) = parseValue(engine, tokeniser)
        return Pair(AssignmentInstruction(dest.name, rightValue, pos), lastToken)
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
        val instr = parseValueToplevel(engine, tokeniser, CloseFnDef)

        val obj = UserFunction(leftFnArgs, rightFnArgs, instr)

        engine.registerFunction(name, obj)
        return LiteralSymbol(name, pos)
    }

    fun addLeftArg(instr: Instruction) {
        val (token, pos) = tokeniser.nextTokenWithPosition()
        val instrWithIndex = if (token == OpenBracket) {
            val indexInstr = parseValueToplevel(engine, tokeniser, CloseBracket)
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
            return Pair(makeResultList(), token)
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
            is OpenParen -> addLeftArg(parseValueToplevel(engine, tokeniser, CloseParen))
            is OpenFnDef -> return processFn(parseFnDefinition(engine, tokeniser, pos), pos)
            is ParsedLong -> leftArgs.add(LiteralInteger(token.value, pos))
            is ParsedDouble -> leftArgs.add(LiteralDouble(token.value, pos))
            is ParsedComplex -> leftArgs.add(LiteralComplex(token.value, pos))
            is ParsedCharacter -> leftArgs.add(LiteralCharacter(token.value, pos))
            is LeftArrow -> return processAssignment(engine, tokeniser, pos)
            is FnDefSym -> leftArgs.add(processFunctionDefinition(engine, tokeniser, pos))
            is APLNullSym -> leftArgs.add(LiteralAPLNullValue(pos))
            is StringToken -> leftArgs.add(LiteralStringValue(token.value, pos))
            is QuotePrefix -> leftArgs.add(LiteralSymbol(tokeniser.nextTokenWithType(), pos))
            is LambdaToken -> leftArgs.add(processLambda(engine, tokeniser, pos))
            is ApplyToken -> return processFn(parseApplyDefinition(engine, tokeniser), pos)
            else -> throw UnexpectedToken(token, pos)
        }
    }
}

fun parseApplyDefinition(engine: Engine, tokeniser: TokenGenerator): APLFunctionDescriptor {
    val (token, firstPos) = tokeniser.nextTokenWithPosition()
    val ref = when (token) {
        is Symbol -> VariableRef(token, firstPos)
        is OpenParen -> parseValueToplevel(engine, tokeniser, CloseParen)
        else -> throw UnexpectedToken(token)
    }
    return DynamicFunctionDescriptor(ref)
}

class EvalLambdaFnx(val fn: APLFunction, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        return LambdaValue(fn)
    }
}

fun processLambda(engine: Engine, tokeniser: TokenGenerator, pos: Position): EvalLambdaFnx {
    val token = tokeniser.nextToken()
    return when (token) {
        is OpenFnDef -> {
            val fnDefinition = parseFnDefinition(engine, tokeniser, pos)
            EvalLambdaFnx(fnDefinition.make(pos), pos)
        }
        else -> throw UnexpectedToken(token)
    }
}

fun parseFnDefinition(
    engine: Engine,
    tokeniser: TokenGenerator,
    pos: Position,
    leftArgName: Symbol? = null,
    rightArgName: Symbol? = null
): DeclaredFunction {
    val instruction = parseValueToplevel(engine, tokeniser, CloseFnDef)
    return DeclaredFunction(instruction, leftArgName ?: engine.internSymbol("⍺"), rightArgName ?: engine.internSymbol("⍵"), pos)
}

fun parseOperator(fn: APLFunctionDescriptor, engine: Engine, tokeniser: TokenGenerator): APLFunctionDescriptor {
    var currentFn = fn
    var token: Token
    while (true) {
        token = tokeniser.nextToken()
        if (token is Symbol) {
            val op = engine.getOperator(token) ?: break
            val axis = parseAxis(engine, tokeniser)
            currentFn = op.combineFunction(currentFn, axis)
        } else {
            break
        }
    }
    if (token != EndOfFile) {
        tokeniser.pushBackToken(token)
    }
    return currentFn
}

fun parseAxis(engine: Engine, tokeniser: TokenGenerator): Instruction? {
    val token = tokeniser.nextToken()
    if (token != OpenBracket) {
        tokeniser.pushBackToken(token)
        return null
    }

    return parseValueToplevel(engine, tokeniser, CloseBracket)
}
