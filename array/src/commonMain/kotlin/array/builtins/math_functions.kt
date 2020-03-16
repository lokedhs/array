package array.builtins

import array.*
import array.complex.Complex
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
) : DeferredResultArray() {
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
) : DeferredResultArray() {
    private val aRank = a.rank()
    private val bRank = b.rank()

    init {
        unless(aRank == 0 || bRank == 0 || a.dimensions().compare(b.dimensions())) {
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

abstract class MathCombineAPLFunction(pos: Position) : APLFunction(pos) {
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

abstract class MathNumericCombineAPLFunction(pos: Position) : MathCombineAPLFunction(pos) {
    override fun combine1Arg(a: APLSingleValue): APLValue = numberCombine1Arg(a.ensureNumber())
    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue = numberCombine2Arg(a.ensureNumber(), b.ensureNumber())

    open fun numberCombine1Arg(a: APLNumber): APLValue = TODO("not implemented")
    open fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue = TODO("not implemented")
}

class AddAPLFunction : APLFunctionDescriptor {

    class AddAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                a,
                { x -> x.makeAPLNumber() },
                { x -> x.makeAPLNumber() },
                { x -> Complex(x.real, -x.imaginary).makeAPLNumber() }
            )
        }

        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                a,
                b,
                { x, y -> (x + y).makeAPLNumber() },
                { x, y -> (x + y).makeAPLNumber() },
                { x, y -> (x + y).makeAPLNumber() }
            )
        }

        override fun identityValue() = APLLong(0)
    }

    override fun make(pos: Position) = AddAPLFunctionImpl(pos)
}

class SubAPLFunction : APLFunctionDescriptor {
    class SubAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                a,
                { x -> (-x).makeAPLNumber() },
                { x -> (-x).makeAPLNumber() },
                { x -> (-x).makeAPLNumber() }
            )
        }

        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                a,
                b,
                { x, y -> (x - y).makeAPLNumber() },
                { x, y -> (x - y).makeAPLNumber() },
                { x, y -> (x - y).makeAPLNumber() }
            )
        }

        override fun identityValue() = APLLong(0)
    }

    override fun make(pos: Position) = SubAPLFunctionImpl(pos)
}

class MulAPLFunction : APLFunctionDescriptor {
    class MulAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                a,
                { x -> x.sign.toLong().makeAPLNumber() },
                { x -> x.sign.makeAPLNumber() },
                { x -> x.abs().makeAPLNumber() }
            )
        }

        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                a,
                b,
                { x, y -> (x * y).makeAPLNumber() },
                { x, y -> (x * y).makeAPLNumber() },
                { x, y -> (x * y).makeAPLNumber() }
            )
        }

        override fun identityValue() = APLLong(1)
    }

    override fun make(pos: Position) = MulAPLFunctionImpl(pos)
}

class DivAPLFunction : APLFunctionDescriptor {
    class DivAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                a,
                { x -> if (x == 0L) 0.makeAPLNumber() else (1.0 / x).makeAPLNumber() },
                { x -> if (x == 0.0) 0.makeAPLNumber() else (1.0 / x).makeAPLNumber() },
                { x -> if (x == Complex.ZERO) 0.makeAPLNumber() else x.reciprocal().makeAPLNumber() }
            )
        }

        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                a,
                b,
                { x, y ->
                    when {
                        y == 0L -> APLLong(0)
                        x % y == 0L -> APLLong(x / y)
                        else -> APLDouble(x.toDouble() / y.toDouble())
                    }
                },
                { x, y -> APLDouble(if (y == 0.0) 0.0 else x / y) },
                { x, y -> if (y == Complex.ZERO) APLDouble(0.0) else APLComplex(x / y) }
            )
        }

        override fun identityValue() = APLLong(1)
    }

    override fun make(pos: Position) = DivAPLFunctionImpl(pos)
}

class PowerAPLFunction : APLFunctionDescriptor {
    class PowerAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                a,
                { x -> exp(x.toDouble()).makeAPLNumber() },
                { x -> exp(x).makeAPLNumber() },
                { x -> Complex(E).pow(x).makeAPLNumber() }
            )
        }

        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                a,
                b,
                { x, y -> x.toDouble().pow(y.toDouble()).makeAPLNumber() },
                { x, y -> x.pow(y).makeAPLNumber() },
                { x, y -> x.pow(y).makeAPLNumber() }
            )
        }

        override fun identityValue() = APLLong(1)
    }

    override fun make(pos: Position) = PowerAPLFunctionImpl(pos)

}

class LogAPLFunction : APLFunctionDescriptor {
    class LogAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber) = APLDouble(ln(a.asDouble()))
        override fun numberCombine2Arg(a: APLNumber, b: APLNumber) = APLDouble(log(b.asDouble(), a.asDouble()))
    }

    override fun make(pos: Position) = LogAPLFunctionImpl(pos)
}

class SinAPLFunction : APLFunctionDescriptor {
    class SinAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber) = APLDouble(sin(a.asDouble()))
    }

    override fun make(pos: Position) = SinAPLFunctionImpl(pos)
}

class CosAPLFunction : APLFunctionDescriptor {
    class CosAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber) = APLDouble(cos(a.asDouble()))
    }

    override fun make(pos: Position) = CosAPLFunctionImpl(pos)
}

class TanAPLFunction : APLFunctionDescriptor {
    class TanAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber) = APLDouble(tan(a.asDouble()))
    }

    override fun make(pos: Position) = TanAPLFunctionImpl(pos)
}

class AsinAPLFunction : APLFunctionDescriptor {
    class AsinAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber) = APLDouble(asin(a.asDouble()))
    }

    override fun make(pos: Position) = AsinAPLFunctionImpl(pos)
}

class AcosAPLFunction : APLFunctionDescriptor {
    class AcosAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber) = APLDouble(acos(a.asDouble()))
    }

    override fun make(pos: Position) = AcosAPLFunctionImpl(pos)
}

class AtanAPLFunction : APLFunctionDescriptor {
    class AtanAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber) = APLDouble(atan(a.asDouble()))
    }

    override fun make(pos: Position) = AtanAPLFunctionImpl(pos)
}

class AndAPLFunction : APLFunctionDescriptor {
    class AndAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            val aValue = a.asDouble()
            val bValue = b.asDouble()
            if ((aValue == 0.0 || aValue == 1.0) && (bValue == 0.0 || bValue == 1.0)) {
                return APLLong(if (aValue == 1.0 && bValue == 1.0) 1 else 0)
            } else {
                TODO("LCM is not implemented")
            }
        }

        override fun identityValue() = APLLong(1)
    }

    override fun make(pos: Position) = AndAPLFunctionImpl(pos)
}

class OrAPLFunction : APLFunctionDescriptor {
    class OrAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            val aValue = a.asDouble()
            val bValue = b.asDouble()
            if ((aValue == 0.0 || aValue == 1.0) && (bValue == 0.0 || bValue == 1.0)) {
                return APLLong(if (aValue == 1.0 || bValue == 1.0) 1 else 0)
            } else {
                TODO("GCD is not implemented")
            }
        }

        override fun identityValue() = APLLong(0)
    }

    override fun make(pos: Position) = OrAPLFunctionImpl(pos)
}
