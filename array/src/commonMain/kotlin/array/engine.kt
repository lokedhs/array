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
    val leftArgName: Symbol,
    val rightArgName: Symbol
) : APLFunctionDescriptor {
    inner class DeclaredFunctionImpl(pos: Position) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            val localContext = context.link()
            localContext.setVar(rightArgName, a)
            return instruction.evalWithContext(localContext)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            val localContext = context.link()
            localContext.setVar(leftArgName, a)
            localContext.setVar(rightArgName, b)
            return instruction.evalWithContext(localContext)
        }
    }

    override fun make(pos: Position) = DeclaredFunctionImpl(pos)
}

interface APLOperator

interface APLOperatorOneArg : APLOperator {
    fun combineFunction(fn: APLFunctionDescriptor, operatorAxis: Instruction?): APLFunctionDescriptor
}

interface APLOperatorTwoArg : APLOperator {
    fun combineFunction(fn1: APLFunctionDescriptor, fn2: APLFunctionDescriptor, operatorAxis: Instruction?): APLFunctionDescriptor
}

private const val CORE_NAMESPACE_NAME = "kap"
private const val KEYWORD_NAMESPACE_NAME = "core"
private const val DEFAULT_NAMESPACE_NAME = "default"

class Engine {
    private val functions = HashMap<Symbol, APLFunctionDescriptor>()
    private val operators = HashMap<Symbol, APLOperator>()
    private val variables = HashMap<Symbol, APLValue>()
    private val functionDefinitionListeners = ArrayList<FunctionDefinitionListener>()
    private val functionAliases = HashMap<Symbol, Symbol>()
    private val namespaces = HashMap<String, Namespace>()

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

        // operators
        registerNativeOperator("¨", ForEachOp())
        registerNativeOperator("/", ReduceOp())
        registerNativeOperator("⌺", OuterJoinOp())
        registerNativeOperator(".", OuterInnerJoinOp())
        registerNativeOperator("⍨", CommuteOp())

        // function aliases
        functionAliases[coreNamespace.internAndExport("*")] = coreNamespace.internAndExport("⋆")

        platformInit(this)
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
    fun parseWithTokenGenerator(tokeniser: TokenGenerator) = APLParser(tokeniser).parseValueToplevel(EndOfFile)
    fun parseString(input: String) = parseWithTokenGenerator(TokenGenerator(this, StringSourceLocation(input)))
    fun internSymbol(name: String, namespace: Namespace? = null): Symbol = (namespace ?: currentNamespace).internSymbol(name)

    fun lookupVar(name: Symbol): APLValue? = variables[name]
    fun makeRuntimeContext() = RuntimeContext(this, null)
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
}

expect fun platformInit(engine: Engine)

class RuntimeContext(val engine: Engine, val parent: RuntimeContext? = null) {
    private val localVariables = HashMap<Symbol, APLValue>()

    fun lookupVar(name: Symbol): APLValue? {
        val result = localVariables[name]
        return when {
            result != null -> result
            parent != null -> parent.lookupVar(name)
            else -> engine.lookupVar(name)
        }
    }

    fun setVar(name: Symbol, value: APLValue) {
        localVariables[name] = value
    }

    fun link() = RuntimeContext(engine, this)

    fun assignArgs(args: List<Symbol>, a: APLValue, pos: Position? = null) {
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
