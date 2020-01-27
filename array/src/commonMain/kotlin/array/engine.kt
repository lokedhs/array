package array

import array.builtins.*

interface APLFunction {
    fun eval1Arg(context: RuntimeContext, arg: APLValue) : APLValue
    fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue) : APLValue
}

interface APLOperator {
    fun combineFunction(fn: APLFunction): APLFunction
}

class Engine {
    private val functions = HashMap<Symbol,APLFunction>()
    private val operators = HashMap<Symbol,APLOperator>()
    private val symbols = HashMap<String,Symbol>()
    private val variables = HashMap<Symbol,APLValue>()

    init {
        registerFunction(internSymbol("+"), AddAPLFunction())
        registerFunction(internSymbol("-"), SubAPLFunction())
        registerFunction(internSymbol("×"), MulAPLFunction())
        registerFunction(internSymbol("÷"), DivAPLFunction())
        registerFunction(internSymbol("⍳"), IotaAPLFunction())
        registerFunction(internSymbol("⍴"), RhoAPLFunction())
        registerFunction(internSymbol("print"), PrintAPLFunction())

        registerOperator(internSymbol("¨"), ForEachOp())
    }

    fun registerFunction(name: Symbol, fn: APLFunction) {
        functions[name] = fn
    }

    fun registerOperator(name: Symbol, fn: APLOperator) {
        operators[name] = fn
    }

    fun getFunction(name: Symbol) = functions[name]
    fun getOperator(token: Symbol) = operators[token]
    fun parseString(input: String) = parseValueToplevel(this, TokenGenerator(this, StringCharacterProvider(input)), EndOfFile)
    fun internSymbol(name: String): Symbol = symbols.getOrPut(name, {Symbol(name)})
    fun lookupVar(name: Symbol): APLValue? = variables[name]
    fun makeRuntimeContext() = RuntimeContext(this)
}

class RuntimeContext(val engine: Engine) {
    fun lookupVar(name: Symbol) = engine.lookupVar(name)
}
