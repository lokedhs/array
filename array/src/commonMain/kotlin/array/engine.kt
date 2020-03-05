package array

import array.builtins.*

interface APLFunction {
    fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue
    fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue
}

abstract class NoAxisAPLFunction : APLFunction {
    override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        return eval1Arg(context, a)
    }

    abstract fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue


    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        return eval2Arg(context, a, b)
    }

    abstract fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue
}

class DeclaredFunction(val instruction: Instruction, val leftArgName: Symbol, val rightArgName: Symbol) : APLFunction {
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

interface APLOperator {
    fun combineFunction(fn: APLFunction, operatorAxis: Instruction?): APLFunction
}

class Engine {
    private val functions = HashMap<Symbol, APLFunction>()
    private val operators = HashMap<Symbol, APLOperator>()
    private val symbols = HashMap<String, Symbol>()
    private val variables = HashMap<Symbol, APLValue>()
    private val functionDefinitionListeners = ArrayList<FunctionDefinitionListener>()

    init {
        // core functions
        registerFunction(internSymbol("+"), AddAPLFunction())
        registerFunction(internSymbol("-"), SubAPLFunction())
        registerFunction(internSymbol("×"), MulAPLFunction())
        registerFunction(internSymbol("÷"), DivAPLFunction())
        registerFunction(internSymbol("⋆"), PowerAPLFunction())
        registerFunction(internSymbol("⍟"), LogAPLFunction())
        registerFunction(internSymbol("⍳"), IotaAPLFunction())
        registerFunction(internSymbol("⍴"), RhoAPLFunction())
        registerFunction(internSymbol("⊢"), IdentityAPLFunction())
        registerFunction(internSymbol("⊣"), HideAPLFunction())
        registerFunction(internSymbol("="), EqualsAPLFunction())
        registerFunction(internSymbol("≠"), NotEqualsAPLFunction())
        registerFunction(internSymbol("<"), LessThanAPLFunction())
        registerFunction(internSymbol(">"), GreaterThanAPLFunction())
        registerFunction(internSymbol("≤"), LessThanEqualAPLFunction())
        registerFunction(internSymbol("≥"), GreaterThanEqualAPLFunction())
        registerFunction(internSymbol("⌷"), AccessFromIndexAPLFunction())
        registerFunction(internSymbol("⊂"), EncloseAPLFunction())
        registerFunction(internSymbol("⊃"), DiscloseAPLFunction())
        registerFunction(internSymbol("∧"), AndAPLFunction())
        registerFunction(internSymbol("∨"), OrAPLFunction())
        registerFunction(internSymbol(","), ConcatenateAPLFunction())
        registerFunction(internSymbol("↑"), TakeAPLFunction())
        registerFunction(internSymbol("?"), RandomAPLFunction())
        registerFunction(internSymbol("⌽"), RotateHorizFunction())
        registerFunction(internSymbol("⊖"), RotateVertFunction())

        // io functions
        registerFunction(internSymbol("print"), PrintAPLFunction())
        registerFunction(internSymbol("readCsvFile"), ReadCSVFunction())

        // maths
        registerFunction(internSymbol("sin"), SinAPLFunction())
        registerFunction(internSymbol("cos"), CosAPLFunction())
        registerFunction(internSymbol("tan"), TanAPLFunction())
        registerFunction(internSymbol("asin"), AsinAPLFunction())
        registerFunction(internSymbol("acos"), AcosAPLFunction())
        registerFunction(internSymbol("atan"), AtanAPLFunction())

        // operators
        registerOperator(internSymbol("¨"), ForEachOp())
        registerOperator(internSymbol("/"), ReduceOp())
    }

    fun addFunctionDefinitionListener(listener: FunctionDefinitionListener) {
        functionDefinitionListeners.add(listener)
    }

    fun removeFunctionDefinitionListener(listener: FunctionDefinitionListener) {
        functionDefinitionListeners.remove(listener)
    }

    fun registerFunction(name: Symbol, fn: APLFunction) {
        functions[name] = fn
        functionDefinitionListeners.forEach { it.functionDefined(name, fn) }
    }

    fun registerOperator(name: Symbol, fn: APLOperator) {
        operators[name] = fn
        functionDefinitionListeners.forEach { it.operatorDefined(name, fn) }
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

    fun getFunction(name: Symbol) = functions[name]
    fun getOperator(token: Symbol) = operators[token]
    fun parseWithTokenGenerator(tokeniser: TokenGenerator) = parseValueToplevel(this, tokeniser, EndOfFile)
    fun parseString(input: String) = parseWithTokenGenerator(TokenGenerator(this, StringCharacterProvider(input)))
    fun internSymbol(name: String): Symbol = symbols.getOrPut(name, { Symbol(name) })
    fun lookupVar(name: Symbol): APLValue? = variables[name]
    fun makeRuntimeContext() = RuntimeContext(this, null)
}

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
}

interface FunctionDefinitionListener {
    fun functionDefined(name: Symbol, fn: APLFunction) = Unit
    fun functionRemoved(name: Symbol) = Unit
    fun operatorDefined(name: Symbol, fn: APLOperator) = Unit
    fun operatorRemoved(name: Symbol) = Unit
}
