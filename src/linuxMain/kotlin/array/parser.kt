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
        singleCharFunctions = HashSet(listOf("+", "-", "*", "/"))
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
            singleCharFunctions.contains(ch.toString()) -> Symbol(ch.toString())
            else -> throw UnexpectedSymbol("Unexpected symbol: ${ch}")
        }
    }

    private fun collectNumber(firstChar: Char) : ParsedNumber {
        val buf = StringBuilder()
        buf.append(firstChar)
        while(pos < content.length) {
            val ch = content[pos]
            if(ch.isLetter()) {
                throw IllegalNumberFormat("Illegal number format")
            }
            if(!ch.isDigit()) {
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
            val ch = content[pos]
            if (!ch.isLetterOrDigit()) {
                break
            }
            buf.append(ch)
            pos++
        }
        return Symbol(buf.toString())
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

class FunctionCall1Arg(fn: Function, rightArgs: Instruction) : Instruction
class FunctionCall2Arg(fn: Function, leftArgs: Instruction, rightArgs: Instruction) : Instruction

class VariableRef(val name: Symbol) : Instruction

class Literal1DArray(val values: List<Instruction>) : Instruction
class LiteralNumber() : Instruction // this is not correct

fun parseValue(engine: Engine, tokeniser: TokenGenerator): Instruction {
    val leftArgs = ArrayList<Instruction>()

    val token = tokeniser.nextToken()
    if (token == CloseParen || token == EndOfFile) {
        return Literal1DArray(leftArgs)
    }

    when (token) {
        is Symbol -> {
            val fn = engine.getFunction(token)
            if (fn != null) {
                val rightArgs = parseValue(engine, tokeniser)
                return FunctionCall2Arg(fn, Literal1DArray(leftArgs), rightArgs)
            } else {
                leftArgs.add(VariableRef(token))
            }
        }
        is LiteralNumber -> {

        }
    }
}
