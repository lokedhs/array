package array

import array.builtins.AddAPLFunction
import array.builtins.IotaAPLFunction
import array.builtins.RhoAPLFunction
import array.builtins.SubAPLFunction

interface APLFunction {
    fun eval1Arg(arg: APLValue) : APLValue
    fun eval2Arg(arg1: APLValue, arg2: APLValue) : APLValue
}

class Engine {
    private val functions = HashMap<Symbol,APLFunction>()
    private val symbols = HashMap<String,Symbol>()
    private val variables = HashMap<Symbol,APLValue>()

    init {
        registerFunction(internSymbol("+"), AddAPLFunction())
        registerFunction(internSymbol("-"), SubAPLFunction())
        registerFunction(internSymbol("⍳"), IotaAPLFunction())
        registerFunction(internSymbol("⍴"), RhoAPLFunction())
        registerFunction(internSymbol("print"), PrintAPLFunction())
    }

    fun registerFunction(name: Symbol, fn: APLFunction) {
        functions[name] = fn
    }

    fun getFunction(name: Symbol): APLFunction? {
        return functions[name]
    }

    fun parseString(input: String): Instruction {
        val tokeniser = TokenGenerator(this, input)
        return parseValue(this, tokeniser, EndOfFile)
    }

    fun internSymbol(name: String): Symbol {
        return symbols.getOrPut(name, {Symbol(name)})
    }

    fun lookupVar(name: Symbol): APLValue? {
        return variables[name]
    }
}
