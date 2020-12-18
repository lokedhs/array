package array

import array.syntax.processCustomSyntax
import array.syntax.processDefsyntax
import array.syntax.processDefsyntaxSub

//data class InstrTokenHolder(val instruction: Optional<Instruction>, val lastToken: Token, val pos: Position)
sealed class ParseResultHolder(val lastToken: Token, val pos: Position) {
    class InstrParseResult(val instr: Instruction, lastToken: Token, pos: Position) : ParseResultHolder(lastToken, pos)
    class FnParseResult(val fn: APLFunction, lastToken: Token, pos: Position) : ParseResultHolder(lastToken, pos)
    class EmptyParseResult(lastToken: Token, pos: Position) : ParseResultHolder(lastToken, pos)
}

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

    private fun findEnvironmentBinding(sym: Symbol): EnvironmentBinding {
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
        return when (val result = parseExprToplevel(endToken)) {
            is ParseResultHolder.EmptyParseResult -> EmptyValueMarker(result.pos)
            is ParseResultHolder.InstrParseResult -> result.instr
            is ParseResultHolder.FnParseResult -> throw ParseException("Function expression not allowed", result.pos)
        }
    }

    fun parseExprToplevel(endToken: Token): ParseResultHolder {
        val firstExpr = parseList()
        if (firstExpr.lastToken == endToken) {
            return firstExpr
        }

        fun throwIfInvalidToken(holder: ParseResultHolder) {
            if (holder.lastToken != StatementSeparator && holder.lastToken != Newline) {
                throw UnexpectedToken(holder.lastToken, holder.pos)
            }
        }

        throwIfInvalidToken(firstExpr)

        val statementList = ArrayList<Instruction>()

        fun addInstr(holder: ParseResultHolder) {
            if (holder is ParseResultHolder.InstrParseResult) {
                statementList.add(holder.instr)
            } else if (holder !is ParseResultHolder.EmptyParseResult) {
                throw IllegalContextForFunction(holder.pos)
            }
        }

        addInstr(firstExpr)

        while (true) {
            val holder = parseList()
            addInstr(holder)
            if (holder.lastToken == endToken) {
                return when (statementList.size) {
                    0 -> ParseResultHolder.InstrParseResult(EmptyValueMarker(holder.pos), holder.lastToken, holder.pos)
                    1 -> ParseResultHolder.InstrParseResult(statementList[0], holder.lastToken, holder.pos)
                    else -> ParseResultHolder.InstrParseResult(InstructionList(statementList), holder.lastToken, holder.pos)
                }
            } else {
                throwIfInvalidToken(holder)
            }
        }
    }

    private fun parseList(): ParseResultHolder {
        val firstValue = parseValue()
        if (firstValue.lastToken == ListSeparator) {
            if (firstValue is ParseResultHolder.FnParseResult) {
                throw ParseException("Function expressions can't be part of a list", firstValue.pos)
            }

            fun mkInstr(v: ParseResultHolder): Instruction {
                return when (v) {
                    is ParseResultHolder.EmptyParseResult -> EmptyValueMarker(v.pos)
                    is ParseResultHolder.InstrParseResult -> v.instr
                    is ParseResultHolder.FnParseResult -> throw ParseException("Function expressions can't be part of a list", v.pos)
                }
            }

            val statementList = ArrayList<Instruction>()
            statementList.add(mkInstr(firstValue))
            while (true) {
                val holder = parseValue()
                statementList.add(mkInstr(holder))
                if (holder.lastToken != ListSeparator) {
                    return ParseResultHolder.InstrParseResult(ParsedAPLList(statementList), holder.lastToken, firstValue.pos)
                }
            }
        } else {
            return firstValue
        }
    }

    private fun makeResultList(leftArgs: List<Instruction>): Instruction? {
        return when {
            leftArgs.isEmpty() -> null
            leftArgs.size == 1 -> LiteralScalarValue(leftArgs[0])
            else -> Literal1DArray(leftArgs)
        }
    }

    private fun processFn(fn: APLFunction, leftArgs: List<Instruction>): ParseResultHolder {
        val axis = parseAxis()
        val parsedFn = parseOperator(fn)
        return when (val holder = parseValue()) {
            is ParseResultHolder.EmptyParseResult -> {
                if (leftArgs.isNotEmpty()) {
                    throw ParseException("Missing right argument", fn.pos)
                }
                ParseResultHolder.FnParseResult(parsedFn, holder.lastToken, holder.pos)
            }
            is ParseResultHolder.InstrParseResult -> {
                if (leftArgs.isEmpty()) {
                    ParseResultHolder.InstrParseResult(
                        FunctionCall1Arg(parsedFn, holder.instr, axis, fn.pos),
                        holder.lastToken,
                        holder.pos)
                } else {
                    val leftArgsChecked = makeResultList(leftArgs) ?: throw ParseException("Left args is empty", holder.pos)
                    ParseResultHolder.InstrParseResult(
                        FunctionCall2Arg(parsedFn, leftArgsChecked, holder.instr, axis, fn.pos),
                        holder.lastToken,
                        holder.pos)
                }
            }
            is ParseResultHolder.FnParseResult -> throw IllegalContextForFunction(holder.pos)
        }
    }

    private fun processAssignment(pos: Position, leftArgs: List<Instruction>): ParseResultHolder.InstrParseResult {
        // Ensure that the left argument to leftarrow is a single symbol
        unless(leftArgs.size == 1) {
            throw IncompatibleTypeParseException("Can only assign to a single variable", pos)
        }
        val dest = leftArgs[0]
        if (dest !is VariableRef) {
            throw IncompatibleTypeParseException("Attempt to assign to a type which is not a variable", pos)
        }
        return when (val holder = parseValue()) {
            is ParseResultHolder.InstrParseResult -> ParseResultHolder.InstrParseResult(
                AssignmentInstruction(
                    dest.binding,
                    holder.instr,
                    pos), holder.lastToken, pos)
            is ParseResultHolder.FnParseResult -> throw IllegalContextForFunction(holder.pos)
            is ParseResultHolder.EmptyParseResult -> throw ParseException("No right-side value in assignment instruction", pos)
        }
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

    private fun registerDefinedUserFunction(definedUserFunction: DefinedUserFunction) {
        val engine = tokeniser.engine
        when (val oldDefinition = engine.getFunction(definedUserFunction.name)) {
            null -> engine.registerFunction(definedUserFunction.name, definedUserFunction.fn)
            is UserFunction -> oldDefinition.replaceFunctionDefinition(definedUserFunction.fn)
            else -> throw InvalidFunctionRedefinition(definedUserFunction.name, definedUserFunction.pos)
        }
        tokeniser.engine.registerFunction(definedUserFunction.name, definedUserFunction.fn)
    }

    private fun parseUserDefinedFn(pos: Position): DefinedUserFunction {
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
            val leftFnArgs1 = leftFnArgs.map { sym -> currentEnvironment().bindLocal(sym) }
            val rightFnArgs1 = rightFnArgs.map { sym -> currentEnvironment().bindLocal(sym) }
            val inProcessUserFunction = UserFunction(name, leftFnArgs1, rightFnArgs1, DummyInstr(pos), currentEnvironment())
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

    fun parseValue(): ParseResultHolder {
        val leftArgs = ArrayList<Instruction>()

        fun processIndex(pos: Position) {
            if (leftArgs.isEmpty()) {
                throw ParseException("Index referencing without argument", pos)
            }
            val element = leftArgs.removeLast()
            val index = parseValueToplevel(CloseBracket)
            leftArgs.add(ArrayIndex(element, index, pos))
        }

        while (true) {
            val (token, pos) = tokeniser.nextTokenWithPosition()
            if (listOf(CloseParen, EndOfFile, StatementSeparator, CloseFnDef, CloseBracket, ListSeparator, Newline).contains(token)) {
                val resultList = makeResultList(leftArgs)
                return if (resultList == null) {
                    ParseResultHolder.EmptyParseResult(token, pos)
                } else {
                    ParseResultHolder.InstrParseResult(resultList, token, pos)
                }
            }

            when (token) {
                is Symbol -> {
                    val customSyntax = tokeniser.engine.syntaxRulesForSymbol(token)
                    if (customSyntax != null) {
                        leftArgs.add(processCustomSyntax(this, customSyntax))
                    } else {
                        val fn = lookupFunction(token)
                        if (fn != null) {
                            return processFn(fn.make(pos), leftArgs)
                        } else {
                            leftArgs.add(makeVariableRef(token, pos))
                        }
                    }
                }
                is OpenParen -> when (val expr = parseExprToplevel(CloseParen)) {
                    is ParseResultHolder.InstrParseResult -> leftArgs.add(expr.instr)
                    is ParseResultHolder.FnParseResult -> return processFn(expr.fn, leftArgs)
                    is ParseResultHolder.EmptyParseResult -> throw ParseException("Empty expression", pos)
                }
                is OpenFnDef -> return processFn(parseFnDefinition().make(pos), leftArgs)
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
                is ApplyToken -> return processFn(parseApplyDefinition().make(pos), leftArgs)
                is NamespaceToken -> processNamespace()
                is ImportToken -> processImport()
                is ExportToken -> processExport()
                is DefsyntaxSubToken -> processDefsyntaxSub(this, pos)
                is DefsyntaxToken -> leftArgs.add(processDefsyntax(this, pos))
                is IncludeToken -> leftArgs.add(processInclude(pos))
                is LocalToken -> processLocal()
                is DeclareToken -> processDeclare()
                is OpenBracket -> processIndex(pos)
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

    private fun processSingleCharDeclaration() {
        val (stringToken, stringPos) = tokeniser.nextTokenAndPosWithType<StringToken>()
        val codepointList = stringToken.value.asCodepointList()
        if (codepointList.size != 1) {
            throw IllegalDeclaration("singleChar declaration argument must be a string of length 1", stringPos)
        }
        tokeniser.registerSingleCharFunction(stringToken.value)
    }

    private fun processDeclare() {
        val engine = tokeniser.engine
        tokeniser.nextTokenWithType<OpenParen>()
        val (sym, symPosition) = tokeniser.nextTokenAndPosWithType<Symbol>()
        unless(sym.namespace === engine.keywordNamespace) {
            throw IllegalDeclaration("Declaration name must be a keyword", symPosition)
        }
        when (sym.symbolName) {
            "singleChar" -> processSingleCharDeclaration()
            else -> throw IllegalDeclaration("Unknown declaration name: ${sym.nameWithNamespace()}")
        }
        tokeniser.nextTokenWithType<CloseParen>()
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
            is Symbol -> {
                val fnDefinition = lookupFunction(token) ?: throw ParseException("Symbol is not a valid function", pos)
                EvalLambdaFnx(fnDefinition.make(pos), pos)
            }
            is OpenParen -> {
                val holder = parseExprToplevel(CloseParen)
                if (holder !is ParseResultHolder.FnParseResult) {
                    throw ParseException("Argument is not a function", pos)
                }
                EvalLambdaFnx(holder.fn, pos)
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
                DeclaredFunction("<unnamed>", instruction, leftBinding, rightBinding, currentEnvironment())
            }
        } else {
            val instruction = parseValueToplevel(endToken)
            DeclaredNonBoundFunction(instruction, currentEnvironment())
        }
    }

    private fun parseOperator(fn: APLFunction): APLFunction {
        var currentFn = fn
        var token: Token
        loop@ while (true) {
            val (readToken, opPos) = tokeniser.nextTokenWithPosition()
            token = readToken
            when (token) {
                is Symbol -> {
                    val op = tokeniser.engine.getOperator(token) ?: break
                    currentFn = op.parseAndCombineFunctions(this, currentFn, opPos)
                }
                else -> break
            }
        }
        tokeniser.pushBackToken(token)
        return currentFn
    }

    fun parseAxis(): Instruction? {
        val token = tokeniser.nextToken()
        if (token != OpenBracket) {
            tokeniser.pushBackToken(token)
            return null
        }

        return parseValueToplevel(CloseBracket)
    }
}
