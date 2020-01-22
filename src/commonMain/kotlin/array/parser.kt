package array

abstract class Token

object Whitespace : Token()
object EndOfFile : Token()
object CloseParen : Token()

class Symbol(val value: String) : Token(), Comparable<Symbol> {
    override fun toString() = "Symbol[name=${value}]"
    override fun compareTo(other: Symbol) = value.compareTo(other.value)
    override fun hashCode() = value.hashCode()
    override fun equals(other: Any?) = other != null && other is Symbol && value == other.value
}

class ParsedNumber(val value: Long) : Token()

class TokenGenerator(val engine: Engine, val content: String) {
    private val singleCharFunctions: Set<String>
    private var pos = 0
    private val pushBackQueue = ArrayList<Token>()

    init {
        singleCharFunctions = HashSet(listOf("+", "-", "×", "÷"))
    }

    private fun getNextChar(): Int {
        // Ignore surrogate pairs for now
        return content[pos++].toInt()
    }

    fun nextTokenOrSpace(): Token {
        if (!pushBackQueue.isEmpty()) {
            return pushBackQueue.removeAt(pushBackQueue.size - 1)
        }

        if (pos >= content.length) {
            return EndOfFile;
        }

        // For now, let's ignore surrogate pairs.
        val ch = content[pos++].toInt()
        return when {
            isOpenParen(ch) -> collectParenExpr()
            isNegationSign(ch) -> collectNegativeNumber()
            singleCharFunctions.contains(codepointToString(ch)) -> engine.internSymbol(codepointToString(ch))
            isDigit(ch) -> collectNumber(ch)
            isWhitespace(ch) -> Whitespace
            isLetter(ch) -> collectSymbol(ch)
            else -> throw UnexpectedSymbol(ch)
        }
    }

    private fun codepointToString(ch: Int): String {
        val buf = StringBuilder()
        buf.addCodepoint(ch)
        return buf.toString()
    }

    private fun collectParenExpr(): Token {
        TODO("collect until closing paren.")
    }

    private fun isOpenParen(ch: Int) = ch == '('.toInt()
    private fun isNegationSign(ch: Int) = ch == '¯'.toInt()

    private fun collectNumber(firstChar: Int, isNegative: Boolean = false): ParsedNumber {
        val buf = StringBuilder()
        buf.addCodepoint(firstChar)
        while (pos < content.length) {
            val ch = getNextChar()
            if (isLetter(ch)) {
                throw IllegalNumberFormat("Illegal number format")
            }
            if (!isDigit(ch)) {
                pos--;
                break;
            }
            buf.addCodepoint(ch)
        }
        return ParsedNumber(buf.toString().toLong() * if(isNegative) -1 else 1)
    }

    private fun collectNegativeNumber() : ParsedNumber {
        val ch = getNextChar()
        unless(isDigit(ch)) {
            throw IllegalNumberFormat("Negation sign not followed by number")
        }
        return collectNumber(ch, true)
    }

    private fun collectSymbol(firstChar: Int): Symbol {
        val buf = StringBuilder()
        buf.addCodepoint(firstChar)
        while (pos < content.length) {
            val ch = getNextChar()
            if (!isLetter(ch) || isDigit(ch)) {
                pos--;
                break
            }
            buf.addCodepoint(ch)
        }
        return engine.internSymbol(buf.toString())
    }

    fun nextToken(): Token {
        while (true) {
            val token = nextTokenOrSpace()
            if (token != Whitespace) {
                return token
            }
        }
    }
}

interface Instruction {
    fun evalWithEngine(engine: Engine): APLValue
}

class FunctionCall1Arg(val fn: Function, val rightArgs: Instruction) : Instruction {
    override fun evalWithEngine(engine: Engine) = fn.eval1Arg(rightArgs.evalWithEngine(engine))
    override fun toString() = "FunctionCall1Arg(fn=${fn}, rightArgs=${rightArgs})"
}

class FunctionCall2Arg(val fn: Function, val leftArgs: Instruction, val rightArgs: Instruction) : Instruction {
    override fun evalWithEngine(engine: Engine): APLValue {
        val leftValue = rightArgs.evalWithEngine(engine)
        val rightValue = leftArgs.evalWithEngine(engine)
        return fn.eval2Arg(rightValue, leftValue)
    }

    override fun toString() = "FunctionCall2Arg(fn=${fn}, leftArgs=${leftArgs}, rightArgs=${rightArgs})"
}

class VariableRef(val name: Symbol) : Instruction {
    override fun evalWithEngine(engine: Engine): APLValue {
        return engine.lookupVar(name) ?: throw VariableNotAssigned(name)
    }

    override fun toString() = "Var(${name})"
}

class Literal1DArray(val values: List<Instruction>) : Instruction {
    override fun evalWithEngine(engine: Engine): APLValue {
        val size = values.size
        val result = Array<APLValue?>(size) {null}
        for (i in (size - 1) downTo 0) {
            result[i] = values[i].evalWithEngine(engine)
        }
        return APLArrayImpl(arrayOf(size)) { result[it]!! }
    }

    override fun toString() = "Literal1DArray(${values})"
}

class LiteralScalarValue(val value: Instruction) : Instruction {
    override fun evalWithEngine(engine: Engine) = value.evalWithEngine(engine)
    override fun toString() = "LiteralScalarValue(${value})"
}

class LiteralNumber(val value: Long) : Instruction {
    override fun evalWithEngine(engine: Engine) = APLLong(value)
    override fun toString() = "LiteralNumber(value=$value)"
}

fun parseValue(engine: Engine, tokeniser: TokenGenerator): Instruction {
    fun valueListToArg(args: List<Instruction>): Instruction {
        unless(!args.isEmpty()) {
            throw IllegalStateException("Argument list should not be empty")
        }
        if (args.size == 1) {
            return LiteralScalarValue(args[0])
        } else {
            return Literal1DArray(args)
        }
    }

    val leftArgs = ArrayList<Instruction>()

    while (true) {
        val token = tokeniser.nextToken()
        if (token == CloseParen || token == EndOfFile) {
            return valueListToArg(leftArgs)
        }

        when (token) {
            is Symbol -> {
                val fn = engine.getFunction(token)
                if (fn != null) {
                    val rightArgs = parseValue(engine, tokeniser)
                    return if (leftArgs.isEmpty()) FunctionCall1Arg(fn, rightArgs) else FunctionCall2Arg(
                        fn,
                        valueListToArg(leftArgs),
                        rightArgs
                    )
                } else {
                    leftArgs.add(VariableRef(token))
                }
            }
            is ParsedNumber -> leftArgs.add(LiteralNumber(token.value))
            else -> throw UnexpectedToken(token)
        }
    }
}
