package array

class InvalidRegex(message: String, pos: Position? = null) : APLEvalException(message, pos)

private fun regexpFromValue(a: APLValue, pos: Position): Regex {
    val regexString = a.toStringValue(pos)
    try {
        return regexString.toRegex()
    } catch (e: Exception) {
        throwAPLException(InvalidRegex("Invalid format: ${regexString}", pos))
    }
}

class RegexpMatchesFunction : APLFunctionDescriptor {
    class RegexpMatchesFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val matchString = b.toStringValue(pos)
            val regexp = regexpFromValue(a, pos)
            return if (regexp.find(matchString) != null) APLLONG_1 else APLLONG_0
        }
    }

    override fun make(pos: Position) = RegexpMatchesFunctionImpl(pos)
}

class RegexpFindFunction : APLFunctionDescriptor {
    class RegexpFindFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val matchString = b.toStringValue(pos)
            val regexp = regexpFromValue(a, pos)
            val result = regexp.find(matchString) ?: return APLNullValue.APL_NULL_INSTANCE
            val groups = result.groups
            var undefinedSym: Symbol? = null
            return APLArrayImpl(dimensionsOfSize(groups.size), Array(groups.size) { i ->
                val v = groups.get(i)
                assertx(!(i == 0 && v == null))
                if (v == null) {
                    if (undefinedSym == null) {
                        undefinedSym = context.engine.keywordNamespace.internSymbol("undefined")
                    }
                    APLSymbol(undefinedSym!!)
                } else {
                    APLString(v.value)
                }
            })
        }
    }

    override fun make(pos: Position) = RegexpFindFunctionImpl(pos)
}


class RegexModule : KapModule {
    override val name get() = "regexp"

    override fun init(engine: Engine) {
        val namespace = engine.makeNamespace("regexp")
        engine.registerFunction(namespace.internAndExport("matches"), RegexpMatchesFunction())
        engine.registerFunction(namespace.internAndExport("find"), RegexpFindFunction())
    }
}
