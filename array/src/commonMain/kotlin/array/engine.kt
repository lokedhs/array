package array

import array.builtins.*

interface APLFunction {
    fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?) : APLValue
    fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?) : APLValue
}

abstract class NoAxisAPLFunction : APLFunction {
    override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?) : APLValue {
        return eval1Arg(context, a)
    }

    abstract fun eval1Arg(context: RuntimeContext, a: APLValue) : APLValue


    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?) : APLValue {
        return eval2Arg(context, a, b)
    }

    abstract fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue) : APLValue
}

class DeclaredFunction(val instruction: Instruction, val leftArgName: Symbol, val rightArgName: Symbol) : APLFunction {
    override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        val localContext = context.link()
        localContext.setVar(rightArgName, a)
        return instruction.evalWithContext(localContext)
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        val localContext = context.link()
        localContext.setVar(leftArgName, a)
        localContext.setVar(rightArgName, b)
        return instruction.evalWithContext(localContext)
    }
}

interface APLOperator {
    fun combineFunction(fn: APLFunction, operatorAxis: Instruction?): APLFunction
}

class Engine {
    private val functions = HashMap<Symbol,APLFunction>()
    private val operators = HashMap<Symbol,APLOperator>()
    private val symbols = HashMap<String,Symbol>()
    private val variables = HashMap<Symbol,APLValue>()

    init {
        // core functions
        registerFunction(internSymbol("+"), AddAPLFunction())
        registerFunction(internSymbol("-"), SubAPLFunction())
        registerFunction(internSymbol("×"), MulAPLFunction())
        registerFunction(internSymbol("÷"), DivAPLFunction())
        registerFunction(internSymbol("⋆"), PowerAPLFunction())
        registerFunction(internSymbol("⍟"), LogAPLFunction())
        registerFunction(internSymbol("⍳"), IotaAPLFunction())
        registerFunction(internSymbol("⍴"), RhoAPLFunction())
        registerFunction(internSymbol("⊢"), IdentityAPLFunction())
        registerFunction(internSymbol("⊣"), HideAPLFunction())
        registerFunction(internSymbol("="), EqualsAPLFunction())
        registerFunction(internSymbol("≠"), NotEqualsAPLFunction())
        registerFunction(internSymbol("<"), LessThanAPLFunction())
        registerFunction(internSymbol(">"), GreaterThanAPLFunction())
        registerFunction(internSymbol("≤"), LessThanEqualAPLFunction())
        registerFunction(internSymbol("≥"), GreaterThanEqualAPLFunction())
        registerFunction(internSymbol("⌷"), AccessFromIndexAPLFunction())
        registerFunction(internSymbol("⊂"), EncloseAPLFunction())
        registerFunction(internSymbol("⊃"), DiscloseAPLFunction())
        registerFunction(internSymbol("∧"), AndAPLFunction())
        registerFunction(internSymbol("∨"), OrAPLFunction())
        registerFunction(internSymbol(","), ConcatenateAPLFunction())
        registerFunction(internSymbol("↑"), TakeAPLFunction())
        registerFunction(internSymbol("?"), RandomAPLFunction())

        // io functions
        registerFunction(internSymbol("print"), PrintAPLFunction())

        // maths
        registerFunction(internSymbol("sin"), SinAPLFunction())
        registerFunction(internSymbol("cos"), CosAPLFunction())
        registerFunction(internSymbol("tan"), TanAPLFunction())
        registerFunction(internSymbol("asin"), AsinAPLFunction())
        registerFunction(internSymbol("acos"), AcosAPLFunction())
        registerFunction(internSymbol("atan"), AtanAPLFunction())

        // operators
        registerOperator(internSymbol("¨"), ForEachOp())
        registerOperator(internSymbol("/"), ReduceOp())
    }

    fun registerFunction(name: Symbol, fn: APLFunction) {
        functions[name] = fn
    }

    fun registerOperator(name: Symbol, fn: APLOperator) {
        operators[name] = fn
    }

    fun getFunction(name: Symbol) = functions[name]
    fun getOperator(token: Symbol) = operators[token]
    fun parseString(input: String) = parseValueToplevel(this, TokenGenerator(this, StringCharacterProvider(input)), EndOfFile)
    fun internSymbol(name: String): Symbol = symbols.getOrPut(name, {Symbol(name)})
    fun lookupVar(name: Symbol): APLValue? = variables[name]
    fun makeRuntimeContext() = RuntimeContext(this, null)
}

class RuntimeContext(val engine: Engine, val parent: RuntimeContext?) {
    private val localVariables = HashMap<Symbol,APLValue>()

    fun lookupVar(name: Symbol): APLValue? {
        val result = localVariables[name]
        return when {
            result != null -> result
            parent != null -> parent.lookupVar(name)
            else -> engine.lookupVar(name)
        }
    }

    fun setVar(name: Symbol, value: APLValue) {
        localVariables[name] = value
    }

    fun link() = RuntimeContext(engine, this)
}
