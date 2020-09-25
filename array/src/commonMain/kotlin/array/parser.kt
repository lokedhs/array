package array

data class InstrTokenHolder(val instruction: Optional<Instruction>, val lastToken: Token, val pos: Position)

class EnvironmentBinding(val environment: Environment, val name: Symbol) {
    override fun toString(): String {
        return "EnvironmentBinding[environment=${environment}, name=${name}, key=${hashCode().toString(16)}]"
    }
}

class Environment {
    private val bindings = HashMap<Symbol, EnvironmentBinding>()
    private val localFunctions = HashMap<Symbol, UserFunction>()

    fun findBinding(sym: Symbol) = bindings[sym]

    fun bindLocal(sym: Symbol, binding: EnvironmentBinding? = null): EnvironmentBinding {
        val newBinding = binding ?: EnvironmentBinding(this, sym)
        bindings[sym] = newBinding
        return newBinding
    }

    fun localBindings(): Collection<EnvironmentBinding> {
        return bindings.values
    }

    fun registerLocalFunction(name: Symbol, userFn: UserFunction) {
        localFunctions[name] = userFn
    }

    fun findLocalFunction(name: Symbol): APLFunctionDescriptor? {
        return localFunctions[name]
    }

    companion object {
        fun nullEnvironment() = Environment()
    }
}

class APLParser(val tokeniser: TokenGenerator) {

    private var environments: MutableList<Environment> = ArrayList(listOf(tokeniser.engine.rootContext.environment))

    fun currentEnvironment() = environments.last()

    fun pushEnvironment(): Environment {
        val env = Environment()
        environments.add(env)
        return env
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun popEnvironment(): Environment {
        val env = environments.removeLast()
        assertx(environments.size > 0)
        return env
    }

    fun reinitialiseEnvironments(newEnvironments: MutableList<Environment>? = null): MutableList<Environment> {
        return environments.also {
            environments = newEnvironments ?: ArrayList(listOf(Environment.nullEnvironment()))
        }
    }

    inline fun <T> withEnvironment(fn: (Environment) -> T): T {
        val env = pushEnvironment()
        try {
            return fn(env)
        } finally {
            popEnvironment()
        }
    }

    inline fun <T> withNullEnvironment(fn: (Environment) -> T): T {
        val oldEnvironment = reinitialiseEnvironments()
        try {
            return fn(currentEnvironment())
        } finally {
            reinitialiseEnvironments(oldEnvironment)
        }
    }

    fun findEnvironmentBinding(sym: Symbol): EnvironmentBinding {
        environments.asReversed().forEach { env ->
            val binding = env.findBinding(sym)
            if (binding != null) {
                currentEnvironment().bindLocal(sym, binding)
                return binding
            }
        }
        return currentEnvironment().bindLocal(sym)
    }

    fun parseValueToplevel(endToken: Token): Instruction {
        val statementList = ArrayList<Instruction>()
        while (true) {
            val holder = parseList()
            holder.instruction.withValueIfExists { instr ->
                statementList.add(instr)
            }
            if (holder.lastToken == endToken) {
                return when (statementList.size) {
                    0 -> EmptyValueMarker(holder.pos)
                    1 -> statementList[0]
                    else -> InstructionList(statementList)
                }
            } else if (holder.lastToken != StatementSeparator && holder.lastToken != Newline) {
                throw UnexpectedToken(holder.lastToken, holder.pos)
            }
        }
    }

    private fun parseList(): InstrTokenHolder {
        val statementList = ArrayList<Instruction>()
        while (true) {
            val holder = parseValue()
            if (holder.lastToken == ListSeparator) {
                statementList.add(holder.instruction.withValue({ it }, { EmptyValueMarker(holder.pos) }))
            } else {
                if (!statementList.isEmpty()) {
                    statementList.add(holder.instruction.withValue({ it }, { EmptyValueMarker(holder.pos) }))
                } else {
                    holder.instruction.withValueIfExists { statementList.add(it) }
                }
                val list = when {
                    statementList.isEmpty() -> Optional.empty()
                    statementList.size == 1 -> Optional.make(statementList[0])
                    else -> Optional.make(ParsedAPLList(statementList))
                }
                return InstrTokenHolder(list, holder.lastToken, holder.pos)
            }
        }
    }

    private fun makeResultList(leftArgs: List<Instruction>): Optional<Instruction> {
        return when {
            leftArgs.isEmpty() -> Optional.empty()
            leftArgs.size == 1 -> Optional.make(LiteralScalarValue(leftArgs[0]))
            else -> Optional.make(Literal1DArray(leftArgs))
        }
    }

    private fun processFn(fn: APLFunctionDescriptor, pos: Position, leftArgs: List<Instruction>): InstrTokenHolder {
        val axis = parseAxis()
        val parsedFn = parseOperator(fn, pos)
        val (rightValue, lastToken) = parseValue()
        return rightValue.withValue({ instr ->
            if (leftArgs.isEmpty()) {
                InstrTokenHolder(Optional.make(FunctionCall1Arg(parsedFn.make(pos), instr, axis, pos)), lastToken, pos)
            } else {
                makeResultList(leftArgs).withValue({ leftArgs ->
                    InstrTokenHolder(Optional.make(FunctionCall2Arg(parsedFn.make(pos), leftArgs, instr, axis, pos)), lastToken, pos)
                }, { throw ParseException("No left-side argument", pos) })
            }
        }, { throw ParseException("Missing right-side argument to function", pos) })
    }

    private fun processAssignment(pos: Position, leftArgs: List<Instruction>): InstrTokenHolder {
        // Ensure that the left argument to leftarrow is a single symbol
        unless(leftArgs.size == 1) {
            throw IncompatibleTypeParseException("Can only assign to a single variable", pos)
        }
        val dest = leftArgs[0]
        if (dest !is VariableRef) {
            throw IncompatibleTypeParseException("Attempt to assign to a type which is not a variable", pos)
        }
        val (rightValue, lastToken, assignmentPos) = parseValue()
        return rightValue.withValue({ instr ->
            InstrTokenHolder(Optional.make(AssignmentInstruction(dest.binding, instr, pos)), lastToken, pos)
        }, { throw ParseException("No right-side value in assignment instruction", assignmentPos) })
    }

    private fun parseFnArgs(): List<Symbol> {
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

    data class DefinedUserFunction(val fn: UserFunction, val name: Symbol, val pos: Position)

    private fun processFunctionDefinition(pos: Position, leftArgs: List<Instruction>): Instruction {
        if (leftArgs.isNotEmpty()) {
            throw ParseException("Function definition with non-null left argument", pos)
        }
        val definedUserFunction = parseUserDefinedFn(pos)
        registerDefinedUserFunction(definedUserFunction)
        return LiteralSymbol(definedUserFunction.name, definedUserFunction.pos)
    }

    fun registerDefinedUserFunction(definedUserFunction: DefinedUserFunction) {
        tokeniser.engine.registerFunction(definedUserFunction.name, definedUserFunction.fn)
    }

    fun parseUserDefinedFn(pos: Position): DefinedUserFunction {
        val leftFnArgs = parseFnArgs()
        val name = tokeniser.nextTokenWithType<Symbol>()
        val rightFnArgs = parseFnArgs()

        // Ensure that no arguments are duplicated
        fun checkArgs(list: List<Symbol>) {
            val duplicated = list.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
            if (duplicated.isNotEmpty()) {
                throw ParseException("Symbols in multiple position: ${duplicated.joinToString(separator = " ") { it.symbolName }}", pos)
            }
        }
        checkArgs(leftFnArgs)
        checkArgs(rightFnArgs)

        // Read the opening brace
        tokeniser.nextTokenWithType<OpenFnDef>()
        // Parse like a normal function definition
        withEnvironment {
            val leftFnArgs1 = leftFnArgs.map(this::findEnvironmentBinding)
            val rightFnArgs1 = rightFnArgs.map(this::findEnvironmentBinding)
            val inProcessUserFunction = UserFunction(leftFnArgs1, rightFnArgs1, DummyInstr(pos), currentEnvironment())
            currentEnvironment().registerLocalFunction(name, inProcessUserFunction)
            val instr = parseValueToplevel(CloseFnDef)
            inProcessUserFunction.instr = instr
            return DefinedUserFunction(inProcessUserFunction, name, pos)
        }
    }

    private fun lookupFunction(name: Symbol): APLFunctionDescriptor? {
        environments.asReversed().forEach { env ->
            val function = env.findLocalFunction(name)
            if (function != null) {
                return function
            }
        }
        return tokeniser.engine.getFunction(name)
    }

    private fun parseValue(): InstrTokenHolder {
        val leftArgs = ArrayList<Instruction>()

        fun addLeftArg(instr: Instruction) {
            val (token, pos) = tokeniser.nextTokenWithPosition()
            val instrWithIndex = if (token == OpenBracket) {
                val indexInstr = parseValueToplevel(CloseBracket)
                ArrayIndex(instr, indexInstr, pos)
            } else {
                tokeniser.pushBackToken(token)
                instr
            }
            leftArgs.add(instrWithIndex)
        }

        while (true) {
            val (token, pos) = tokeniser.nextTokenWithPosition()
            if (listOf(CloseParen, EndOfFile, StatementSeparator, CloseFnDef, CloseBracket, ListSeparator, Newline).contains(token)) {
                return InstrTokenHolder(makeResultList(leftArgs), token, pos)
            }

            when (token) {
                is Symbol -> {
                    val customSyntax = tokeniser.engine.syntaxRulesForSymbol(token)
                    if (customSyntax != null) {
                        addLeftArg(processCustomSyntax(this, customSyntax))
                    } else {
                        val fn = lookupFunction(token)
                        if (fn != null) {
                            return processFn(fn, pos, leftArgs)
                        } else {
                            addLeftArg(makeVariableRef(token, pos))
                        }
                    }
                }
                is OpenParen -> addLeftArg(parseValueToplevel(CloseParen))
                is OpenFnDef -> return processFn(parseFnDefinition(), pos, leftArgs)
                is ParsedLong -> leftArgs.add(LiteralInteger(token.value, pos))
                is ParsedDouble -> leftArgs.add(LiteralDouble(token.value, pos))
                is ParsedComplex -> leftArgs.add(LiteralComplex(token.value, pos))
                is ParsedCharacter -> leftArgs.add(LiteralCharacter(token.value, pos))
                is LeftArrow -> return processAssignment(pos, leftArgs)
                is FnDefSym -> leftArgs.add(processFunctionDefinition(pos, leftArgs))
                is APLNullSym -> leftArgs.add(LiteralAPLNullValue(pos))
                is StringToken -> leftArgs.add(LiteralStringValue(token.value, pos))
                is QuotePrefix -> leftArgs.add(LiteralSymbol(tokeniser.nextTokenWithType(), pos))
                is LambdaToken -> leftArgs.add(processLambda(pos))
                is ApplyToken -> return processFn(parseApplyDefinition(), pos, leftArgs)
                is NamespaceToken -> processNamespace()
                is ImportToken -> processImport()
                is ExportToken -> processExport()
                is DefsyntaxToken -> leftArgs.add(processDefsyntax(this, pos))
                is IncludeToken -> leftArgs.add(processInclude(pos))
                is LocalToken -> processLocal()
                else -> throw UnexpectedToken(token, pos)
            }
        }
    }

    private fun makeVariableRef(symbol: Symbol, pos: Position): Instruction {
        if (tokeniser.engine.isSelfEvaluatingSymbol(symbol)) {
            return LiteralSymbol(symbol, pos)
        }
        return VariableRef(symbol, findEnvironmentBinding(symbol), pos)
    }

    private fun processInclude(pos: Position): Instruction {
        val engine = tokeniser.engine
        tokeniser.nextTokenWithType<OpenParen>()
        val filename = tokeniser.nextTokenWithType<StringToken>()
        tokeniser.nextTokenWithType<CloseParen>()
        val resolved = engine.resolveLibraryFile(filename.value) ?: filename.value
        try {
            val innerParser = APLParser(TokenGenerator(engine, FileSourceLocation(resolved)))
            engine.withSavedNamespace {
                withEnvironment {
                    return innerParser.parseValueToplevel(EndOfFile)
                }
            }
        } catch (e: MPFileException) {
            throw ParseException("Error loading file: ${e.message}", pos)
        }
    }

    private fun processNamespace() {
        tokeniser.nextTokenWithType<OpenParen>()
        val namespaceName = tokeniser.nextTokenWithType<StringToken>()
        tokeniser.nextTokenWithType<CloseParen>()
        val namespace = tokeniser.engine.makeNamespace(namespaceName.value)
        tokeniser.engine.currentNamespace = namespace
    }

    private fun processImport() {
        tokeniser.nextTokenWithType<OpenParen>()
        val namespaceName = tokeniser.nextTokenWithType<StringToken>()
        tokeniser.nextTokenWithType<CloseParen>()
        val namespace = tokeniser.engine.makeNamespace(namespaceName.value)
        tokeniser.engine.currentNamespace.addImport(namespace)
    }

    private fun parseSymbolList(fn: (Symbol) -> Unit) {
        tokeniser.nextTokenWithType<OpenParen>()
        while (true) {
            val (token, pos) = tokeniser.nextTokenWithPosition()
            when (token) {
                is Symbol -> fn(token)
                is CloseParen -> break
                else -> throw UnexpectedToken(token, pos)
            }
        }
    }

    private fun processExport() {
        parseSymbolList { sym ->
            exportSymbolIfInterned(sym)
        }
    }


    private fun processLocal() {
        parseSymbolList { sym ->
            currentEnvironment().bindLocal(sym)
        }
    }

    private fun exportSymbolIfInterned(symbol: Symbol) {
        symbol.namespace.exportIfInterned(symbol)
    }

    private fun parseApplyDefinition(): APLFunctionDescriptor {
        val (token, firstPos) = tokeniser.nextTokenWithPosition()
        val ref = when (token) {
            is Symbol -> makeVariableRef(token, firstPos)
            is OpenParen -> parseValueToplevel(CloseParen)
            else -> throw UnexpectedToken(token, firstPos)
        }
        return DynamicFunctionDescriptor(ref)
    }

    class EvalLambdaFnx(val fn: APLFunction, pos: Position) : Instruction(pos) {
        override fun evalWithContext(context: RuntimeContext): APLValue {
            return LambdaValue(fn, context)
        }
    }

    private fun processLambda(pos: Position): EvalLambdaFnx {
        val (token, pos2) = tokeniser.nextTokenWithPosition()
        return when (token) {
            is OpenFnDef -> {
                val fnDefinition = parseFnDefinition()
                EvalLambdaFnx(fnDefinition.make(pos), pos)
            }
            else -> throw UnexpectedToken(token, pos2)
        }
    }

    fun parseFnDefinition(
        leftArgName: Symbol? = null,
        rightArgName: Symbol? = null,
        endToken: Token = CloseFnDef,
        allocateEnvironment: Boolean = true
    ): APLFunctionDescriptor {
        return if (allocateEnvironment) {
            val engine = tokeniser.engine
            withEnvironment {
                val leftBinding = currentEnvironment().bindLocal(leftArgName ?: engine.internSymbol("⍺", engine.currentNamespace))
                val rightBinding = currentEnvironment().bindLocal(rightArgName ?: engine.internSymbol("⍵", engine.currentNamespace))
                val instruction = parseValueToplevel(endToken)
                DeclaredFunction(instruction, leftBinding, rightBinding, currentEnvironment())
            }
        } else {
            val instruction = parseValueToplevel(endToken)
            DeclaredNonBoundFunction(instruction, currentEnvironment())
        }
    }

    private fun parseTwoArgOperatorArgument(): Pair<APLFunctionDescriptor, Position> {
        val (token, pos) = tokeniser.nextTokenWithPosition()
        return when (token) {
            is Symbol -> {
                val fn = tokeniser.engine.getFunction(token) ?: throw ParseException("Symbol is not a function", pos)
                Pair(parseOperator(fn, pos), pos)
            }
            is OpenFnDef -> {
                Pair(parseFnDefinition(), pos)
            }
            else -> throw ParseException("Expected function, got: ${token}", pos)
        }
    }

    private fun parseOperator(fn: APLFunctionDescriptor, fnPos: Position): APLFunctionDescriptor {
        var currentFn = fn
        var token: Token
        loop@ while (true) {
            val (readToken, opPos) = tokeniser.nextTokenWithPosition()
            token = readToken
            if (token is Symbol) {
                val op = tokeniser.engine.getOperator(token) ?: break
                val axis = parseAxis()
                when (op) {
                    is APLOperatorOneArg -> {
                        currentFn = op.combineFunction(currentFn, axis, fnPos)
                    }
                    is APLOperatorTwoArg -> {
                        parseTwoArgOperatorArgument().let { (fn2, fn2Pos) ->
                            currentFn = op.combineFunction(fn, fn2, axis, opPos, fnPos, fn2Pos)
                        }
                    }
                    else -> throw IllegalStateException("Operators must be either one or two arg")
                }

            } else {
                break
            }
        }
        tokeniser.pushBackToken(token)
        return currentFn
    }

    private fun parseAxis(): Instruction? {
        val token = tokeniser.nextToken()
        if (token != OpenBracket) {
            tokeniser.pushBackToken(token)
            return null
        }

        return parseValueToplevel(CloseBracket)
    }
}
