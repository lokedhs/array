package array

abstract class Token

object Whitespace : Token()
object EndOfFile : Token()
object OpenParen : Token()
object CloseParen : Token()
object StatementSeparator : Token()

class Symbol(val value: String) : Token(), Comparable<Symbol> {
    override fun toString() = "Symbol[name=${value}]"
    override fun compareTo(other: Symbol) = value.compareTo(other.value)
    override fun hashCode() = value.hashCode()
    override fun equals(other: Any?) = other != null && other is Symbol && value == other.value
}

class ParsedLong(val value: Long) : Token()

class TokenGenerator(val engine: Engine, val content: CharacterProvider) {
    private val singleCharFunctions: Set<String>
    private var pos = 0
    private val pushBackQueue = ArrayList<Token>()

    init {
        singleCharFunctions = hashSetOf(
            "!", "#", "%", "&", "*", "+", ",", "-", "/", "<", "=", ">", "?", "@", "^", "|",
            "~", "¨", "×", "÷", "←", "↑", "→", "↓", "∊", "∘", "∧", "∨", "∩", "∪", "∼", "≠",
            "≡", "≢", "≤", "≥", "⊂", "⊃", "⊖", "⊢", "⊣", "⊤", "⊥", "⋆", "⌈", "⌊", "⌶", "⌷",
            "⌹", "⌺", "⌽", "⌿", "⍀", "⍉", "⍋", "⍎", "⍒", "⍕", "⍙", "⍞", "⍟", "⍠", "⍣", "⍤",
            "⍥", "⍨", "⍪", "⍫", "⍬", "⍱", "⍲", "⍳", "⍴", "⍵", "⍶", "⍷", "⍸", "⍹", "⍺", "◊",
            "○", "$", "¥", "χ", "\\")
    }

    fun nextTokenOrSpace(): Token {
        if (!pushBackQueue.isEmpty()) {
            return pushBackQueue.removeAt(pushBackQueue.size - 1)
        }

        val ch = content.nextCodepoint() ?: return EndOfFile

        return when {
            isOpenParen(ch) -> OpenParen
            isCloseParen(ch) -> CloseParen
            isNegationSign(ch) -> collectNegativeNumber()
            isStatementSeparator(ch) -> StatementSeparator
            singleCharFunctions.contains(codepointToString(ch)) -> engine.internSymbol(codepointToString(ch))
            isDigit(ch) -> collectNumber(ch)
            isWhitespace(ch) -> Whitespace
            isLetter(ch) -> collectSymbol(ch)
            else -> throw UnexpectedSymbol(ch)
        }
    }

    fun pushBackToken(token: Token) {
        pushBackQueue.add(token)
    }

    private fun codepointToString(ch: Int): String {
        val buf = StringBuilder()
        buf.addCodepoint(ch)
        return buf.toString()
    }

    private fun isOpenParen(ch: Int) = ch == '('.toInt()
    private fun isCloseParen(ch: Int) = ch == ')'.toInt()
    private fun isStatementSeparator(ch: Int) = ch == '◊'.toInt()
    private fun isNegationSign(ch: Int) = ch == '¯'.toInt()

    private fun collectNumber(firstChar: Int, isNegative: Boolean = false): ParsedLong {
        val buf = StringBuilder()
        buf.addCodepoint(firstChar)
        while (true) {
            val ch = content.nextCodepoint() ?: break
            if (isLetter(ch)) {
                throw IllegalNumberFormat("Illegal number format")
            }
            if (!isDigit(ch)) {
                content.revertLastChars(1)
                break
            }
            buf.addCodepoint(ch)
        }
        return ParsedLong(buf.toString().toLong() * if (isNegative) -1 else 1)
    }

    private fun collectNegativeNumber(): ParsedLong {
        val ch = content.nextCodepoint()
        if (ch == null || !isDigit(ch)) {
            throw IllegalNumberFormat("Negation sign not followed by number")
        }
        return collectNumber(ch, true)
    }

    private fun collectSymbol(firstChar: Int): Symbol {
        val buf = StringBuilder()
        buf.addCodepoint(firstChar)
        while (true) {
            val ch = content.nextCodepoint() ?: break
            if (!isLetter(ch) || isDigit(ch)) {
                pos--
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
    fun evalWithContext(context: RuntimeContext): APLValue
}

class InstructionList(val instructions: List<Instruction>) : Instruction {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        var result: APLValue? = null
        for (instr in instructions) {
            result = instr.evalWithContext(context)
        }
        if (result == null) {
            throw IllegalStateException("Empty instruction list")
        }
        return result
    }
}

class FunctionCall1Arg(val fn: APLFunction, val rightArgs: Instruction) : Instruction {
    override fun evalWithContext(context: RuntimeContext) = fn.eval1Arg(context, rightArgs.evalWithContext(context))
    override fun toString() = "FunctionCall1Arg(fn=${fn}, rightArgs=${rightArgs})"
}

class FunctionCall2Arg(val fn: APLFunction, val leftArgs: Instruction, val rightArgs: Instruction) : Instruction {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val leftValue = rightArgs.evalWithContext(context)
        val rightValue = leftArgs.evalWithContext(context)
        return fn.eval2Arg(context, rightValue, leftValue)
    }

    override fun toString() = "FunctionCall2Arg(fn=${fn}, leftArgs=${leftArgs}, rightArgs=${rightArgs})"
}

class VariableRef(val name: Symbol) : Instruction {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        return context.lookupVar(name) ?: throw VariableNotAssigned(name)
    }

    override fun toString() = "Var(${name})"
}

class Literal1DArray(val values: List<Instruction>) : Instruction {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val size = values.size
        val result = Array<APLValue?>(size) { null }
        for (i in (size - 1) downTo 0) {
            result[i] = values[i].evalWithContext(context)
        }
        return APLArrayImpl(arrayOf(size)) { result[it]!! }
    }

    override fun toString() = "Literal1DArray(${values})"
}

class LiteralScalarValue(val value: Instruction) : Instruction {
    override fun evalWithContext(context: RuntimeContext) = value.evalWithContext(context)
    override fun toString() = "LiteralScalarValue(${value})"
}

class LiteralNumber(val value: Long) : Instruction {
    override fun evalWithContext(context: RuntimeContext) = APLLong(value)
    override fun toString() = "LiteralNumber(value=$value)"
}

fun parseValueToplevel(engine: Engine, tokeniser: TokenGenerator, endToken: Token): Instruction {
    val statementList = ArrayList<Instruction>()

    while (true) {
        val (instr, lastToken) = parseValue(engine, tokeniser)
        statementList.add(instr)
        if (lastToken == endToken) {
            assertx(!statementList.isEmpty())
            return if (statementList.size == 1) instr else InstructionList(statementList)
        } else if (lastToken != StatementSeparator) {
            throw UnexpectedToken(lastToken)
        }
    }
}

fun parseValue(engine: Engine, tokeniser: TokenGenerator): Pair<Instruction, Token> {
    val leftArgs = ArrayList<Instruction>()

    fun makeResultList(): Instruction {
        if (leftArgs.isEmpty()) {
            throw IllegalStateException("Argument list should not be empty")
        }
        return if (leftArgs.size == 1) {
            LiteralScalarValue(leftArgs[0])
        } else {
            Literal1DArray(leftArgs)
        }
    }

    while (true) {
        val token = tokeniser.nextToken()
        if (token == CloseParen || token == EndOfFile || token == StatementSeparator) {
            return Pair(makeResultList(), token)
        }

        when (token) {
            is Symbol -> {
                val fn = engine.getFunction(token)
                if (fn != null) {
                    val parsedFn = parseOperator(fn, engine, tokeniser)
                    val (rightValue, lastToken) = parseValue(engine, tokeniser)
                    return if (leftArgs.isEmpty()) {
                        Pair(FunctionCall1Arg(parsedFn, rightValue), lastToken)
                    } else {
                        Pair(FunctionCall2Arg(parsedFn, makeResultList(), rightValue), lastToken)
                    }
                } else {
                    leftArgs.add(VariableRef(token))
                }
            }
            is OpenParen -> leftArgs.add(parseValueToplevel(engine, tokeniser, CloseParen))
            is ParsedLong -> leftArgs.add(LiteralNumber(token.value))
            else -> throw UnexpectedToken(token)
        }
    }
}

fun parseOperator(fn: APLFunction, engine: Engine, tokeniser: TokenGenerator) : APLFunction {
    var currentFn = fn
    var token: Token
    while(true) {
        token = tokeniser.nextToken()
        if(token is Symbol) {
            val op = engine.getOperator(token) ?: break
            currentFn = op.combineFunction(currentFn, null)
        }
        else {
            break
        }
    }
    if(token != EndOfFile) {
        tokeniser.pushBackToken(token)
    }
    return currentFn
}
