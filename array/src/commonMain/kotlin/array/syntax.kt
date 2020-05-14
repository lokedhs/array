package array

class CustomSyntax(val triggerSymbol: Symbol, val rulesList: List<SyntaxRule>, val instr: Instruction, val pos: Position)

class SyntaxRuleVariableBinding(val name: Symbol, val value: Instruction)

interface SyntaxRule {
    fun processRule(parser: APLParser, bindings: MutableList<SyntaxRuleVariableBinding>)
}

class ConstantSyntaxRule(val symbolName: Symbol) : SyntaxRule {
    override fun processRule(parser: APLParser, bindings: MutableList<SyntaxRuleVariableBinding>) {
        val (sym, pos) = parser.tokeniser.nextTokenAndPosWithType<Symbol>()
        if (sym !== symbolName) {
            throw SyntaxRuleMismatch(symbolName, sym, pos)
        }
    }
}

class ValueSyntaxRule(val variable: Symbol) : SyntaxRule {
    override fun processRule(parser: APLParser, bindings: MutableList<SyntaxRuleVariableBinding>) {
        parser.tokeniser.nextTokenAndPosWithType<OpenParen>()
        val instr = parser.parseValueToplevel(CloseParen)
        bindings.add(SyntaxRuleVariableBinding(variable, instr))
    }
}

class FunctionSyntaxRule(val variable: Symbol) : SyntaxRule {
    override fun processRule(parser: APLParser, bindings: MutableList<SyntaxRuleVariableBinding>) {
        val (token, pos) = parser.tokeniser.nextTokenWithPosition()
        if (token !is OpenFnDef) {
            throw UnexpectedToken(token, pos)
        }
        val fnDefinition = parser.parseFnDefinition()
        bindings.add(SyntaxRuleVariableBinding(variable, APLParser.EvalLambdaFnx(fnDefinition.make(pos), pos)))
    }
}

class CallWithVarInstruction(val instr: Instruction, val bindings: List<SyntaxRuleVariableBinding>, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val newContext = context.link()
        bindings.forEach { b ->
            newContext.setVar(b.name, b.value.evalWithContext(context))
        }
        return instr.evalWithContext(newContext)
    }
}

fun processDefsyntax(parser: APLParser, pos: Position): Instruction {
    val tokeniser = parser.tokeniser
    val triggerSymbol = tokeniser.nextTokenWithType<Symbol>()
    tokeniser.nextTokenWithType<OpenParen>()

    fun processPair(curr: MutableList<SyntaxRule>, token: Symbol, pos: Position) {
        if (token.namespace !== tokeniser.engine.keywordNamespace) {
            throw ParseException("Tag is not a keyword: ${token.nameWithNamespace()}", pos)
        }
        when (token.symbolName) {
            "constant" -> curr.add(ConstantSyntaxRule(tokeniser.nextTokenWithType()))
            "value" -> curr.add(ValueSyntaxRule(tokeniser.nextTokenWithType()))
            "function" -> curr.add(FunctionSyntaxRule(tokeniser.nextTokenWithType()))
            else -> throw ParseException("Unexpected tag: ${token.nameWithNamespace()}")
        }
    }

    val rulesList = ArrayList<SyntaxRule>()
    while (true) {
        val (token, p) = tokeniser.nextTokenWithPosition()
        when (token) {
            is Symbol -> processPair(rulesList, token, p)
            is CloseParen -> break
        }
    }

    tokeniser.nextTokenWithType<OpenFnDef>()
    val instr = parser.parseValueToplevel(CloseFnDef)

    tokeniser.engine.registerCustomSyntax(CustomSyntax(triggerSymbol, rulesList, instr, pos))

    return LiteralSymbol(triggerSymbol, pos)
}

fun processCustomSyntax(parser: APLParser, customSyntax: CustomSyntax): Instruction {
    val bindings = ArrayList<SyntaxRuleVariableBinding>()
    customSyntax.rulesList.forEach { rule ->
        rule.processRule(parser, bindings)
    }
    return CallWithVarInstruction(customSyntax.instr, bindings, customSyntax.pos)
}
