package array

class CustomSyntax(val triggerSymbol: Symbol, val rulesList: List<SyntaxRule>, val instr: Instruction, val pos: Position)

class SyntaxRuleVariableBinding(val name: Symbol, val value: Instruction)

interface SyntaxRule {
    fun isValid(token: Token): Boolean
    fun processRule(parser: APLParser, bindings: MutableList<SyntaxRuleVariableBinding>)
}

class ConstantSyntaxRule(val symbolName: Symbol) : SyntaxRule {
    override fun isValid(token: Token) = token === symbolName

    override fun processRule(parser: APLParser, bindings: MutableList<SyntaxRuleVariableBinding>) {
        val (sym, pos) = parser.tokeniser.nextTokenAndPosWithType<Symbol>()
        if (sym !== symbolName) {
            throw SyntaxRuleMismatch(symbolName, sym, pos)
        }
    }
}

class ValueSyntaxRule(val variable: Symbol) : SyntaxRule {
    override fun isValid(token: Token) = token is OpenParen

    override fun processRule(parser: APLParser, bindings: MutableList<SyntaxRuleVariableBinding>) {
        parser.tokeniser.nextTokenWithType<OpenParen>()
        val instr = parser.parseValueToplevel(CloseParen)
        bindings.add(SyntaxRuleVariableBinding(variable, instr))
    }
}

class FunctionSyntaxRule(val variable: Symbol) : SyntaxRule {
    override fun isValid(token: Token) = token is OpenFnDef

    override fun processRule(parser: APLParser, bindings: MutableList<SyntaxRuleVariableBinding>) {
        val (token, pos) = parser.tokeniser.nextTokenWithPosition()
        if (token !is OpenFnDef) {
            throw UnexpectedToken(token, pos)
        }
        val fnDefinition = parser.parseFnDefinition()
        bindings.add(SyntaxRuleVariableBinding(variable, APLParser.EvalLambdaFnx(fnDefinition.make(pos), pos)))
    }
}

class OptionalSyntaxRule(val initialRule: SyntaxRule, val rest: List<SyntaxRule>) : SyntaxRule {
    override fun isValid(token: Token) = initialRule.isValid(token)

    override fun processRule(parser: APLParser, bindings: MutableList<SyntaxRuleVariableBinding>) {
        if (initialRule.isValid(parser.tokeniser.peekToken())) {
            initialRule.processRule(parser, bindings)
            rest.forEach { rule ->
                rule.processRule(parser, bindings)
            }
        }
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

private fun processPair(tokeniser: TokenGenerator, curr: MutableList<SyntaxRule>, token: Symbol, pos: Position) {
    if (token.namespace !== tokeniser.engine.keywordNamespace) {
        throw ParseException("Tag is not a keyword: ${token.nameWithNamespace()}", pos)
    }
    when (token.symbolName) {
        "constant" -> curr.add(ConstantSyntaxRule(tokeniser.nextTokenWithType()))
        "value" -> curr.add(ValueSyntaxRule(tokeniser.nextTokenWithType()))
        "function" -> curr.add(FunctionSyntaxRule(tokeniser.nextTokenWithType()))
        "optional" -> curr.add(processOptional(tokeniser))
        else -> throw ParseException("Unexpected tag: ${token.nameWithNamespace()}")
    }
}

private fun processPairs(tokeniser: TokenGenerator): ArrayList<SyntaxRule> {
    val rulesList = ArrayList<SyntaxRule>()
    tokeniser.iterateUntilToken(CloseParen) { token, pos ->
        when (token) {
            is Symbol -> processPair(tokeniser, rulesList, token, pos)
            else -> throw UnexpectedToken(token, pos)
        }
    }
    return rulesList
}

private fun processOptional(tokeniser: TokenGenerator): OptionalSyntaxRule {
    tokeniser.nextTokenWithType<OpenParen>()
    val rulesList = processPairs(tokeniser)
    if (rulesList.isEmpty()) {
        throw ParseException("Optional syntax rules must have at least one rule")
    }
    return OptionalSyntaxRule(rulesList[0], rulesList.drop(1))
}

fun processDefsyntax(parser: APLParser, pos: Position): Instruction {
    val tokeniser = parser.tokeniser
    val triggerSymbol = tokeniser.nextTokenWithType<Symbol>()
    tokeniser.nextTokenWithType<OpenParen>()

    val rulesList = processPairs(tokeniser)

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
