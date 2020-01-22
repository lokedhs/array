package array

interface Function {
    fun eval1Arg(arg: APLValue) : APLValue
    fun eval2Arg(arg1: APLValue, arg2: APLValue) : APLValue
}

class Engine {
    private val functions = HashMap<Symbol,Function>()
    private val symbols = HashMap<String,Symbol>()
    private val variables = HashMap<Symbol,APLValue>()

    init {
        registerFunction(internSymbol("+"), AddFunction())
        registerFunction(internSymbol("-"), SubFunction())
        registerFunction(internSymbol("print"), PrintFunction())
    }

    fun registerFunction(name: Symbol, fn: Function) {
        functions[name] = fn
    }

    fun getFunction(name: Symbol): Function? {
        return functions[name]
    }

    fun parseString(input: String): Instruction {
        val tokeniser = TokenGenerator(this, input)
        return parseValue(this, tokeniser)
    }

    fun internSymbol(name: String): Symbol {
        return symbols.getOrPut(name, {Symbol(name)})
    }

    fun lookupVar(name: Symbol): APLValue? {
        return variables[name]
    }
}
