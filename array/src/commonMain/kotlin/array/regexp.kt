package array

class InvalidRegexp(message: String, pos: Position? = null) : APLEvalException(message, pos)

private fun regexpFromValue(a: APLValue, pos: Position): Regex {
    return if (a is RegexpMatcherValue) {
        a.matcher
    } else {
        val regexpString = a.toStringValue(pos)
        try {
            toRegexpWithException(regexpString, emptySet())
        } catch (e: RegexpParseException) {
            throwAPLException(InvalidRegexp("Invalid format: ${regexpString}", pos))
        }
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

class RegexpMatcherValue(val matcher: Regex) : APLSingleValue() {
    override val aplValueType get() = APLValueType.INTERNAL
    override fun formatted(style: FormatStyle) = "regexp-matcher"
    override fun compareEquals(reference: APLValue) = reference is RegexpMatcherValue && matcher == reference.matcher
    override fun makeKey(): APLValueKey = APLValueKeyImpl(this, matcher)
}

class CreateRegexpFunction : APLFunctionDescriptor {
    class CreateRegexpFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return RegexpMatcherValue(toRegexpWithException(a.toStringValue(pos), emptySet()))
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val d = a.dimensions
            val flags = when {
                d.size == 0 -> setOf(valueToFlag(context.engine, a))
                d.size == 1 -> a.membersSequence().map { v -> valueToFlag(context.engine, v) }.toSet()
                else -> throwAPLException(APLEvalException("Regexp flags must be a single symbol or a one-dimensional array", pos))
            }
            return RegexpMatcherValue(toRegexpWithException(b.toStringValue(pos), flags))
        }

        private fun valueToFlag(engine: Engine, v: APLValue): RegexOption {
            val s = v.unwrapDeferredValue()
            if (s !is APLSymbol) {
                throwAPLException(APLEvalException("Regexp flag must be a symbol"))
            }
            val sym = s.value
            return when {
                sym === engine.keywordNamespace.internSymbol("ignoreCase") -> RegexOption.IGNORE_CASE
                sym === engine.keywordNamespace.internSymbol("multiLine") -> RegexOption.MULTILINE
                else -> throwAPLException(APLEvalException("Unknown regexp flag: ${sym.symbolName}"))
            }
        }
    }

    override fun make(pos: Position) = CreateRegexpFunctionImpl(pos)
}

class RegexpMatcherIndexFunction : APLFunctionDescriptor {
    class RegexpMatcherIndexFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val matchString = b.toStringValue(pos)
            val regexp = regexpFromValue(a, pos)
            val result = regexp.find(matchString) ?: return APLNullValue.APL_NULL_INSTANCE
            val groups = result.groups
            return APLArrayImpl(dimensionsOfSize(groups.size), Array(groups.size) { i ->
                val v = groups.get(i)
                assertx(!(i == 0 && v == null))
                if (v == null) {
                    APLNullValue.APL_NULL_INSTANCE
                } else {
                    val (start, end) = indexesFromRegexpMatchGroup(v)
                    APLArrayLong(dimensionsOfSize(2), longArrayOf(start.toLong(), end.toLong()))
                }
            })
        }
    }

    override fun make(pos: Position) = RegexpMatcherIndexFunctionImpl(pos)
}


class RegexpModule : KapModule {
    override val name get() = "regexp"

    override fun init(engine: Engine) {
        val namespace = engine.makeNamespace("regexp")
        fun registerFn(name: String, fn: APLFunctionDescriptor) {
            engine.registerFunction(namespace.internAndExport(name), fn)
        }
        registerFn("matches", RegexpMatchesFunction())
        registerFn("find", RegexpFindFunction())
        registerFn("create", CreateRegexpFunction())
        registerFn("index", RegexpMatcherIndexFunction())
    }
}
