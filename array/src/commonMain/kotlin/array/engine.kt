package array

import array.builtins.*

interface APLFunctionDescriptor {
    fun make(pos: Position): APLFunction
}

abstract class APLFunction(val pos: Position) {
    open fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue = throw Unimplemented1ArgException(pos)
    open fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue =
        throw Unimplemented2ArgException(pos)

    open fun identityValue(): APLValue = throw APLIncompatibleDomainsException("Function does not have an identity value", pos)
}

abstract class NoAxisAPLFunction(pos: Position) : APLFunction(pos) {
    override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        if (axis != null) {
            throw AxisNotSupported(pos)
        }
        return eval1Arg(context, a)
    }

    open fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue = throw Unimplemented1ArgException(pos)


    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        if (axis != null) {
            throw AxisNotSupported(pos)
        }
        return eval2Arg(context, a, b)
    }

    open fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue = throw Unimplemented2ArgException(pos)
}

class DeclaredFunction(
    val instruction: Instruction,
    val leftArgName: EnvironmentBinding,
    val rightArgName: EnvironmentBinding,
    val env: Environment
) : APLFunctionDescriptor {
    inner class DeclaredFunctionImpl(pos: Position) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            val localContext = context.link(env)
            localContext.setVar(rightArgName, a)
            return instruction.evalWithContext(localContext)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            val localContext = context.link(env)
            localContext.setVar(leftArgName, a)
            localContext.setVar(rightArgName, b)
            return instruction.evalWithContext(localContext)
        }
    }

    override fun make(pos: Position) = DeclaredFunctionImpl(pos)
}

interface APLOperator

interface APLOperatorOneArg : APLOperator {
    fun combineFunction(fn: APLFunctionDescriptor, operatorAxis: Instruction?, pos: Position): APLFunctionDescriptor
}

interface APLOperatorTwoArg : APLOperator {
    fun combineFunction(
        fn1: APLFunctionDescriptor,
        fn2: APLFunctionDescriptor,
        operatorAxis: Instruction?,
        opPos: Position,
        fn1Pos: Position,
        fn2Pos: Position): APLFunctionDescriptor
}

private const val CORE_NAMESPACE_NAME = "kap"
private const val KEYWORD_NAMESPACE_NAME = "core"
private const val DEFAULT_NAMESPACE_NAME = "default"

class Engine {
    private val functions = HashMap<Symbol, APLFunctionDescriptor>()
    private val operators = HashMap<Symbol, APLOperator>()
    private val functionDefinitionListeners = ArrayList<FunctionDefinitionListener>()
    private val functionAliases = HashMap<Symbol, Symbol>()
    private val namespaces = HashMap<String, Namespace>()
    private val customSyntaxEntries = HashMap<Symbol, CustomSyntax>()
    private val librarySearchPaths = ArrayList<String>()

    var standardOutput: CharacterOutput = NullCharacterOutput()
    val coreNamespace = makeNamespace(CORE_NAMESPACE_NAME, overrideDefaultImport = true)
    val keywordNamespace = makeNamespace(KEYWORD_NAMESPACE_NAME, overrideDefaultImport = true)
    val initialNamespace = makeNamespace(DEFAULT_NAMESPACE_NAME)
    var currentNamespace = initialNamespace

    init {
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
        registerNativeFunction(",", ConcatenateAPLFunction())
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
        registerNativeFunction("/", SelectElementsFunction())

        // io functions
        registerNativeFunction("print", PrintAPLFunction())
        registerNativeFunction("readCsvFile", ReadCSVFunction())
        registerNativeFunction("load", LoadFunction())
        registerNativeFunction("httpRequest", HttpRequestFunction())

        // maths
        registerNativeFunction("sin", SinAPLFunction())
        registerNativeFunction("cos", CosAPLFunction())
        registerNativeFunction("tan", TanAPLFunction())
        registerNativeFunction("asin", AsinAPLFunction())
        registerNativeFunction("acos", AcosAPLFunction())
        registerNativeFunction("atan", AtanAPLFunction())

        // metafunctions
        registerNativeFunction("typeof", TypeofFunction())
        registerNativeFunction("isLocallyBound", IsLocallyBoundFunction())
        registerNativeFunction("comp", CompFunction())

        // operators
        registerNativeOperator("¨", ForEachOp())
        registerNativeOperator("/", ReduceOp())
        registerNativeOperator("⌺", OuterJoinOp())
        registerNativeOperator(".", OuterInnerJoinOp())
        registerNativeOperator("⍨", CommuteOp())
        registerNativeOperator("⍣", PowerAPLOperator())

        // function aliases
        functionAliases[coreNamespace.internAndExport("*")] = coreNamespace.internAndExport("⋆")

        platformInit(this)
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

    private fun registerNativeFunction(name: String, fn: APLFunctionDescriptor) {
        val sym = coreNamespace.internAndExport(name)
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

    fun parseWithTokenGenerator(tokeniser: TokenGenerator): RootEnvironmentInstruction {
        val parser = APLParser(tokeniser)
        val instr = parser.parseValueToplevel(EndOfFile)
        return RootEnvironmentInstruction(parser.currentEnvironment(), instr, instr.pos)
    }

    fun parseString(input: String) = parseWithTokenGenerator(TokenGenerator(this, StringSourceLocation(input)))
    fun internSymbol(name: String, namespace: Namespace? = null): Symbol = (namespace ?: currentNamespace).internSymbol(name)

    fun makeRuntimeContext() = RuntimeContext(this, Environment.nullEnvironment(), null)
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
        customSyntaxEntries[customSyntax.triggerSymbol] = customSyntax
    }

    fun syntaxRulesForSymbol(name: Symbol): CustomSyntax? {
        return customSyntaxEntries[name]
    }

    inline fun <T> withSavedNamespace(fn: () -> T): T {
        val oldNamespace = currentNamespace
        try {
            return fn()
        } finally {
            currentNamespace = oldNamespace
        }
    }
}

expect fun platformInit(engine: Engine)

class VariableHolder {
    var value: APLValue? = null
}

class RuntimeContext(val engine: Engine, val env: Environment, val parent: RuntimeContext? = null) {
    private val localVariables = HashMap<EnvironmentBinding, VariableHolder>()

    init {
        initBindings(env.localBindings())
    }

    private fun initBindings(bindings: Collection<EnvironmentBinding>) {
        bindings.forEach { b ->
            val holder = if (b.environment === env) {
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
                throw APLIllegalArgumentException("Argument mismatch. Expected: ${expectedLength}, actual length: ${actualLength}", pos)
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
}

interface FunctionDefinitionListener {
    fun functionDefined(name: Symbol, fn: APLFunctionDescriptor) = Unit
    fun functionRemoved(name: Symbol) = Unit
    fun operatorDefined(name: Symbol, fn: APLOperator) = Unit
    fun operatorRemoved(name: Symbol) = Unit
}
