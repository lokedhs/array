package array.builtins

import array.*
import kotlin.math.*

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

    private val aRank = a.rank()
    private val bRank = b.rank()

    init {
        unless(aRank == 0 || bRank == 0 || Arrays.equals(a.dimensions(), b.dimensions())) {
            throw InvalidDimensionsException("Arguments must be of the same dimension, or one of the arguments must be a scalar")
        }
    }

    override fun dimensions() = if(aRank == 0) b.dimensions() else a.dimensions()

    override fun valueAt(p: Int): APLValue {
        return fn.combineValues(
            if(aRank == 0) a else a.valueAt(p),
            if(bRank == 0) b else b.valueAt(p))
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

    open fun combine1Arg(a: APLValue): APLValue = TODO("not implemented")
    open fun combine2Arg(a: APLValue, b: APLValue): APLValue = TODO("not implemented")
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

class PowerAPLFunction : MathCombineAPLFunction() {
    override fun combine1Arg(a: APLValue) = APLDouble(exp(a.asDouble()))
    override fun combine2Arg(a: APLValue, b: APLValue) = APLDouble(a.asDouble().pow(b.asDouble()))
}

class LogAPLFunction : MathCombineAPLFunction() {
    override fun combine1Arg(a: APLValue) = APLDouble(ln(a.asDouble()))
    override fun combine2Arg(a: APLValue, b: APLValue) = APLDouble(log(b.asDouble(), a.asDouble()))
}

class SinAPLFunction : MathCombineAPLFunction() {
    override fun combine1Arg(a: APLValue) = APLDouble(sin(a.asDouble()))
}

class CosAPLFunction : MathCombineAPLFunction() {
    override fun combine1Arg(a: APLValue) = APLDouble(cos(a.asDouble()))
}

class TanAPLFunction : MathCombineAPLFunction() {
    override fun combine1Arg(a: APLValue) = APLDouble(tan(a.asDouble()))
}
