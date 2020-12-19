package array

import array.builtins.*
import array.syntax.CustomSyntax

interface APLFunctionDescriptor {
    fun make(pos: Position): APLFunction
}

abstract class APLFunction(val pos: Position) {
    open fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue = throwAPLException(Unimplemented1ArgException(pos))
    open fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue =
        throwAPLException(Unimplemented2ArgException(pos))

    open fun identityValue(): APLValue = throwAPLException(APLIncompatibleDomainsException("Function does not have an identity value", pos))
    open fun deriveBitwise(): APLFunctionDescriptor? = null
}

abstract class NoAxisAPLFunction(pos: Position) : APLFunction(pos) {
    override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        if (axis != null) {
            throwAPLException(AxisNotSupported(pos))
        }
        return eval1Arg(context, a)
    }

    open fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue = throwAPLException(Unimplemented1ArgException(pos))


    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        if (axis != null) {
            throwAPLException(AxisNotSupported(pos))
        }
        return eval2Arg(context, a, b)
    }

    open fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue = throwAPLException(Unimplemented2ArgException(pos))
}

/**
 * A function that is declared directly in a { ... } expression.
 */
class DeclaredFunction(
    val name: String,
    val instruction: Instruction,
    val leftArgName: EnvironmentBinding,
    val rightArgName: EnvironmentBinding,
    val env: Environment
) : APLFunctionDescriptor {
    inner class DeclaredFunctionImpl(pos: Position) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            val localContext = context.link(env)
            localContext.setVar(rightArgName, a)
            return localContext.withCallStackElement("declaredFunction1", pos) {
                instruction.evalWithContext(localContext)
            }
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            val localContext = context.link(env)
            localContext.setVar(leftArgName, a)
            localContext.setVar(rightArgName, b)
            return localContext.withCallStackElement("declaredFunction2", pos) {
                instruction.evalWithContext(localContext)
            }
        }
    }

    override fun make(pos: Position) = DeclaredFunctionImpl(pos)
}

/**
 * A special declared function which ignores its arguments. Its primary use is inside defsyntax rules
 * where the functions are only used to provide code structure and not directly called by the user.
 */
class DeclaredNonBoundFunction(val instruction: Instruction, val env: Environment) : APLFunctionDescriptor {
    inner class DeclaredNonBoundFunctionImpl(pos: Position) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            return instruction.evalWithContext(context)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            return instruction.evalWithContext(context)
        }
    }

    override fun make(pos: Position) = DeclaredNonBoundFunctionImpl(pos)
}

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

interface APLOperatorTwoArg : APLOperator {
    override fun parseAndCombineFunctions(aplParser: APLParser, currentFn: APLFunction, opPos: Position): APLFunction {
        val axis = aplParser.parseAxis()
        val (token, pos) = aplParser.tokeniser.nextTokenWithPosition()
        val rightFn = when (token) {
            is Symbol -> {
                val fn = aplParser.tokeniser.engine.getFunction(token) ?: throw ParseException("Symbol is not a function", pos)
                fn.make(pos)
            }
            is OpenFnDef -> {
                aplParser.parseFnDefinition().make(pos)
            }
            is OpenParen -> {
                val holder = aplParser.parseExprToplevel(CloseParen)
                if (holder !is ParseResultHolder.FnParseResult) {
                    throw ParseException("Expected function", pos)
                }
                holder.fn
            }
            else -> throw ParseException("Expected function, got: ${token}", pos)
        }
        return combineFunction(currentFn, rightFn, axis, opPos).make(opPos)
    }

    fun combineFunction(
        fn1: APLFunction,
        fn2: APLFunction,
        operatorAxis: Instruction?,
        opPos: Position): APLFunctionDescriptor
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

private const val CORE_NAMESPACE_NAME = "kap"
private const val KEYWORD_NAMESPACE_NAME = "keyword"
private const val DEFAULT_NAMESPACE_NAME = "default"

val threadLocalEngineRef = makeMPThreadLocal(Engine::class)

class CallStackElement(val name: String, val pos: Position?) {
    fun copy() = CallStackElement(name, pos)
}

class Engine {
    private val functions = HashMap<Symbol, APLFunctionDescriptor>()
    private val operators = HashMap<Symbol, APLOperator>()
    private val functionDefinitionListeners = ArrayList<FunctionDefinitionListener>()
    private val functionAliases = HashMap<Symbol, Symbol>()
    private val namespaces = HashMap<String, Namespace>()
    private val customSyntaxSubEntries = HashMap<Symbol, CustomSyntax>()
    private val customSyntaxEntries = HashMap<Symbol, CustomSyntax>()
    private val librarySearchPaths = ArrayList<String>()
    private val modules = ArrayList<KapModule>()
    private val exportedSingleCharFunctions = HashSet<String>()

    val rootContext = RuntimeContext(this, Environment())
    var standardOutput: CharacterOutput = NullCharacterOutput()
    val coreNamespace = makeNamespace(CORE_NAMESPACE_NAME, overrideDefaultImport = true)
    val keywordNamespace = makeNamespace(KEYWORD_NAMESPACE_NAME, overrideDefaultImport = true)
    val initialNamespace = makeNamespace(DEFAULT_NAMESPACE_NAME)
    var currentNamespace = initialNamespace
    val callStack = ArrayList<CallStackElement>()

    init {
        // Intern the names of all the types in the core namespace.
        // This ensures that code that refers to the unqualified versions of the names pick up the correct symbol.
        APLValueType.values().forEach { aplValueType ->
            coreNamespace.internAndExport(aplValueType.typeName)
        }

        // core functions
        registerNativeFunction("+", AddAPLFunction())
        registerNativeFunction("-", SubAPLFunction())
        registerNativeFunction("×", MulAPLFunction())
        registerNativeFunction("÷", DivAPLFunction())
        registerNativeFunction("⋆", PowerAPLFunction())
        registerNativeFunction("⍟", LogAPLFunction())
        registerNativeFunction("⍳", IotaAPLFunction())
        registerNativeFunction("⍴", RhoAPLFunction())
        registerNativeFunction("⊢", IdentityAPLFunction())
        registerNativeFunction("⊣", HideAPLFunction())
        registerNativeFunction("=", EqualsAPLFunction())
        registerNativeFunction("≠", NotEqualsAPLFunction())
        registerNativeFunction("<", LessThanAPLFunction())
        registerNativeFunction(">", GreaterThanAPLFunction())
        registerNativeFunction("≤", LessThanEqualAPLFunction())
        registerNativeFunction("≥", GreaterThanEqualAPLFunction())
        registerNativeFunction("⌷", AccessFromIndexAPLFunction())
        registerNativeFunction("⊂", EncloseAPLFunction())
        registerNativeFunction("⊃", DiscloseAPLFunction())
        registerNativeFunction("∧", AndAPLFunction())
        registerNativeFunction("∨", OrAPLFunction())
        registerNativeFunction(",", ConcatenateAPLFunctionLastAxis())
        registerNativeFunction("⍪", ConcatenateAPLFunctionFirstAxis())
        registerNativeFunction("↑", TakeAPLFunction())
        registerNativeFunction("?", RandomAPLFunction())
        registerNativeFunction("⌽", RotateHorizFunction())
        registerNativeFunction("⊖", RotateVertFunction())
        registerNativeFunction("↓", DropAPLFunction())
        registerNativeFunction("⍉", TransposeFunction())
        registerNativeFunction("⌊", MinAPLFunction())
        registerNativeFunction("⌈", MaxAPLFunction())
        registerNativeFunction("|", ModAPLFunction())
        registerNativeFunction("∘", NullFunction())
        registerNativeFunction("≡", CompareFunction())
        registerNativeFunction("≢", CompareNotEqualFunction())
        registerNativeFunction("∊", MemberFunction())
        registerNativeFunction("⍋", GradeUpFunction())
        registerNativeFunction("⍒", GradeDownFunction())
        registerNativeFunction("⍷", FindFunction())
        registerNativeFunction("/", SelectElementsLastAxisFunction())
        registerNativeFunction("⌿", SelectElementsFirstAxisFunction())
        registerNativeFunction("∼", NotAPLFunction())
        registerNativeFunction("⍕", FormatAPLFunction())
        registerNativeFunction("⍸", WhereAPLFunction())
        registerNativeFunction("∪", UniqueFunction())
        registerNativeFunction("⍲", NandAPLFunction())
        registerNativeFunction("⍱", NorAPLFunction())

        // hash tables
        registerNativeFunction("map", MapAPLFunction())
        registerNativeFunction("mapGet", MapGetAPLFunction())
        registerNativeFunction("mapPut", MapPutAPLFunction())
        registerNativeFunction("mapRemove", MapRemoveAPLFunction())

        // io functions
        registerNativeFunction("print", PrintAPLFunction(), "io")
        registerNativeFunction("readCsvFile", ReadCSVFunction())
        registerNativeFunction("load", LoadFunction())
        registerNativeFunction("httpRequest", HttpRequestFunction(), "io")
        registerNativeFunction("httpPost", HttpPostFunction(), "io")
        registerNativeFunction("readdir", ReaddirFunction())

        // misc functions
        registerNativeFunction("sleep", SleepFunction(), "time")
        registerNativeFunction("→", ThrowFunction())
        registerNativeOperator("catch", CatchOperator())
        registerNativeFunction("labels", LabelsFunction())
        registerNativeFunction("timeMillis", TimeMillisFunction(), "time")

        // maths
        registerNativeFunction("sin", SinAPLFunction(), "math")
        registerNativeFunction("cos", CosAPLFunction(), "math")
        registerNativeFunction("tan", TanAPLFunction(), "math")
        registerNativeFunction("asin", AsinAPLFunction(), "math")
        registerNativeFunction("acos", AcosAPLFunction(), "math")
        registerNativeFunction("atan", AtanAPLFunction(), "math")

        // metafunctions
        registerNativeFunction("typeof", TypeofFunction())
        registerNativeFunction("isLocallyBound", IsLocallyBoundFunction())
        registerNativeFunction("comp", CompFunction())

        // operators
        registerNativeOperator("¨", ForEachOp())
        registerNativeOperator("/", ReduceOpLastAxis())
        registerNativeOperator("⌿", ReduceOpFirstAxis())
        registerNativeOperator("⌺", OuterJoinOp())
        registerNativeOperator(".", OuterInnerJoinOp())
        registerNativeOperator("⍨", CommuteOp())
        registerNativeOperator("⍣", PowerAPLOperator())
        registerNativeOperator("\\", ScanLastAxisOp())
        registerNativeOperator("⍀", ScanFirstAxisOp())
        registerNativeOperator("⍤", RankOperator())
        registerNativeOperator("∵", BitwiseOp())

        // function aliases
        functionAliases[coreNamespace.internAndExport("*")] = coreNamespace.internAndExport("⋆")
        functionAliases[coreNamespace.internAndExport("~")] = coreNamespace.internAndExport("∼")

        platformInit(this)

        addModule(UnicodeModule())
    }

    fun addModule(module: KapModule) {
        module.init(this)
        modules.add(module)
    }

    fun addLibrarySearchPath(path: String) {
        val fixedPath = PathUtils.cleanupPathName(path)
        if (!librarySearchPaths.contains(fixedPath)) {
            librarySearchPaths.add(fixedPath)
        }
    }

    fun resolveLibraryFile(requestedFile: String): String? {
        if (requestedFile.isEmpty()) {
            return null
        }
        if (PathUtils.isAbsolutePath(requestedFile)) {
            return null
        }
        librarySearchPaths.forEach { path ->
            val name = "${path}/${requestedFile}"
            if (fileExists(name)) {
                return name
            }
        }
        return null
    }

    fun addFunctionDefinitionListener(listener: FunctionDefinitionListener) {
        functionDefinitionListeners.add(listener)
    }

    fun removeFunctionDefinitionListener(listener: FunctionDefinitionListener) {
        functionDefinitionListeners.remove(listener)
    }

    fun registerFunction(name: Symbol, fn: APLFunctionDescriptor) {
        functions[name] = fn
        functionDefinitionListeners.forEach { it.functionDefined(name, fn) }
    }

    private fun registerNativeFunction(name: String, fn: APLFunctionDescriptor, namespaceName: String? = null) {
        val namespace = if (namespaceName == null) coreNamespace else makeNamespace(namespaceName)
        val sym = namespace.internAndExport(name)
        registerFunction(sym, fn)
    }

    fun registerOperator(name: Symbol, fn: APLOperator) {
        operators[name] = fn
        functionDefinitionListeners.forEach { it.operatorDefined(name, fn) }
    }

    private fun registerNativeOperator(name: String, fn: APLOperator) {
        val sym = coreNamespace.internAndExport(name)
        registerOperator(sym, fn)
    }

    fun getUserDefinedFunctions(): Map<Symbol, UserFunction> {
        val res = HashMap<Symbol, UserFunction>()
        functions.forEach {
            val v = it.value
            if (v is UserFunction) {
                res[it.key] = v
            }
        }
        return res
    }

    fun getFunction(name: Symbol) = functions[resolveAlias(name)]
    fun getOperator(name: Symbol) = operators[resolveAlias(name)]

    fun parseAndEval(source: SourceLocation, newContext: Boolean): APLValue {
        withThreadLocalAssigned {
            val tokeniser = TokenGenerator(this, source)
            exportedSingleCharFunctions.forEach { token ->
                tokeniser.registerSingleCharFunction(token)
            }
            val parser = APLParser(tokeniser)
            return if (newContext) {
                withSavedNamespace {
                    val instr = parser.parseValueToplevel(EndOfFile)
                    val newInstr = RootEnvironmentInstruction(parser.currentEnvironment(), instr, instr.pos)
                    newInstr.evalWithNewContext(this)
                }
            } else {
                val instr = parser.parseValueToplevel(EndOfFile)
                rootContext.reinitRootBindings()
                instr.evalWithContext(rootContext)
            }
        }
    }

    fun internSymbol(name: String, namespace: Namespace? = null): Symbol = (namespace ?: currentNamespace).internSymbol(name)

    fun makeNamespace(name: String, overrideDefaultImport: Boolean = false): Namespace {
        return namespaces.getOrPut(name) {
            val namespace = Namespace(name)
            if (!overrideDefaultImport) {
                namespace.addImport(coreNamespace)
            }
            namespace
        }
    }

    private fun resolveAlias(name: Symbol) = functionAliases[name] ?: name

    fun isSelfEvaluatingSymbol(name: Symbol) = name.namespace === keywordNamespace

    fun registerCustomSyntax(customSyntax: CustomSyntax) {
        customSyntaxEntries[customSyntax.name] = customSyntax
    }

    fun syntaxRulesForSymbol(name: Symbol): CustomSyntax? {
        return customSyntaxEntries[name]
    }

    fun registerCustomSyntaxSub(customSyntax: CustomSyntax) {
        customSyntaxSubEntries[customSyntax.name] = customSyntax
    }

    fun customSyntaxSubRulesForSymbol(name: Symbol): CustomSyntax? {
        return customSyntaxSubEntries[name]
    }

    inline fun <T> withSavedNamespace(fn: () -> T): T {
        val oldNamespace = currentNamespace
        try {
            return fn()
        } finally {
            currentNamespace = oldNamespace
        }
    }

    inline fun <T> withCallStackElement(name: String, pos: Position, fn: () -> T): T {
        if(callStack.size >= 100) {
            throwAPLException(APLEvalException("Stack overflow", pos))
        }
        val callStackElement = CallStackElement(name, pos)
        callStack.add(callStackElement)
        val prevSize = callStack.size
        try {
            return fn()
        } finally {
            assertx(prevSize == callStack.size)
            val removedElement = callStack.removeLast()
            assertx(removedElement === callStackElement)
        }
    }

    inline fun <T> withThreadLocalAssigned(fn: () -> T): T {
        val oldThreadLocal = threadLocalEngineRef.value
        threadLocalEngineRef.value = this
        try {
            return fn()
        } finally {
            threadLocalEngineRef.value = oldThreadLocal
        }
    }

    fun registerExportedSingleCharFunction(name: String) {
        exportedSingleCharFunctions.add(name)
    }
}

fun throwAPLException(ex: APLEvalException): Nothing {
    val engine = threadLocalEngineRef.value
    if (engine != null) {
        ex.callStack = engine.callStack.map { e -> e.copy() }
    }
    throw ex
}

expect fun platformInit(engine: Engine)

class VariableHolder {
    var value: APLValue? = null
}

class RuntimeContext(val engine: Engine, val environment: Environment, val parent: RuntimeContext? = null) {
    private val localVariables = HashMap<EnvironmentBinding, VariableHolder>()

    init {
        initBindings()
    }

    private fun initBindings() {
        environment.localBindings().forEach { b ->
            val holder = if (b.environment === environment) {
                VariableHolder()
            } else {
                fun recurse(c: RuntimeContext?): VariableHolder {
                    if (c == null) {
                        throw IllegalStateException("Can't find binding in parents")
                    }
                    return c.localVariables[b] ?: recurse(c.parent)
                }
                recurse(parent)
            }
            localVariables[b] = holder
        }
    }

    fun reinitRootBindings() {
        environment.localBindings().forEach { b ->
            if (localVariables[b] == null) {
                localVariables[b] = VariableHolder()
            }
        }
    }

//    fun lookupVar(name: Symbol, localOnly: Boolean = false): APLValue? {
//        val result = localVariables[name]
//        if (result != null) {
//            return result.value
//        }
//        if (!localOnly && parent != null) {
//            return parent.lookupVar(name)
//        }
//        return null
//    }

    fun isLocallyBound(sym: Symbol): Boolean {
        // TODO: This hack is needed for the KAP function isLocallyBound to work. A better strategy is needed.
        val holder = localVariables.entries.find { it.key.name === sym } ?: return false
        return holder.value.value != null
    }

    private fun findOrThrow(name: EnvironmentBinding): VariableHolder {
        return localVariables[name] ?: throw IllegalStateException("Attempt to set the value of a nonexistent binding: ${name}")
    }

    fun setVar(name: EnvironmentBinding, value: APLValue) {
        this.findOrThrow(name).value = value
    }

    fun getVar(binding: EnvironmentBinding): APLValue? = findOrThrow(binding).value

    fun link(env: Environment): RuntimeContext = RuntimeContext(engine, env, this)

    fun assignArgs(args: List<EnvironmentBinding>, a: APLValue, pos: Position? = null) {
        fun checkLength(expectedLength: Int, actualLength: Int) {
            if (expectedLength != actualLength) {
                throwAPLException(APLIllegalArgumentException("Argument mismatch. Expected: ${expectedLength}, actual length: ${actualLength}", pos))
            }
        }

        val v = a.unwrapDeferredValue()
        if (v is APLList) {
            checkLength(args.size, v.listSize())
            for (i in args.indices) {
                setVar(args[i], v.listElement(i))
            }
        } else {
            checkLength(args.size, 1)
            setVar(args[0], a)
        }
    }

    inline fun <T> withCallStackElement(name: String, pos: Position, fn: () -> T): T {
        return engine.withCallStackElement(name, pos, fn)
    }
}

interface FunctionDefinitionListener {
    fun functionDefined(name: Symbol, fn: APLFunctionDescriptor) = Unit
    fun functionRemoved(name: Symbol) = Unit
    fun operatorDefined(name: Symbol, fn: APLOperator) = Unit
    fun operatorRemoved(name: Symbol) = Unit
}

interface KapModule {
    /**
     * The name of the module.
     */
    val name: String

    /**
     * Initialise the module.
     */
    fun init(engine: Engine)
}
