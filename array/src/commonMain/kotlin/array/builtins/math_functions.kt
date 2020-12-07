package array.builtins

import array.*
import array.complex.*
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
    a: APLValue,
    b: APLValue,
    private val pos: Position
) : DeferredResultArray() {
    private val aRank = a.rank
    private val bRank = b.rank
    private val a0: APLValue
    private val b0: APLValue

    init {
        unless(aRank == 0 || bRank == 0 || a.dimensions.compareEquals(b.dimensions)) {
            throwAPLException(InvalidDimensionsException("Arguments must be of the same dimension, or one of the arguments must be a scalar", pos))
        }
        a0 = a.unwrapDeferredValue()
        b0 = b.unwrapDeferredValue()
    }

    override val dimensions = if (aRank == 0) b.dimensions else a.dimensions

    override val rank = dimensions.size

    override fun valueAt(p: Int): APLValue {
        val v1 = when {
            a0 is APLSingleValue -> a0
            a0.isScalar() -> a0.valueAt(0)
            else -> a0.valueAt(p)
        }
        val v2 = when {
            b0 is APLSingleValue -> b0
            b0.isScalar() -> b0.valueAt(0)
            else -> b0.valueAt(p)
        }
        return if (v1 is APLSingleValue && v2 is APLSingleValue) {
            fn.combineValues(v1, v2)
        } else {
            ArraySum2Args(fn, v1, v2, pos)
        }
    }
}

abstract class MathCombineAPLFunction(pos: Position) : APLFunction(pos) {
    override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
        if (a is APLSingleValue) {
            return combine1Arg(a)
        }

        val fn = object : CellSumFunction1Arg {
            override fun combine(a: APLSingleValue): APLValue {
                return combine1Arg(a)
            }
        }
        return ArraySum1Arg(fn, a)
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        if (a is APLSingleValue && b is APLSingleValue) {
            return combine2Arg(a, b)
        }

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
                ensureValidAxis(axisInt, d2, pos)
                if (d1[0] != d2[axisInt]) {
                    throwAPLException(InvalidDimensionsException("Dimensions of A does not match dimensions of B across axis ${axisInt}", pos))
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
                    if (axisInt == 0) Pair(a, b) else throwAPLException(IllegalAxisException(axisInt, aDimensions, pos))
                }
                aDimensions.size == 1 -> Pair(computeTransformation(a, aDimensions, bDimensions), b)
                bDimensions.size == 1 -> Pair(a, computeTransformation(b, bDimensions, aDimensions))
                else -> throwAPLException(APLIllegalArgumentException("When specifying an axis, A or B has ro be rank 1", pos))
            }

            return ArraySum2Args(fn, a1, b1, pos)
        } else {
            return ArraySum2Args(fn, a, b, pos)
        }
    }

    open fun combine1Arg(a: APLSingleValue): APLValue = throwAPLException(Unimplemented1ArgException(pos))
    open fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue = throwAPLException(Unimplemented2ArgException(pos))
}

abstract class MathNumericCombineAPLFunction(pos: Position) : MathCombineAPLFunction(pos) {
    override fun combine1Arg(a: APLSingleValue): APLValue = numberCombine1Arg(a.ensureNumber(pos))
    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue = numberCombine2Arg(a.ensureNumber(pos), b.ensureNumber())

    open fun numberCombine1Arg(a: APLNumber): APLValue = throwAPLException(Unimplemented1ArgException(pos))
    open fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue = throwAPLException(Unimplemented2ArgException(pos))
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
                { x -> x.signum().makeAPLNumber() }
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

class NotAPLFunction : APLFunctionDescriptor {
    class NotAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> notOp(x, pos) },
                { x -> notOp(x.toLong(), pos) },
                { x ->
                    if (x.imaginary == 0.0) {
                        notOp(x.real.toLong(), pos)
                    } else {
                        throwAPLException(APLIncompatibleDomainsException("Not operation not supported for complex", pos))
                    }
                }
            )
        }

        private fun notOp(v: Long, pos: Position): APLValue {
            val result = when (v) {
                0L -> 1
                1L -> 0
                else -> throwAPLException(APLIncompatibleDomainsException("Not operation not supported for value", pos))
            }
            return result.makeAPLNumber()
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            if (axis != null) {
                throwAPLException(AxisNotSupported(pos))
            }
            val a1 = a.arrayify()
            if (a1.dimensions.size != 1) {
                throwAPLException(InvalidDimensionsException("Left argument to without must be a scalar or a 1-dimensional array", pos))
            }
            val b1 = b.arrayify()
            val map = HashSet<Any>()
            b1.iterateMembers { v ->
                map.add(v.makeKey())
            }
            val result = ArrayList<APLValue>()
            a1.iterateMembers { v ->
                if (!map.contains(v.makeKey())) {
                    result.add(v)
                }
            }
            return APLArrayImpl(dimensionsOfSize(result.size), result.toTypedArray())
        }
    }

    override fun make(pos: Position): APLFunction {
        return NotAPLFunctionImpl(pos)
    }
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
                { _, _ -> TODO("Not implemented") })
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
                { x -> E.pow(x).makeAPLNumber() }
            )
        }

        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> x.toDouble().pow(y.toDouble()).makeAPLNumber() },
                { x, y ->
                    if (x < 0) {
                        x.toComplex().pow(y.toComplex()).makeAPLNumber()
                    } else {
                        x.pow(y).makeAPLNumber()
                    }
                },
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
        override fun combine1Arg(a: APLSingleValue): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> if (x < 0) x.toDouble().toComplex().ln().makeAPLNumber() else ln(x.toDouble()).makeAPLNumber() },
                { x -> if (x < 0) x.toComplex().ln().makeAPLNumber() else ln(x).makeAPLNumber() },
                { x -> x.ln().makeAPLNumber() }
            )
        }

        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y ->
                    if (x < 0 || y < 0) y.toDouble().toComplex().log(x.toDouble()).makeAPLNumber() else log(
                        y.toDouble(),
                        x.toDouble()).makeAPLNumber()
                },
                { x, y -> if (x < 0 || y < 0) y.toComplex().log(x.toComplex()).makeAPLNumber() else log(y, x).makeAPLNumber() },
                { x, y -> y.log(x).makeAPLNumber() }
            )
        }
    }

    override fun make(pos: Position) = LogAPLFunctionImpl(pos)
}

class SinAPLFunction : APLFunctionDescriptor {
    class SinAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> sin(x.toDouble()).makeAPLNumber() },
                { x -> sin(x).makeAPLNumber() },
                { x -> complexSin(x).makeAPLNumber() }
            )
        }
    }

    override fun make(pos: Position) = SinAPLFunctionImpl(pos)
}

class CosAPLFunction : APLFunctionDescriptor {
    class CosAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> cos(x.toDouble()).makeAPLNumber() },
                { x -> cos(x).makeAPLNumber() },
                { x -> complexCos(x).makeAPLNumber() }
            )
        }
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
            return numericRelationOperation(pos,
                a,
                b,
                { x, y ->
                    when {
                        x == 0L || y == 0L -> APLLONG_0
                        x == 1L && x == 1L -> APLLONG_1
                        else -> (x * (y / integerGcd(x, y))).makeAPLNumber()
                    }
                },
                { x, y ->
                    when {
                        x == 0.0 || y == 0.0 -> APLLONG_0
                        x == 1.0 || y == 1.0 -> APLLONG_1
                        else -> (x * (y / floatGcd(x, y))).makeAPLNumber()
                    }
                },
                { x, y -> (y * (x / complexGcd(x, y))).nearestGaussian().makeAPLNumber() })
        }

        override fun identityValue() = APLLONG_1
    }

    override fun make(pos: Position) = AndAPLFunctionImpl(pos)
}

fun integerGcd(m: Long, n: Long): Long {
    if (m == 0L) return n
    if (n == 0L) return m
    var aa = 1L
    var b = 1L
    var a = 0L
    var bb = 0L
    var c = m.absoluteValue
    var d = n.absoluteValue
    while (true) {
        val r = c % d
        if (r == 0L) return d
        val q = c / d
        val ta = aa
        val tb = bb
        c = d
        d = r
        aa = a
        a = ta - q * a
        bb = b
        b = tb - q * b
    }
}

fun floatGcd(a: Double, b: Double): Double {
    var a1 = a.absoluteValue
    var b1 = b.absoluteValue
    if (b1 < a1) {
        val tmp = b1
        b1 = a1
        a1 = tmp
    }
    while (true) {
        if (a1.absoluteValue < 0.00001) return b1
        val r = b1.rem(a1)
        b1 = a1
        a1 = r
    }
}

fun complexGcd(a: Complex, b: Complex): Complex {
    var a1 = a.nearestGaussian()
    var b1 = b.nearestGaussian()
    while (true) {
        if (a1.abs() > b1.abs()) {
            val tmp = a1
            a1 = b1
            b1 = tmp
        }
        if (a1.abs() < 0.2) {
            return b1
        }
        val quot = b1 / a1
        val q = quot.nearestGaussian()
        val r = b1 - q * a1
        b1 = a1
        a1 = r
    }
}

class OrAPLFunction : APLFunctionDescriptor {
    class OrAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(pos,
                a,
                b,
                { x, y ->
                    when {
                        x == 0L && y == 0L -> APLLONG_0
                        (x == 0L || x == 1L) && y == 1L -> APLLONG_1
                        x == 1L && (y == 0L || y == 1L) -> APLLONG_1
                        else -> integerGcd(x, y).makeAPLNumber()
                    }
                },
                { x, y ->
                    when {
                        x == 0.0 && y == 0.0 -> APLLONG_0
                        (x == 0.0 || x == 1.0) && y == 1.0 -> APLLONG_1
                        x == 1.0 && (y == 0.0 || y == 1.0) -> APLLONG_1
                        else -> floatGcd(x, y).makeAPLNumber()

                    }
                },
                { x, y -> complexGcd(x, y).makeAPLNumber() })
        }

        override fun identityValue() = APLLONG_0
    }

    override fun make(pos: Position) = OrAPLFunctionImpl(pos)
}
