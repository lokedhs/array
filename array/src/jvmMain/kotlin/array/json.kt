package array

import array.json.parseJsonToAPL

class ReadJsonAPLFunction : APLFunctionDescriptor {
    class ReadJsonAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val filename = a.toStringValue(pos)
            val json = openCharFile(filename).use { input ->
                parseJsonToAPL(input)
            }
            return json
        }
    }

    override fun make(pos: Position) = ReadJsonAPLFunctionImpl(pos)
}

class ReadStringJsonAPLFunction : APLFunctionDescriptor {
    class ReadStringJsonAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val content = a.toStringValue(pos)
            return parseJsonToAPL(StringCharacterProvider(content))
        }
    }

    override fun make(pos: Position) = ReadStringJsonAPLFunctionImpl(pos)
}


class JsonAPLModule : KapModule {
    override val name: String
        get() = "json"

    override fun init(engine: Engine) {
        val namespace = engine.makeNamespace("json")
        engine.registerFunction(namespace.internAndExport("read"), ReadJsonAPLFunction())
        engine.registerFunction(namespace.internAndExport("readString"), ReadStringJsonAPLFunction())
    }
}
