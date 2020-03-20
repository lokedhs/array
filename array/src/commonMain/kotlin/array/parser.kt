package array

import array.complex.Complex

abstract class Token

object Whitespace : Token()
object EndOfFile : Token()
object OpenParen : Token()
object CloseParen : Token()
object OpenFnDef : Token()
object CloseFnDef : Token()
object OpenBracket : Token()
object CloseBracket : Token()
object StatementSeparator : Token()
object LeftArrow : Token()
object FnDefSym : Token()
object APLNullSym : Token()
object QuotePrefix : Token()

class Symbol(val symbolName: String) : Token(), Comparable<Symbol> {
    override fun toString() = "Symbol[name=${symbolName}]"
    override fun compareTo(other: Symbol) = symbolName.compareTo(other.symbolName)
    override fun hashCode() = symbolName.hashCode()
    override fun equals(other: Any?) = other != null && other is Symbol && symbolName == other.symbolName
}

class StringToken(val value: String) : Token()
class ParsedLong(val value: Long) : Token()
class ParsedDouble(val value: Double) : Token()
class ParsedComplex(val value: Complex) : Token()

class Position(val sourceText: String, val line: Int, val col: Int)

class TokenGenerator(val engine: Engine, contentArg: CharacterProvider) {
    private val content = PushBackCharacterProvider(contentArg)
    private val singleCharFunctions: Set<String>
    private val pushBackQueue = ArrayList<Token>()

    private val charToTokenMap = hashMapOf(
        "(" to OpenParen,
        ")" to CloseParen,
        "{" to OpenFnDef,
        "}" to CloseFnDef,
        "[" to OpenBracket,
        "]" to CloseBracket,
        "←" to LeftArrow,
        "◊" to StatementSeparator,
        "∇" to FnDefSym,
        "⍬" to APLNullSym
    )

    init {
        singleCharFunctions = hashSetOf(
            "!", "#", "%", "&", "*", "+", ",", "-", "/", "<", "=", ">", "?", "@", "^", "|",
            "~", "¨", "×", "÷", "↑", "→", "↓", "∊", "∘", "∧", "∨", "∩", "∪", "∼", "≠", "≡",
            "≢", "≤", "≥", "⊂", "⊃", "⊖", "⊢", "⊣", "⊤", "⊥", "⋆", "⌈", "⌊", "⌶", "⌷", "⌹",
            "⌺", "⌽", "⌿", "⍀", "⍉", "⍋", "⍎", "⍒", "⍕", "⍙", "⍞", "⍟", "⍠", "⍣", "⍤", "⍥",
            "⍨", "⍪", "⍫", "⍱", "⍲", "⍳", "⍴", "⍵", "⍶", "⍷", "⍸", "⍹", "⍺", "◊",
            "○", "$", "¥", "χ", "\\"
        )
    }

    inline fun <reified T : Token> nextTokenWithType(): T {
        val token = nextToken()
        if (token is T) {
            return token
        } else {
            throw UnexpectedToken(token)
        }
    }

    fun nextTokenOrSpace(): Pair<Token, Position> {
        val posBeforeParse = content.pos()
        fun mkpos(token: Token) = Pair(token, posBeforeParse)

        if (!pushBackQueue.isEmpty()) {
            return mkpos(pushBackQueue.removeAt(pushBackQueue.size - 1))
        }

        val ch = content.nextCodepoint() ?: return mkpos(EndOfFile)

        charToTokenMap[charToString(ch)]?.also { return mkpos(it) }

        return mkpos(
            when {
                singleCharFunctions.contains(charToString(ch)) -> engine.internSymbol(charToString(ch))
                isNegationSign(ch) || isDigit(ch) -> {
                    content.pushBack(); collectNumber()
                }
                isWhitespace(ch) -> Whitespace
                isLetter(ch) -> collectSymbol(ch)
                isQuoteChar(ch) -> collectString()
                isCommentChar(ch) -> skipUntilNewline()
                isQuotePrefixChar(ch) -> QuotePrefix
                else -> throw UnexpectedSymbol(ch)
            }
        )
    }

    fun pushBackToken(token: Token) {
        pushBackQueue.add(token)
    }

    private fun isNegationSign(ch: Int) = ch == '¯'.toInt()
    private fun isQuoteChar(ch: Int) = ch == '"'.toInt()
    private fun isCommentChar(ch: Int) = ch == '⍝'.toInt()
    private fun isSymbolStartChar(ch: Int) = isLetter(ch) || ch == '_'.toInt()
    private fun isSymbolContinuation(ch: Int) = isSymbolStartChar(ch) || isDigit(ch)
    private fun isNumericConstituent(ch: Int) =
        isDigit(ch) || isNegationSign(ch) || ch == '.'.toInt() || ch == 'j'.toInt() || ch == 'J'.toInt()

    private fun isQuotePrefixChar(ch: Int) = ch == '\''.toInt()

    private fun skipUntilNewline(): Whitespace {
        while (true) {
            val ch = content.nextCodepoint()
            if (ch == null || ch == '\n'.toInt()) {
                break
            }
        }
        return Whitespace
    }

    private fun collectNumber(): Token {
        val buf = StringBuilder()
        var foundComplex = false
        loop@ while (true) {
            val ch = content.nextCodepoint() ?: break
            when {
                ch == 'j'.toInt() || ch == 'J'.toInt() -> {
                    if (foundComplex) {
                        throw IllegalNumberFormat("Garbage after number")
                    }
                    foundComplex = true
                }
                isLetter(ch) -> throw IllegalNumberFormat("Garbage after number")
                !isNumericConstituent(ch) -> {
                    content.pushBack()
                    break@loop
                }
            }
            buf.addCodepoint(ch)
        }

        val s = buf.toString()
        for (parser in NUMBER_PARSERS) {
            val result = parser.process(s)
            if (result != null) {
                return result
            }
        }
        throw IllegalNumberFormat("Content cannot be parsed as a number")
    }

    private fun collectSymbol(firstChar: Int): Symbol {
        val buf = StringBuilder()
        buf.addCodepoint(firstChar)
        while (true) {
            val ch = content.nextCodepoint() ?: break
            if (!isSymbolContinuation(ch)) {
                content.pushBack()
                break
            }
            buf.addCodepoint(ch)
        }
        return engine.internSymbol(buf.toString())
    }

    private fun collectString(): Token {
        val buf = StringBuilder()
        while (true) {
            val ch = content.nextCodepoint() ?: throw ParseException("End of input in the middle of string")
            if (ch == '"'.toInt()) {
                break
            } else if (ch == '\\'.toInt()) {
                val next = content.nextCodepoint() ?: throw ParseException("End of input in the middle of string")
                buf.addCodepoint(next)
            } else {
                buf.addCodepoint(ch)
            }
        }
        return StringToken(buf.toString())
    }

    fun nextToken(): Token {
        return nextTokenWithPosition().first
    }

    fun nextTokenWithPosition(): Pair<Token, Position> {
        while (true) {
            val tokenAndPos = nextTokenOrSpace()
            if (tokenAndPos.first != Whitespace) {
                return tokenAndPos
            }
        }
    }

    private class NumberParser(val pattern: Regex, val fn: (MatchResult) -> Token) {
        fun process(s: String): Token? {
            val result = pattern.matchEntire(s)
            return if (result == null) {
                null
            } else {
                fn(result)
            }
        }
    }

    companion object {
        private fun withNeg(isNegative: Boolean, s: String) = if (isNegative) "-$s" else s

        private val NUMBER_PARSERS = listOf(
            NumberParser("^(¯?)([0-9]+\\.[0-9]*)\$".toRegex()) { result ->
                val groups = result.groups
                val sign = groups.get(1) ?: throw IllegalNumberFormat("Illegal format of sign")
                val s = groups.get(2) ?: throw IllegalNumberFormat("Illegal format of number part")
                ParsedDouble(withNeg(sign.value != "", s.value).toDouble())
            },
            NumberParser("^(¯?)([0-9]+)$".toRegex()) { result ->
                val groups = result.groups
                val sign = groups.get(1) ?: throw IllegalNumberFormat("Illegal format of sign")
                val s = groups.get(2) ?: throw IllegalNumberFormat("Illegal format of number part")
                ParsedLong(withNeg(sign.value != "", s.value).toLong())
            },
            NumberParser("^(¯?)([0-9]+(?:\\.[0-9]*)?)[jJ](¯?)([0-9]+(?:\\.[0-9]*)?)$".toRegex()) { result ->
                val groups = result.groups
                val realSign = groups.get(1) ?: throw IllegalNumberFormat("Illegal format of sign in real part")
                val realS = groups.get(2) ?: throw IllegalNumberFormat("Illegal format of number in real part")
                val complexSign = groups.get(3) ?: throw IllegalNumberFormat("Illegal format of sign in complex")
                val complexS = groups.get(4) ?: throw IllegalNumberFormat("Illegal format of number in complex part")
                ParsedComplex(
                    Complex(
                        withNeg(realSign.value != "", realS.value).toDouble(),
                        withNeg(complexSign.value != "", complexS.value).toDouble()
                    )
                )
            }
        )
    }
}

abstract class Instruction(val pos: Position) {
    abstract fun evalWithContext(context: RuntimeContext): APLValue
}

class InstructionList(val instructions: List<Instruction>) : Instruction(instructions[0].pos) {
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

class FunctionCall1Arg(
    val fn: APLFunction,
    val rightArgs: Instruction,
    val axis: Instruction?,
    pos: Position
) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) =
        fn.eval1Arg(context, rightArgs.evalWithContext(context), axis?.evalWithContext(context))

    override fun toString() = "FunctionCall1Arg(fn=${fn}, rightArgs=${rightArgs})"
}

class FunctionCall2Arg(
    val fn: APLFunction,
    val leftArgs: Instruction,
    val rightArgs: Instruction,
    val axis: Instruction?,
    pos: Position
) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val leftValue = rightArgs.evalWithContext(context)
        val rightValue = leftArgs.evalWithContext(context)
        val axisValue = axis?.evalWithContext(context)
        return fn.eval2Arg(context, rightValue, leftValue, axisValue)
    }

    override fun toString() = "FunctionCall2Arg(fn=${fn}, leftArgs=${leftArgs}, rightArgs=${rightArgs})"
}

class VariableRef(val name: Symbol, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        return context.lookupVar(name) ?: throw VariableNotAssigned(name)
    }

    override fun toString() = "Var(${name})"
}

class Literal1DArray(val values: List<Instruction>) : Instruction(values[0].pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val size = values.size
        val result = Array<APLValue?>(size) { null }
        for (i in (size - 1) downTo 0) {
            result[i] = values[i].evalWithContext(context)
        }
        return APLArrayImpl(dimensionsOfSize(size)) { result[it]!! }
    }

    override fun toString() = "Literal1DArray(${values})"
}

class LiteralScalarValue(val value: Instruction) : Instruction(value.pos) {
    override fun evalWithContext(context: RuntimeContext) = value.evalWithContext(context)
    override fun toString() = "LiteralScalarValue(${value})"
}

class LiteralInteger(val value: Long, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) = APLLong(value)
    override fun toString() = "LiteralInteger[value=$value]"
}

class LiteralDouble(val value: Double, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) = APLDouble(value)
    override fun toString() = "LiteralDouble[value=$value]"
}

class LiteralComplex(val value: Complex, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) = value.makeAPLNumber()
    override fun toString() = "LiteralComplex[value=$value]"
}

class LiteralSymbol(name: Symbol, pos: Position) : Instruction(pos) {
    private val value = APLSymbol(name)
    override fun evalWithContext(context: RuntimeContext): APLValue = value
}

class LiteralAPLNullValue(pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) = APLNullValue()
}

class LiteralStringValue(val s: String, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext) = makeAPLString(s)
}

class AssignmentInstruction(val name: Symbol, val instr: Instruction, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val res = instr.evalWithContext(context)
        context.setVar(name, res)
        return res
    }
}

class UserFunction(private val arg: Symbol, private val instr: Instruction) : APLFunctionDescriptor {
    class UserFunctionImpl(
        private val arg: Symbol,
        private val instr: Instruction,
        pos: Position
    ) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            val inner = context.link()
            inner.setVar(arg, a)
            return instr.evalWithContext(inner)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            TODO("not implemented")
        }
    }

    override fun make(pos: Position) = UserFunctionImpl(arg, instr, pos)
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
            throw ParseException("Argument list should not be empty")
        }
        return if (leftArgs.size == 1) {
            LiteralScalarValue(leftArgs[0])
        } else {
            Literal1DArray(leftArgs)
        }
    }

    fun processFn(fn: APLFunctionDescriptor, pos: Position): Pair<Instruction, Token> {
        val axis = parseAxis(engine, tokeniser)
        val parsedFn = parseOperator(fn, engine, tokeniser)
        val (rightValue, lastToken) = parseValue(engine, tokeniser)
        return if (leftArgs.isEmpty()) {
            Pair(FunctionCall1Arg(parsedFn.make(pos), rightValue, axis, pos), lastToken)
        } else {
            Pair(FunctionCall2Arg(parsedFn.make(pos), makeResultList(), rightValue, axis, pos), lastToken)
        }
    }

    fun processAssignment(engine: Engine, tokeniser: TokenGenerator, pos: Position): Pair<Instruction, Token> {
        // Ensure that the left argument to leftarrow is a single symbol
        unless(leftArgs.size == 1) {
            throw IncompatibleTypeException("Can only assign to a single variable")
        }
        val dest = leftArgs[0]
        if (dest !is VariableRef) {
            throw IncompatibleTypeException("Attempt to assign to a type which is not a variable")
        }
        val (rightValue, lastToken) = parseValue(engine, tokeniser)
        return Pair(AssignmentInstruction(dest.name, rightValue, pos), lastToken)
    }

    fun processFunctionDefinition(engine: Engine, tokeniser: TokenGenerator, pos: Position): Instruction {
        if (!leftArgs.isEmpty()) {
            throw ParseException("Function definition with non-null left argument")
        }

        val name = tokeniser.nextTokenWithType<Symbol>()
        val arg = tokeniser.nextTokenWithType<Symbol>()
        // Read the opening brace
        tokeniser.nextTokenWithType<OpenFnDef>()
        // Parse like a normal function definition
        val instr = parseValueToplevel(engine, tokeniser, CloseFnDef)

        val obj = UserFunction(arg, instr)

        engine.registerFunction(name, obj)
        return LiteralSymbol(name, pos)
    }

    while (true) {
        val (token, pos) = tokeniser.nextTokenWithPosition()
        if (token == CloseParen || token == EndOfFile || token == StatementSeparator || token == CloseFnDef || token == CloseBracket) {
            return Pair(makeResultList(), token)
        }

        when (token) {
            is Symbol -> {
                val fn = engine.getFunction(token)
                if (fn != null) {
                    return processFn(fn, pos)
                } else {
                    leftArgs.add(VariableRef(token, pos))
                }
            }
            is OpenFnDef -> return processFn(parseFnDefinition(engine, tokeniser, pos), pos)
            is OpenParen -> leftArgs.add(parseValueToplevel(engine, tokeniser, CloseParen))
            is ParsedLong -> leftArgs.add(LiteralInteger(token.value, pos))
            is ParsedDouble -> leftArgs.add(LiteralDouble(token.value, pos))
            is ParsedComplex -> leftArgs.add(LiteralComplex(token.value, pos))
            is LeftArrow -> return processAssignment(engine, tokeniser, pos)
            is FnDefSym -> leftArgs.add(processFunctionDefinition(engine, tokeniser, pos))
            is APLNullSym -> leftArgs.add(LiteralAPLNullValue(pos))
            is StringToken -> leftArgs.add(LiteralStringValue(token.value, pos))
            is QuotePrefix -> leftArgs.add(LiteralSymbol(tokeniser.nextTokenWithType(), pos))
            else -> throw UnexpectedToken(token)
        }
    }
}

fun parseFnDefinition(
    engine: Engine,
    tokeniser: TokenGenerator,
    pos: Position,
    leftArgName: Symbol? = null,
    rightArgName: Symbol? = null
): DeclaredFunction {
    val instruction = parseValueToplevel(engine, tokeniser, CloseFnDef)
    return DeclaredFunction(instruction, leftArgName ?: engine.internSymbol("⍺"), rightArgName ?: engine.internSymbol("⍵"), pos)
}

fun parseOperator(fn: APLFunctionDescriptor, engine: Engine, tokeniser: TokenGenerator): APLFunctionDescriptor {
    var currentFn = fn
    var token: Token
    while (true) {
        token = tokeniser.nextToken()
        if (token is Symbol) {
            val op = engine.getOperator(token) ?: break
            val axis = parseAxis(engine, tokeniser)
            currentFn = op.combineFunction(currentFn, axis)
        } else {
            break
        }
    }
    if (token != EndOfFile) {
        tokeniser.pushBackToken(token)
    }
    return currentFn
}

fun parseAxis(engine: Engine, tokeniser: TokenGenerator): Instruction? {
    val token = tokeniser.nextToken()
    if (token != OpenBracket) {
        tokeniser.pushBackToken(token)
        return null
    }

    return parseValueToplevel(engine, tokeniser, CloseBracket)
}
