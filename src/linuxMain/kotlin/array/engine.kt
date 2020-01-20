package array

class Function

class Engine {
    private val functions = HashMap<Symbol,Function>()
    private val symbols = HashMap<String,Symbol>()

    fun registerFunction(name: Symbol) {
        functions[name] = Function()
    }

    fun getFunction(name: Symbol): Function? {
        return functions[name]
    }

    fun parseString(input: String): Instruction {
        val tokeniser = TokenGenerator(this, input)
        val result = parseValue(this, tokeniser)
        return result
    }

    fun internSymbol(name: String): Symbol {
        return symbols.getOrPut(name, {Symbol(name)})
    }
}
