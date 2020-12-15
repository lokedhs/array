package array.syntax

import array.*

class CustomSyntax(
    val name: Symbol,
    val environment: Environment,
    val rulesList: List<SyntaxRule>,
    val instr: Instruction,
    val pos: Position)

class SyntaxRuleVariableBinding(val name: EnvironmentBinding, val value: Instruction)

interface SyntaxRule {
    fun isValid(token: Token): Boolean
    fun processRule(parser: APLParser, syntaxRuleBindings: MutableList<SyntaxRuleVariableBinding>)
}

class ConstantSyntaxRule(val symbolName: Symbol) : SyntaxRule {
    override fun isValid(token: Token) = token === symbolName

    override fun processRule(parser: APLParser, syntaxRuleBindings: MutableList<SyntaxRuleVariableBinding>) {
        val (sym, pos) = parser.tokeniser.nextTokenAndPosWithType<Symbol>()
        if (sym !== symbolName) {
            throw SyntaxRuleMismatch(symbolName, sym, pos)
        }
    }
}

class ValueSyntaxRule(val variable: EnvironmentBinding) : SyntaxRule {
    override fun isValid(token: Token) = token is OpenParen

    override fun processRule(parser: APLParser, syntaxRuleBindings: MutableList<SyntaxRuleVariableBinding>) {
        parser.tokeniser.nextTokenWithType<OpenParen>()
        val instr = parser.parseValueToplevel(CloseParen)
        syntaxRuleBindings.add(SyntaxRuleVariableBinding(variable, instr))
    }
}

/*
      defsyntax foo (:function X) {
        ⍞X 1
      }

      foo { print ⍵ }
 */

abstract class FunctionSyntaxRule(private val variable: EnvironmentBinding) : SyntaxRule {
    override fun isValid(token: Token) = token == startToken()

    override fun processRule(parser: APLParser, syntaxRuleBindings: MutableList<SyntaxRuleVariableBinding>) {
        val (token, pos) = parser.tokeniser.nextTokenWithPosition()
        if (token != startToken()) {
            throw UnexpectedToken(token, pos)
        }
        val fnDefinition = parser.parseFnDefinition(endToken = endToken(), allocateEnvironment = allocateEnvironment())
        syntaxRuleBindings.add(
            SyntaxRuleVariableBinding(
                variable,
                APLParser.EvalLambdaFnx(fnDefinition.make(pos), pos)))
    }

    abstract fun startToken(): Token
    abstract fun endToken(): Token
    abstract fun allocateEnvironment(): Boolean
}

/**
 * Syntax rule that describes a function delimited by braces and which allocates a new environment.
 */
class BFunctionSyntaxRule(variable: EnvironmentBinding) : FunctionSyntaxRule(variable) {
    override fun startToken() = OpenFnDef
    override fun endToken() = CloseFnDef
    override fun allocateEnvironment() = true
}

/**
 * Syntax rule that describes a function delimited by braces and does not allocate a new environment.
 */
class NFunctionSyntaxRule(variable: EnvironmentBinding) : FunctionSyntaxRule(variable) {
    override fun startToken() = OpenFnDef
    override fun endToken() = CloseFnDef
    override fun allocateEnvironment() = false
}

/**
 * Syntax rule that describes a function delimited by parentheses and which allocates a new environment.
 */
class ExprFunctionSyntaxRule(variable: EnvironmentBinding) : FunctionSyntaxRule(variable) {
    override fun startToken() = OpenParen
    override fun endToken() = CloseParen
    override fun allocateEnvironment() = true
}

/**
 * Syntax rule that describes a function delimited by parentheses and does not allocate a new environment
 */
class NExprFunctionSyntaxRule(variable: EnvironmentBinding) : FunctionSyntaxRule(variable) {
    override fun startToken() = OpenParen
    override fun endToken() = CloseParen
    override fun allocateEnvironment() = false
}

class OptionalSyntaxRule(val initialRule: SyntaxRule, val rest: List<SyntaxRule>) : SyntaxRule {
    override fun isValid(token: Token) = initialRule.isValid(token)

    override fun processRule(parser: APLParser, syntaxRuleBindings: MutableList<SyntaxRuleVariableBinding>) {
        if (initialRule.isValid(parser.tokeniser.peekToken())) {
            initialRule.processRule(parser, syntaxRuleBindings)
            rest.forEach { rule ->
                rule.processRule(parser, syntaxRuleBindings)
            }
        }
    }
}

class MultiResultInstr(val instructionList: List<Instruction>, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val valueList = Array(instructionList.size) { i ->
            instructionList[i].evalWithContext(context)
        }
        return APLArrayImpl(dimensionsOfSize(valueList.size), valueList)
    }
}

class RepeatSyntaxRule(val name: EnvironmentBinding, val customSyntax: CustomSyntax) : SyntaxRule {
    override fun isValid(token: Token) = customSyntax.rulesList.first().isValid(token)

    override fun processRule(parser: APLParser, syntaxRuleBindings: MutableList<SyntaxRuleVariableBinding>) {
        val tokeniser = parser.tokeniser
        val instructions = ArrayList<Instruction>()
        while (customSyntax.rulesList.first().isValid(tokeniser.peekToken())) {
//            val innerBindings = ArrayList<SyntaxRuleVariableBinding>()
//            initialRule.processRule(parser, innerBindings)
//            rest.forEach { it.processRule(parser, innerBindings) }
//            instructions.add()
            instructions.add(processCustomSyntax(parser, customSyntax))
        }
        syntaxRuleBindings.add(SyntaxRuleVariableBinding(name, MultiResultInstr(instructions, customSyntax.pos)))
    }
}

private fun processPair(parser: APLParser, curr: MutableList<SyntaxRule>, token: Symbol, pos: Position) {
    val tokeniser = parser.tokeniser
    if (token.namespace !== tokeniser.engine.keywordNamespace) {
        throw ParseException("Tag is not a keyword: ${token.nameWithNamespace()}", pos)
    }
    when (token.symbolName) {
        "constant" -> curr.add(ConstantSyntaxRule(tokeniser.nextTokenWithType()))
        "value" -> curr.add(ValueSyntaxRule(parser.currentEnvironment().bindLocal(tokeniser.nextTokenWithType())))
        "function" -> curr.add(BFunctionSyntaxRule(parser.currentEnvironment().bindLocal(tokeniser.nextTokenWithType())))
        "nfunction" -> curr.add(NFunctionSyntaxRule(parser.currentEnvironment().bindLocal(tokeniser.nextTokenWithType())))
        "exprfunction" -> curr.add(ExprFunctionSyntaxRule(parser.currentEnvironment().bindLocal(tokeniser.nextTokenWithType())))
        "nexprfunction" -> curr.add(NExprFunctionSyntaxRule(parser.currentEnvironment().bindLocal(tokeniser.nextTokenWithType())))
        "optional" -> curr.add(processOptional(parser))
        "repeat" -> curr.add(processRepeat(parser))
        else -> throw ParseException("Unexpected tag: ${token.nameWithNamespace()}")
    }
}

private fun processPairs(parser: APLParser): ArrayList<SyntaxRule> {
    parser.tokeniser.nextTokenWithType<OpenParen>()
    val rulesList = ArrayList<SyntaxRule>()
    parser.tokeniser.iterateUntilToken(CloseParen) { token, pos ->
        when (token) {
            is Symbol -> processPair(parser, rulesList, token, pos)
            else -> throw UnexpectedToken(token, pos)
        }
    }
    return rulesList
}

private fun processOptional(parser: APLParser): OptionalSyntaxRule {
    parser.tokeniser.nextTokenWithType<OpenParen>()
    val rulesList = processPairs(parser)
    if (rulesList.isEmpty()) {
        throw ParseException("Optional syntax rules must have at least one rule")
    }
    return OptionalSyntaxRule(rulesList[0], rulesList.drop(1))
}

private fun processRepeat(parser: APLParser): SyntaxRule {
    val tokeniser = parser.tokeniser
    tokeniser.nextTokenWithType<OpenParen>()
    val name = parser.currentEnvironment().bindLocal(tokeniser.nextTokenWithType())
    val (subName, subNamePos) = tokeniser.nextTokenAndPosWithType<Symbol>()
    tokeniser.nextTokenWithType<CloseParen>()

    val subRules = tokeniser.engine.customSyntaxSubRulesForSymbol(subName) ?: throw SyntaxSubRuleNotFound(subName, subNamePos)
    return RepeatSyntaxRule(name, subRules)
}

fun processDefsyntaxSub(parser: APLParser, pos: Position) {
    parser.withEnvironment {
        val tokeniser = parser.tokeniser
        val name = tokeniser.nextTokenWithType<Symbol>()
        val rulesList = processPairs(parser)
        tokeniser.nextTokenWithType<OpenFnDef>()
        val instr = parser.parseValueToplevel(CloseFnDef)
        tokeniser.engine.registerCustomSyntaxSub(CustomSyntax(name, parser.currentEnvironment(), rulesList, instr, pos))
    }
}

fun processDefsyntax(parser: APLParser, pos: Position): Instruction {
    parser.withEnvironment {
        val tokeniser = parser.tokeniser
        val triggerSymbol = tokeniser.nextTokenWithType<Symbol>()
        val rulesList = processPairs(parser)
        tokeniser.nextTokenWithType<OpenFnDef>()
        val instr = parser.parseValueToplevel(CloseFnDef)
        tokeniser.engine.registerCustomSyntax(CustomSyntax(triggerSymbol, parser.currentEnvironment(), rulesList, instr, pos))
        return LiteralSymbol(triggerSymbol, pos)
    }
}

class CallWithVarInstruction(
    val instr: Instruction,
    val env: Environment,
    val bindings: List<Pair<EnvironmentBinding, Instruction>>,
    pos: Position
) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val newContext = context.link(env)
        bindings.forEach { (envBinding, instr) ->
            newContext.setVar(envBinding, instr.evalWithContext(context))
        }
        return instr.evalWithContext(newContext)
    }
}

fun processCustomSyntax(parser: APLParser, customSyntax: CustomSyntax): Instruction {
    val bindings = ArrayList<SyntaxRuleVariableBinding>()
    customSyntax.rulesList.forEach { rule ->
        rule.processRule(parser, bindings)
    }
    val envBindings = bindings.map { b -> Pair(b.name, b.value) }
    return CallWithVarInstruction(customSyntax.instr, customSyntax.environment, envBindings, customSyntax.pos)
}
