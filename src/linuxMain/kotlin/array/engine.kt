package array

class Function

class Engine {
    private val functions = HashMap<Symbol,Function>()

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
}
