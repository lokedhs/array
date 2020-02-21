package array.builtins

import array.*
import kotlin.math.*

interface CellSumFunction1Arg {
    fun combine(a: APLSingleValue): APLValue
}

interface CellSumFunction2Args {
    fun combineValues(a: APLSingleValue, b: APLSingleValue): APLValue
}

class ArraySum1Arg(
    private val fn: CellSumFunction1Arg,
    private val a: APLValue
) : APLArray() {
    override fun dimensions() = a.dimensions()
    override fun size() = a.size()
    override fun valueAt(p: Int): APLValue {
        if (a is APLSingleValue) {
            return fn.combine(a)
        }
        val v = a.valueAt(p)
        return if (v is APLSingleValue) {
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
        return if (v1 is APLSingleValue && v2 is APLSingleValue) {
            fn.combineValues(v1, v2)
        } else {
            ArraySum2Args(fn, v1, v2)
        }
    }
}

abstract class MathCombineAPLFunction : APLFunction {
    override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        val fn = object : CellSumFunction1Arg {
            override fun combine(a: APLSingleValue): APLValue {
                return combine1Arg(a)
            }
        }
        return ArraySum1Arg(fn, a)
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        val fn = object : CellSumFunction2Args {
            override fun combineValues(a: APLSingleValue, b: APLSingleValue): APLValue {
                return combine2Arg(a, b)
            }
        }
        return ArraySum2Args(fn, a, b)
    }

    open fun combine1Arg(a: APLSingleValue): APLValue = TODO("not implemented")
    open fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue = TODO("not implemented")
}

abstract class MathNumericCombineAPLFunction : MathCombineAPLFunction() {
    override fun combine1Arg(a: APLSingleValue): APLValue = numberCombine1Arg(a.ensureNumber())
    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue = numberCombine2Arg(a.ensureNumber(), b.ensureNumber())

    open fun numberCombine1Arg(a: APLNumber): APLValue = TODO("not implemented")
    open fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue = TODO("not implemented")
}

class AddAPLFunction : MathNumericCombineAPLFunction() {
    // No support for complex numbers yet
    override fun numberCombine1Arg(a: APLNumber) = a

    override fun numberCombine2Arg(a: APLNumber, b: APLNumber) = APLDouble(a.asDouble() + b.asDouble())
}

class SubAPLFunction : MathNumericCombineAPLFunction() {
    override fun numberCombine1Arg(a: APLNumber) = APLDouble(-a.asDouble())
    override fun numberCombine2Arg(a: APLNumber, b: APLNumber) = APLDouble(a.asDouble() - b.asDouble())
}

class MulAPLFunction : MathNumericCombineAPLFunction() {
    override fun numberCombine1Arg(a: APLNumber): APLValue {
        val v = a.asDouble()
        val res = when {
            v > 0 -> 1.0
            v < 0 -> -1.0
            else -> 0.0
        }
        return APLDouble(res)
    }

    override fun numberCombine2Arg(a: APLNumber, b: APLNumber) = APLDouble(a.asDouble() * b.asDouble())
}

class DivAPLFunction : MathNumericCombineAPLFunction() {
    override fun numberCombine1Arg(a: APLNumber) = APLDouble(1.0 / a.asDouble())
    override fun numberCombine2Arg(a: APLNumber, b: APLNumber) = APLDouble(a.asDouble() / b.asDouble())
}

class PowerAPLFunction : MathNumericCombineAPLFunction() {
    override fun numberCombine1Arg(a: APLNumber) = APLDouble(exp(a.asDouble()))
    override fun numberCombine2Arg(a: APLNumber, b: APLNumber) = APLDouble(a.asDouble().pow(b.asDouble()))
}

class LogAPLFunction : MathNumericCombineAPLFunction() {
    override fun numberCombine1Arg(a: APLNumber) = APLDouble(ln(a.asDouble()))
    override fun numberCombine2Arg(a: APLNumber, b: APLNumber) = APLDouble(log(b.asDouble(), a.asDouble()))
}

class SinAPLFunction : MathNumericCombineAPLFunction() {
    override fun numberCombine1Arg(a: APLNumber) = APLDouble(sin(a.asDouble()))
}

class CosAPLFunction : MathNumericCombineAPLFunction() {
    override fun numberCombine1Arg(a: APLNumber) = APLDouble(cos(a.asDouble()))
}

class TanAPLFunction : MathNumericCombineAPLFunction() {
    override fun numberCombine1Arg(a: APLNumber) = APLDouble(tan(a.asDouble()))
}

class AsinAPLFunction : MathNumericCombineAPLFunction() {
    override fun numberCombine1Arg(a: APLNumber) = APLDouble(asin(a.asDouble()))
}

class AcosAPLFunction : MathNumericCombineAPLFunction() {
    override fun numberCombine1Arg(a: APLNumber) = APLDouble(acos(a.asDouble()))
}

class AtanAPLFunction : MathNumericCombineAPLFunction() {
    override fun numberCombine1Arg(a: APLNumber) = APLDouble(atan(a.asDouble()))
}

class AndAPLFunction : MathNumericCombineAPLFunction() {
    override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
        val aValue = a.asDouble()
        val bValue = b.asDouble()
        if ((aValue == 0.0 || aValue == 1.0) && (bValue == 0.0 || bValue == 1.0)) {
            return APLLong(if (aValue == 1.0 && bValue == 1.0) 1 else 0)
        } else {
            TODO("LCM is not implemented")
        }
    }
}

class OrAPLFunction : MathNumericCombineAPLFunction() {
    override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
        val aValue = a.asDouble()
        val bValue = b.asDouble()
        if ((aValue == 0.0 || aValue == 1.0) && (bValue == 0.0 || bValue == 1.0)) {
            return APLLong(if (aValue == 1.0 || bValue == 1.0) 1 else 0)
        } else {
            TODO("GCD is not implemented")
        }
    }
}
