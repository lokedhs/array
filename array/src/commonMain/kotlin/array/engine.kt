package array

import array.builtins.*

interface APLFunction {
    fun eval1Arg(context: RuntimeContext, arg: APLValue, axis: APLValue?) : APLValue
    fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue, axis: APLValue?) : APLValue
}

abstract class NoAxisAPLFunction : APLFunction {
    override fun eval1Arg(context: RuntimeContext, arg: APLValue, axis: APLValue?) : APLValue {
        return eval1Arg(context, arg)
    }

    abstract fun eval1Arg(context: RuntimeContext, arg: APLValue) : APLValue


    override fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue, axis: APLValue?) : APLValue {
        return eval2Arg(context, arg1, arg2)
    }

    abstract fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue) : APLValue
}

class DeclaredFunction(val instruction: Instruction) : APLFunction {
    override fun eval1Arg(context: RuntimeContext, arg: APLValue, axis: APLValue?): APLValue {
        val localContext = context.link()
        localContext.setVar(context.engine.internSymbol("⍵"), arg)
        return instruction.evalWithContext(localContext)
    }

    override fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue, axis: APLValue?): APLValue {
        val localContext = context.link()
        localContext.setVar(context.engine.internSymbol("⍺"), arg1)
        localContext.setVar(context.engine.internSymbol("⍵"), arg2)
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
        registerFunction(internSymbol("⊂"), EncloseAPLFunction())
        registerFunction(internSymbol("⊃"), DiscloseAPLFunction())

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
