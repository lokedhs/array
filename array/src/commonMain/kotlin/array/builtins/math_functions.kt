package array.builtins

import array.*
import kotlin.math.*
import array.builtins.MathCombineAPLFunction as MathCombineAPLFunction1

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
    override fun valueAt(p: Int): APLValue {
        if(a is APLSingleValue) {
            return fn.combine(a)
        }
        val v = a.valueAt(p)
        return if(v is APLSingleValue) {
            fn.combine(v)
        } else {
            ArraySum1Arg(fn, v)
        }
    }
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

    private val dimensions = if (aRank == 0) b.dimensions() else a.dimensions()
    override fun dimensions() = dimensions

    private val rank = dimensions.size
    override fun rank() = rank

    override fun valueAt(p: Int): APLValue {
        val v1 = if (a is APLSingleValue) a else a.valueAt(p)
        val v2 = if (b is APLSingleValue) b else b.valueAt(p)
        return if (a is APLSingleValue && b is APLSingleValue) {
            fn.combineValues(v1, v2)
        } else {
            ArraySum2Args(fn, v1, v2)
        }
    }

    override fun asDouble() = if (rank() == 0) valueAt(0).asDouble() else super.asDouble()
}

abstract class MathCombineAPLFunction : APLFunction {
    override fun eval1Arg(context: RuntimeContext, arg: APLValue, axis: APLValue?): APLValue {
        val fn = object : CellSumFunction1Arg {
            override fun combine(a: APLValue): APLValue {
                return combine1Arg(a)
            }
        }
        return ArraySum1Arg(fn, arg)
    }

    override fun eval2Arg(context: RuntimeContext, arg1: APLValue, arg2: APLValue, axis: APLValue?): APLValue {
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

class AddAPLFunction : MathCombineAPLFunction1() {
    // No support for complex numbers yet
    override fun combine1Arg(a: APLValue) = a

    override fun combine2Arg(a: APLValue, b: APLValue) = APLDouble(a.asDouble() + b.asDouble())
}

class SubAPLFunction : MathCombineAPLFunction1() {
    override fun combine1Arg(a: APLValue) = APLDouble(-a.asDouble())
    override fun combine2Arg(a: APLValue, b: APLValue) = APLDouble(a.asDouble() - b.asDouble())
}

class MulAPLFunction : MathCombineAPLFunction1() {
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

class DivAPLFunction : MathCombineAPLFunction1() {
    override fun combine1Arg(a: APLValue) = APLDouble(1.0 / a.asDouble())
    override fun combine2Arg(a: APLValue, b: APLValue) = APLDouble(a.asDouble() / b.asDouble())
}

class PowerAPLFunction : MathCombineAPLFunction1() {
    override fun combine1Arg(a: APLValue) = APLDouble(exp(a.asDouble()))
    override fun combine2Arg(a: APLValue, b: APLValue) = APLDouble(a.asDouble().pow(b.asDouble()))
}

class LogAPLFunction : MathCombineAPLFunction1() {
    override fun combine1Arg(a: APLValue) = APLDouble(ln(a.asDouble()))
    override fun combine2Arg(a: APLValue, b: APLValue) = APLDouble(log(b.asDouble(), a.asDouble()))
}

class SinAPLFunction : MathCombineAPLFunction1() {
    override fun combine1Arg(a: APLValue) = APLDouble(sin(a.asDouble()))
}

class CosAPLFunction : MathCombineAPLFunction1() {
    override fun combine1Arg(a: APLValue) = APLDouble(cos(a.asDouble()))
}

class TanAPLFunction : MathCombineAPLFunction1() {
    override fun combine1Arg(a: APLValue) = APLDouble(tan(a.asDouble()))
}

class AsinAPLFunction : MathCombineAPLFunction1() {
    override fun combine1Arg(a: APLValue) = APLDouble(asin(a.asDouble()))
}

class AcosAPLFunction : MathCombineAPLFunction1() {
    override fun combine1Arg(a: APLValue) = APLDouble(acos(a.asDouble()))
}

class AtanAPLFunction : MathCombineAPLFunction1() {
    override fun combine1Arg(a: APLValue) = APLDouble(atan(a.asDouble()))
}

class EqualsAPLFunction : MathCombineAPLFunction1() {
    override fun combine2Arg(a: APLValue, b: APLValue): APLValue {
        return APLLong(if(a.asDouble() == b.asDouble()) 1 else 0)
    }
}

class NotEqualsAPLFunction : MathCombineAPLFunction1() {
    override fun combine2Arg(a: APLValue, b: APLValue): APLValue {
        return APLLong(if(a.asDouble() != b.asDouble()) 1 else 0)
    }
}

class AndAPLFunction : MathCombineAPLFunction1() {
    override fun combine2Arg(a: APLValue, b: APLValue): APLValue {
        val aValue = a.asDouble()
        val bValue = b.asDouble()
        if((aValue == 0.0 || aValue == 1.0) && (bValue == 0.0 || bValue == 1.0)) {
            return APLLong(if(aValue == 1.0 && bValue == 1.0) 1 else 0)
        }
        else {
            TODO("LCM is not implemented")
        }
    }
}

class OrAPLFunction : MathCombineAPLFunction1() {
    override fun combine2Arg(a: APLValue, b: APLValue): APLValue {
        val aValue = a.asDouble()
        val bValue = b.asDouble()
        if((aValue == 0.0 || aValue == 1.0) && (bValue == 0.0 || bValue == 1.0)) {
            return APLLong(if(aValue == 1.0 || bValue == 1.0) 1 else 0)
        }
        else {
            TODO("GCD is not implemented")
        }
    }
}
