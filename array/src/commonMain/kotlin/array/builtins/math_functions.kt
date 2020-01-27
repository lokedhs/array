package array.builtins

import array.*

interface CellSumFunction1Arg {
    fun combine(a: APLValue): APLValue
}

interface CellSumFunction2Args {
    fun combineValues(a: APLValue, b: APLValue): APLValue
}

class ArraySum1Arg(
    private val fn: CellSumFunction1Arg,
    private val a: APLValue
) : APLArray() {
    override fun dimensions() = a.dimensions()
    override fun size() = a.size()
    override fun valueAt(p: Int) = fn.combine(a.valueAt(p))
}

class ArraySum2Args(
    private val fn: CellSumFunction2Args,
    private val a: APLValue,
    private val b: APLValue
) : APLArray() {

    init {
        assertx(Arrays.equals(a.dimensions(), b.dimensions()))
    }

    override fun dimensions() = a.dimensions() // Both arrays are of the same dimension
    override fun size() = a.size()

    override fun valueAt(p: Int): APLValue {
        return fn.combineValues(a.valueAt(p), b.valueAt(p))
    }
}

abstract class MathCombineAPLFunction : APLFunction {
    override fun eval1Arg(context: RuntimeContext, arg: APLValue): APLValue {
        val fn = object : CellSumFunction1Arg {
            override fun combine(a: APLValue): APLValue {
                return combine1Arg(a)
            }
        }
        return ArraySum1Arg(fn, arg)
    }

    override fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue): APLValue {
        val fn = object : CellSumFunction2Args {
            override fun combineValues(a: APLValue, b: APLValue): APLValue {
                return combine2Arg(a, b)
            }
        }
        return ArraySum2Args(fn, arg1, arg2)
    }

    abstract fun combine1Arg(a: APLValue): APLValue
    abstract fun combine2Arg(a: APLValue, b: APLValue): APLValue
}

class AddAPLFunction : MathCombineAPLFunction() {
    // No support for complex numbers yet
    override fun combine1Arg(a: APLValue) = a

    override fun combine2Arg(a: APLValue, b: APLValue) = APLDouble(a.asDouble() + b.asDouble())
}

class SubAPLFunction : MathCombineAPLFunction() {
    override fun combine1Arg(a: APLValue) = APLDouble(-a.asDouble())
    override fun combine2Arg(a: APLValue, b: APLValue) = APLDouble(a.asDouble() - b.asDouble())
}

class MulAPLFunction : MathCombineAPLFunction() {
    override fun combine1Arg(a: APLValue): APLValue {
        val v = a.asDouble()
        val res = when {
            v > 0 -> 1.0
            v < 0 -> -1.0
            else -> 0.0
        }
        return APLDouble(res)
    }

    override fun combine2Arg(a: APLValue, b: APLValue) = APLDouble(a.asDouble() * b.asDouble())
}

class DivAPLFunction : MathCombineAPLFunction() {
    override fun combine1Arg(a: APLValue) = APLDouble(1.0 / a.asDouble())
    override fun combine2Arg(a: APLValue, b: APLValue) = APLDouble(a.asDouble() / b.asDouble())
}
