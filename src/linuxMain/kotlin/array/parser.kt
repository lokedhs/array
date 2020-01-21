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

    fun nextTokenOrSpace(): Token {
        if (!pushBackQueue.isEmpty()) {
            return pushBackQueue.removeAt(pushBackQueue.size - 1)
        }

        if (pos >= content.length) {
            return EndOfFile;
        }

        val ch = content[pos++]
        return when {
            ch.isDigit() -> collectNumber(ch)
            ch.isWhitespace() -> Whitespace
            ch.isLetter() -> collectSymbol(ch)
            singleCharFunctions.contains(ch.toString()) -> engine.internSymbol(ch.toString())
            else -> throw UnexpectedSymbol(ch)
        }
    }

    private fun collectNumber(firstChar: Char): ParsedNumber {
        val buf = StringBuilder()
        buf.append(firstChar)
        while (pos < content.length) {
            val ch = content[pos++]
            if (ch.isLetter()) {
                throw IllegalNumberFormat("Illegal number format")
            }
            if (!ch.isDigit()) {
                break;
            }
            buf.append(ch)
        }
        return ParsedNumber(buf.toString().toLong())
    }

    private fun collectSymbol(firstChar: Char): Symbol {
        val buf = StringBuilder()
        buf.append(firstChar)
        while (pos < content.length) {
            val ch = content[pos++]
            if (!ch.isLetterOrDigit()) {
                break
            }
            buf.append(ch)
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

interface Instruction

class FunctionCall1Arg(val fn: Function, val rightArgs: Instruction) : Instruction {
    override fun toString() = "FunctionCall1Arg(fn=${fn}, rightArgs=${rightArgs})"
}

class FunctionCall2Arg(val fn: Function, val leftArgs: Instruction, val rightArgs: Instruction) : Instruction {
    override fun toString() = "FunctionCall2Arg(fn=${fn}, leftArgs=${leftArgs}, rightArgs=${rightArgs})"
}

class VariableRef(val name: Symbol) : Instruction {
    override fun toString() = "Var(${name})"
}

class Literal1DArray(val values: List<Instruction>) : Instruction {
    override fun toString() = "Literal1DArray(${values})"
}

class LiteralScalarValue(val value: Instruction) : Instruction {
    override fun toString() = "LiteralScalarValue(${value})"
}

class LiteralNumber(val value: Long) : Instruction {
    override fun toString() = "LiteralNumber(value=$value)"
}

fun parseValue(engine: Engine, tokeniser: TokenGenerator): Instruction {
    fun valueListToArg(args: List<Instruction>) : Instruction {
        assert(!args.isEmpty())
        if(args.size == 1) {
            return LiteralScalarValue(args[0])
        }
        else {
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
                    return if(leftArgs.isEmpty()) FunctionCall1Arg(fn, rightArgs) else FunctionCall2Arg(fn, valueListToArg(leftArgs), rightArgs)
                } else {
                    leftArgs.add(VariableRef(token))
                }
            }
            is ParsedNumber -> leftArgs.add(LiteralNumber(token.value))
            else -> throw UnexpectedToken(token)
        }
    }
}
