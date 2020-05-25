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
    override val dimensions get() = a.dimensions
    override val size get() = a.size
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
    private val b: APLValue,
    private val pos: Position
) : DeferredResultArray() {
    private val aRank = a.rank
    private val bRank = b.rank

    init {
        unless(aRank == 0 || bRank == 0 || a.dimensions.compareEquals(b.dimensions)) {
            throw InvalidDimensionsException("Arguments must be of the same dimension, or one of the arguments must be a scalar", pos)
        }
    }

    override val dimensions = if (aRank == 0) b.dimensions else a.dimensions

    override val rank = dimensions.size

    override fun valueAt(p: Int): APLValue {
        val v1 = if (a is APLSingleValue) a else a.valueAt(p)
        val v2 = if (b is APLSingleValue) b else b.valueAt(p)
        return if (v1 is APLSingleValue && v2 is APLSingleValue) {
            fn.combineValues(v1, v2)
        } else {
            ArraySum2Args(fn, v1, v2, pos)
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

        if (axis != null) {
            val aDimensions = a.dimensions
            val bDimensions = b.dimensions

            val axisInt = axis.ensureNumber(pos).asInt()

            fun computeTransformation(baseVal: APLValue, d1: Dimensions, d2: Dimensions): APLValue {
                ensureValidAxis(axisInt, d2)
                if (d1[0] != d2[axisInt]) {
                    throw InvalidDimensionsException("Dimensions of A does not match dimensions of B across axis ${axisInt}", pos)
                }
                val d = d2.remove(axisInt).insert(d2.size - 1, d2[axisInt])
                val transposeAxis = IntArray(d2.size) { i ->
                    when {
                        i == d2.size - 1 -> axisInt
                        i < axisInt -> i
                        else -> i + 1
                    }
                }
                return TransposedAPLValue(transposeAxis, ResizedArray(d, baseVal), pos)
            }

            // When an axis is given, one of the arguments must be rank 1, and its dimension must be equal to the
            // dimension of the other arguments across the axis
            val (a1, b1) = when {
                aDimensions.size == 1 && bDimensions.size == 1 -> {
                    if (axisInt == 0) Pair(a, b) else throw IllegalAxisException(axisInt, aDimensions, pos)
                }
                aDimensions.size == 1 -> Pair(computeTransformation(a, aDimensions, bDimensions), b)
                bDimensions.size == 1 -> Pair(a, computeTransformation(b, bDimensions, aDimensions))
                else -> throw APLIllegalArgumentException("When specifying an axis, A or B has ro be rank 1", pos)
            }

            return ArraySum2Args(fn, a1, b1, pos)
        } else {
            return ArraySum2Args(fn, a, b, pos)
        }
    }

    open fun combine1Arg(a: APLSingleValue): APLValue = TODO("not implemented")
    open fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue = TODO("not implemented")
}

abstract class MathNumericCombineAPLFunction(pos: Position) : MathCombineAPLFunction(pos) {
    override fun combine1Arg(a: APLSingleValue): APLValue = numberCombine1Arg(a.ensureNumber(pos))
    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue = numberCombine2Arg(a.ensureNumber(pos), b.ensureNumber())

    open fun numberCombine1Arg(a: APLNumber): APLValue = TODO("not implemented")
    open fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue = TODO("not implemented")
}

class AddAPLFunction : APLFunctionDescriptor {

    class AddAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> x.makeAPLNumber() },
                { x -> x.makeAPLNumber() },
                { x -> Complex(x.real, -x.imaginary).makeAPLNumber() }
            )
        }

        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> (x + y).makeAPLNumber() },
                { x, y -> (x + y).makeAPLNumber() },
                { x, y -> (x + y).makeAPLNumber() }
            )
        }

        override fun identityValue() = APLLONG_0
    }

    override fun make(pos: Position) = AddAPLFunctionImpl(pos)
}

class SubAPLFunction : APLFunctionDescriptor {
    class SubAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> (-x).makeAPLNumber() },
                { x -> (-x).makeAPLNumber() },
                { x -> (-x).makeAPLNumber() }
            )
        }

        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> (x - y).makeAPLNumber() },
                { x, y -> (x - y).makeAPLNumber() },
                { x, y -> (x - y).makeAPLNumber() }
            )
        }

        override fun identityValue() = APLLONG_0
    }

    override fun make(pos: Position) = SubAPLFunctionImpl(pos)
}

class MulAPLFunction : APLFunctionDescriptor {
    class MulAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> x.sign.toLong().makeAPLNumber() },
                { x -> x.sign.makeAPLNumber() },
                { x -> x.abs().makeAPLNumber() }
            )
        }

        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> (x * y).makeAPLNumber() },
                { x, y -> (x * y).makeAPLNumber() },
                { x, y -> (x * y).makeAPLNumber() }
            )
        }

        override fun identityValue() = APLLONG_1
    }

    override fun make(pos: Position) = MulAPLFunctionImpl(pos)
}

class DivAPLFunction : APLFunctionDescriptor {
    class DivAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> if (x == 0L) APLLONG_0 else (1.0 / x).makeAPLNumber() },
                { x -> if (x == 0.0) APLLONG_0 else (1.0 / x).makeAPLNumber() },
                { x -> if (x == Complex.ZERO) APLLONG_0 else x.reciprocal().makeAPLNumber() }
            )
        }

        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y ->
                    when {
                        y == 0L -> APLLONG_0
                        x % y == 0L -> (x / y).makeAPLNumber()
                        else -> (x.toDouble() / y.toDouble()).makeAPLNumber()
                    }
                },
                { x, y -> APLDouble(if (y == 0.0) 0.0 else x / y) },
                { x, y -> if (y == Complex.ZERO) APLDouble(0.0) else (x / y).makeAPLNumber() }
            )
        }

        override fun identityValue() = APLLONG_1
    }

    override fun make(pos: Position) = DivAPLFunctionImpl(pos)
}

class ModAPLFunction : APLFunctionDescriptor {
    class ModAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> abs(x).makeAPLNumber() },
                { x -> abs(x).makeAPLNumber() },
                { x -> hypot(x.real, x.imaginary).makeAPLNumber() }
            )
        }

        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> (y % x).let { result -> (if (x < 0) -result else result).makeAPLNumber() } },
                { x, y -> (y % x).let { result -> (if (x < 0) -result else result).makeAPLNumber() } },
                { _, _ -> TODO("Not implemented") }
            )
        }
    }

    override fun make(pos: Position) = ModAPLFunctionImpl(pos)
}

class PowerAPLFunction : APLFunctionDescriptor {
    class PowerAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> exp(x.toDouble()).makeAPLNumber() },
                { x -> exp(x).makeAPLNumber() },
                { x -> Complex(E).pow(x).makeAPLNumber() }
            )
        }

        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> x.toDouble().pow(y.toDouble()).makeAPLNumber() },
                { x, y -> x.pow(y).makeAPLNumber() },
                { x, y -> x.pow(y).makeAPLNumber() }
            )
        }

        override fun identityValue() = APLLONG_1
    }

    override fun make(pos: Position) = PowerAPLFunctionImpl(pos)
}

fun complexFloor(z: Complex): Complex {
    var fr = floor(z.real)
    var dr = z.real - fr
    var fi = floor(z.imaginary)
    var di = z.imaginary - fi
    if (dr > 1) {
        fr += 1.0
        dr = 0.0
    }
    if (di > 1) {
        fi += 1.0
        di = 0.0
    }
    return when {
        dr + di < 1 -> Complex(fr, fi)
        dr < di -> Complex(fr, fi + 1.0)
        else -> Complex(fr + 1.0, fi)
    }
}

class MinAPLFunction : APLFunctionDescriptor {
    class MinAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun combine1Arg(a: APLSingleValue): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> x.makeAPLNumber() },
                { x -> floor(x).makeAPLNumber() },
                { x -> complexFloor(x).makeAPLNumber() }
            )
        }

        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> if (x < y) x.makeAPLNumber() else y.makeAPLNumber() },
                { x, y -> if (x < y) x.makeAPLNumber() else y.makeAPLNumber() },
                { x, y -> (if (x.real < y.real || (x.real == y.real && x.imaginary < y.imaginary)) x else y).makeAPLNumber() },
                { x, y -> if (x < y) APLChar(x) else APLChar(y) }
            )
        }
    }

    override fun make(pos: Position) = MinAPLFunctionImpl(pos)
}

fun complexCeiling(value: Complex): Complex {
    return -complexFloor(-value)
}

class MaxAPLFunction : APLFunctionDescriptor {
    class MaxAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun combine1Arg(a: APLSingleValue): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> x.makeAPLNumber() },
                { x -> ceil(x).makeAPLNumber() },
                { x -> complexCeiling(x).makeAPLNumber() }
            )
        }

        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> if (x > y) x.makeAPLNumber() else y.makeAPLNumber() },
                { x, y -> if (x > y) x.makeAPLNumber() else y.makeAPLNumber() },
                { x, y -> (if (x.real > y.real || (x.real == y.real && x.imaginary > y.imaginary)) x else y).makeAPLNumber() },
                { x, y -> if (x > y) APLChar(x) else APLChar(y) }
            )
        }
    }

    override fun make(pos: Position) = MaxAPLFunctionImpl(pos)
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
                return (if (aValue == 1.0 && bValue == 1.0) 1 else 0).makeAPLNumber()
            } else {
                TODO("LCM is not implemented")
            }
        }

        override fun identityValue() = APLLONG_1
    }

    override fun make(pos: Position) = AndAPLFunctionImpl(pos)
}

class OrAPLFunction : APLFunctionDescriptor {
    class OrAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            val aValue = a.asDouble()
            val bValue = b.asDouble()
            if ((aValue == 0.0 || aValue == 1.0) && (bValue == 0.0 || bValue == 1.0)) {
                return (if (aValue == 1.0 || bValue == 1.0) 1 else 0).makeAPLNumber()
            } else {
                TODO("GCD is not implemented")
            }
        }

        override fun identityValue() = APLLONG_0
    }

    override fun make(pos: Position) = OrAPLFunctionImpl(pos)
}
