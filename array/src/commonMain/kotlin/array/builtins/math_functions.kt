package array.builtins

import array.*
import array.OptimisationFlags.Companion.OPTIMISATION_FLAG_1ARG_DOUBLE
import array.OptimisationFlags.Companion.OPTIMISATION_FLAG_1ARG_LONG
import array.OptimisationFlags.Companion.OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE
import array.OptimisationFlags.Companion.OPTIMISATION_FLAG_2ARG_LONG_LONG
import array.complex.*
import kotlin.math.*

interface CellSumFunction1Arg {
    fun combine(a: APLSingleValue): APLValue
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

private fun throwMismatchedScalarFunctionArgs(pos: Position): Nothing {
    throwAPLException(
        InvalidDimensionsException("Arguments must be of the same dimension, or one of the arguments must be a scalar", pos))
}

class GenericArraySum2Args(
    val fn: MathCombineAPLFunction,
    val a0: APLValue,
    val b0: APLValue,
    val pos: Position
) : DeferredResultArray() {
    private val aRank = a0.rank
    private val bRank = b0.rank

    override val dimensions = if (aRank == 0) b0.dimensions else a0.dimensions
    override val rank = dimensions.size

    init {
        unless(aRank == 0 || bRank == 0 || a0.dimensions.compareEquals(b0.dimensions)) {
            throwMismatchedScalarFunctionArgs(pos)
        }
    }

    override fun valueAt(p: Int): APLValue {
        val a1 = when {
            a0 is APLSingleValue -> a0
            a0.isScalar() -> a0.valueAt(0).unwrapDeferredValue()
            else -> a0.valueAt(p).unwrapDeferredValue()
        }
        val b1 = when {
            b0 is APLSingleValue -> b0
            b0.isScalar() -> b0.valueAt(0).unwrapDeferredValue()
            else -> b0.valueAt(p).unwrapDeferredValue()
        }
        return if (a1 is APLSingleValue && b1 is APLSingleValue) {
            fn.combine2Arg(a1, b1)
        } else {
            fn.makeCellSumFunction2Args(a1, b1, pos)
        }
    }
}

class LongArraySum2Args(
    val fn: MathCombineAPLFunction,
    val a0: APLValue,
    val b0: APLValue,
    val pos: Position
) : APLArray() {
    override val dimensions: Dimensions
    override val specialisedType get() = ArrayMemberType.LONG

    init {
        unless(a0.dimensions.compareEquals(b0.dimensions)) {
            throwMismatchedScalarFunctionArgs(pos)
        }
        dimensions = a0.dimensions
    }

    override fun valueAt(p: Int) = valueAtLong(p, pos).makeAPLNumber()

    override fun valueAtLong(p: Int, pos: Position?): Long {
        return fn.combine2ArgLong(a0.valueAtLong(p, pos), b0.valueAtLong(p, pos))
    }
}

class DoubleArraySum2Args(
    val fn: MathCombineAPLFunction,
    val a0: APLValue,
    val b0: APLValue,
    val pos: Position
) : APLArray() {
    override val dimensions: Dimensions
    override val specialisedType get() = ArrayMemberType.DOUBLE

    init {
        unless(a0.dimensions.compareEquals(b0.dimensions)) {
            throwMismatchedScalarFunctionArgs(pos)
        }
        dimensions = a0.dimensions
    }

    override fun valueAt(p: Int) = valueAtLong(p, pos).makeAPLNumber()

    override fun valueAtDouble(p: Int, pos: Position?): Double {
        return fn.combine2ArgDouble(a0.valueAtDouble(p, pos), b0.valueAtDouble(p, pos))
    }
}

class LongArraySum2ArgsLeftScalar(
    val fn: MathCombineAPLFunction,
    val a0: Long,
    val b0: APLValue,
    val pos: Position
) : APLArray() {
    override val dimensions = b0.dimensions
    override val specialisedType get() = ArrayMemberType.LONG

    override fun valueAt(p: Int) = valueAtLong(p, pos).makeAPLNumber()

    override fun valueAtLong(p: Int, pos: Position?): Long {
        return fn.combine2ArgLong(a0, b0.valueAtLong(p, pos))
    }
}

class LongArraySum2ArgsRightScalar(
    val fn: MathCombineAPLFunction,
    val a0: APLValue,
    val b0: Long,
    val pos: Position
) : APLArray() {
    override val dimensions = a0.dimensions
    override val specialisedType get() = ArrayMemberType.LONG

    override fun valueAt(p: Int) = valueAtLong(p, pos).makeAPLNumber()

    override fun valueAtLong(p: Int, pos: Position?): Long {
        return fn.combine2ArgLong(a0.valueAtLong(p, pos), b0)
    }
}

abstract class MathCombineAPLFunction(pos: Position) : APLFunction(pos) {
    @Suppress("LeakingThis")
    val pos1Arg = name1Arg()?.let { pos.withName(it) } ?: pos

    @Suppress("LeakingThis")
    val pos2Arg = name2Arg()?.let { pos.withName(it) } ?: pos

    open fun name1Arg(): String? = null
    open fun name2Arg(): String? = null

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

    fun makeCellSumFunction2Args(a: APLValue, b: APLValue, pos: Position): APLValue {
        return when {
            a is APLSingleValue && b is APLSingleValue -> throw AssertionError("a and b cannot be singlevalue")
            a is APLSingleValue -> {
                when {
                    a is APLLong && b.specialisedType === ArrayMemberType.LONG && optimisationFlags.is2ALongLong ->
                        LongArraySum2ArgsLeftScalar(this, a.value, b, pos)
                    b.isScalar() -> EnclosedAPLValue.make(makeCellSumFunction2Args(a, b.valueAt(0), pos))
                    else ->
                        GenericArraySum2Args(this, a, b, pos)
                }
            }
            b is APLSingleValue -> {
                when {
                    b is APLLong && a.specialisedType === ArrayMemberType.LONG && optimisationFlags.is2ALongLong ->
                        LongArraySum2ArgsRightScalar(this, a, b.value, pos)
                    a.isScalar() -> EnclosedAPLValue.make(makeCellSumFunction2Args(a.valueAt(0), b, pos))
                    else ->
                        GenericArraySum2Args(this, a, b, pos)
                }
            }
            a.rank == 0 && b.rank == 0 -> EnclosedAPLValue.make(makeCellSumFunction2Args(a.valueAt(0), b.valueAt(0), pos))
            a.specialisedType === ArrayMemberType.LONG && b.specialisedType === ArrayMemberType.LONG && optimisationFlags.is2ALongLong ->
                LongArraySum2Args(this, a, b, pos)
            a.specialisedType === ArrayMemberType.DOUBLE && b.specialisedType === ArrayMemberType.DOUBLE && optimisationFlags.is2ADoubleDouble ->
                DoubleArraySum2Args(this, a, b, pos)
            else ->
                GenericArraySum2Args(this, a, b, pos)
        }
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
        val a0 = a.unwrapDeferredValue()
        val b0 = b.unwrapDeferredValue()

        if (a0 is APLSingleValue && b0 is APLSingleValue) {
            return combine2Arg(a0, b0)
        }

        if (axis != null) {
            val aDimensions = a0.dimensions
            val bDimensions = b0.dimensions

            val axisInt = axis.ensureNumber(pos2Arg).asInt()

            fun computeTransformation(baseVal: APLValue, d1: Dimensions, d2: Dimensions): APLValue {
                ensureValidAxis(axisInt, d2, pos2Arg)
                if (d1[0] != d2[axisInt]) {
                    throwAPLException(
                        InvalidDimensionsException(
                            "Dimensions of A does not match dimensions of B across axis ${axisInt}", pos2Arg
                        )
                    )
                }
                val d = d2.remove(axisInt).insert(d2.size - 1, d2[axisInt])
                val transposeAxis = IntArray(d2.size) { i ->
                    when {
                        i == d2.size - 1 -> axisInt
                        i < axisInt -> i
                        else -> i + 1
                    }
                }
                return TransposedAPLValue(transposeAxis, ResizedArrayImpls.makeResizedArray(d, baseVal), pos2Arg)
            }

            // When an axis is given, one of the arguments must be rank 1, and its dimension must be equal to the
            // dimension of the other arguments across the axis
            val (a1, b1) = when {
                aDimensions.size == 1 && bDimensions.size == 1 -> {
                    if (axisInt == 0) Pair(a0, b0) else throwAPLException(IllegalAxisException(axisInt, aDimensions, pos2Arg))
                }
                aDimensions.size == 1 -> Pair(computeTransformation(a0, aDimensions, bDimensions), b0)
                bDimensions.size == 1 -> Pair(a0, computeTransformation(b0, bDimensions, aDimensions))
                else -> throwAPLException(APLIllegalArgumentException("When specifying an axis, A or B has ro be rank 1", pos2Arg))
            }

            return makeCellSumFunction2Args(a1, b1, pos2Arg)
        } else {
            return makeCellSumFunction2Args(a0, b0, pos2Arg)
        }
    }

    open fun combine1Arg(a: APLSingleValue): APLValue = throwAPLException(Unimplemented1ArgException(pos1Arg))
    open fun combine1ArgLong(a: Long): Long = throw IllegalStateException("Optimisation not implemented for: ${this::class.simpleName}")
    open fun combine1ArgDouble(a: Double): Double =
        throw IllegalStateException("Optimisation not implemented for: ${this::class.simpleName}")

    open fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue = throwAPLException(Unimplemented2ArgException(pos2Arg))
    open fun combine2ArgLong(a: Long, b: Long): Long =
        throw IllegalStateException("Optimisation not implemented for: ${this::class.simpleName}")

    open fun combine2ArgDouble(a: Double, b: Double): Double =
        throw IllegalStateException("Optimisation not implemented for: ${this::class.simpleName}")

    override fun eval2ArgLongLong(context: RuntimeContext, a: Long, b: Long, axis: APLValue?) = combine2ArgLong(a, b)
    override fun eval2ArgDoubleDouble(context: RuntimeContext, a: Double, b: Double, axis: APLValue?) = combine2ArgDouble(a, b)
}

abstract class MathNumericCombineAPLFunction(pos: Position) : MathCombineAPLFunction(pos) {
    override fun combine1Arg(a: APLSingleValue): APLValue = numberCombine1Arg(a.ensureNumber(pos1Arg))
    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue =
        numberCombine2Arg(a.ensureNumber(pos2Arg), b.ensureNumber(pos2Arg))

    open fun numberCombine1Arg(a: APLNumber): APLValue = throwAPLException(Unimplemented1ArgException(pos1Arg))
    open fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue = throwAPLException(Unimplemented2ArgException(pos2Arg))
}

class AddAPLFunction : APLFunctionDescriptor {
    class AddAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> x.makeAPLNumber() },
                { x -> x.makeAPLNumber() },
                { x -> Complex(x.real, -x.imaginary).makeAPLNumber() })
        }

        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> (x + y).makeAPLNumber() },
                { x, y -> (x + y).makeAPLNumber() },
                { x, y -> (x + y).makeAPLNumber() })
        }

        override fun combine1ArgLong(a: Long) = a
        override fun combine1ArgDouble(a: Double) = a

        override fun combine2ArgLong(a: Long, b: Long) = a + b
        override fun combine2ArgDouble(a: Double, b: Double) = a + b

        override fun identityValue() = APLLONG_0
        override fun deriveBitwise() = BitwiseXorFunction()

        override val optimisationFlags
            get() = OptimisationFlags(
                OPTIMISATION_FLAG_1ARG_LONG or
                        OPTIMISATION_FLAG_1ARG_DOUBLE or
                        OPTIMISATION_FLAG_2ARG_LONG_LONG or
                        OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE)
    }

    override fun make(pos: Position) = AddAPLFunctionImpl(pos.withName("add"))
}

class SubAPLFunction : APLFunctionDescriptor {
    class SubAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> (-x).makeAPLNumber() },
                { x -> (-x).makeAPLNumber() },
                { x -> (-x).makeAPLNumber() })
        }

        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> (x - y).makeAPLNumber() },
                { x, y -> (x - y).makeAPLNumber() },
                { x, y -> (x - y).makeAPLNumber() })
        }

        override fun combine1ArgLong(a: Long) = -a
        override fun combine1ArgDouble(a: Double) = -a
        override fun combine2ArgLong(a: Long, b: Long) = a - b
        override fun combine2ArgDouble(a: Double, b: Double) = a - b

        override fun identityValue() = APLLONG_0
        override fun deriveBitwise() = BitwiseXorFunction()

        override val optimisationFlags
            get() = OptimisationFlags(
                OPTIMISATION_FLAG_1ARG_LONG or
                        OPTIMISATION_FLAG_1ARG_DOUBLE or
                        OPTIMISATION_FLAG_2ARG_LONG_LONG or
                        OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE)
    }

    override fun make(pos: Position) = SubAPLFunctionImpl(pos.withName("subtract"))
}

class MulAPLFunction : APLFunctionDescriptor {
    class MulAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> x.sign.toLong().makeAPLNumber() },
                { x -> x.sign.makeAPLNumber() },
                { x -> x.signum().makeAPLNumber() })
        }

        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> (x * y).makeAPLNumber() },
                { x, y -> (x * y).makeAPLNumber() },
                { x, y -> (x * y).makeAPLNumber() })
        }

        override fun identityValue() = APLLONG_1
        override fun deriveBitwise() = BitwiseAndFunction()

        override fun combine1ArgLong(a: Long) = a.sign.toLong()
        override fun combine1ArgDouble(a: Double) = a.sign

        override fun combine2ArgLong(a: Long, b: Long) = a * b
        override fun combine2ArgDouble(a: Double, b: Double) = a * b

        override val optimisationFlags get() = OptimisationFlags(OPTIMISATION_FLAG_1ARG_LONG or OPTIMISATION_FLAG_1ARG_DOUBLE or OPTIMISATION_FLAG_2ARG_LONG_LONG or OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE)

        override fun name1Arg() = "magnitude"
        override fun name2Arg() = "multiply"
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
                { x -> if (x == Complex.ZERO) APLLONG_0 else x.reciprocal().makeAPLNumber() })
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
                { x, y -> if (y == Complex.ZERO) APLDOUBLE_0 else (x / y).makeAPLNumber() })
        }

        override fun combine1ArgDouble(a: Double) = 1.0 / a
        override fun combine2ArgDouble(a: Double, b: Double) = a / b

        override fun identityValue() = APLLONG_1

        override val optimisationFlags get() = OptimisationFlags(OPTIMISATION_FLAG_1ARG_DOUBLE or OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE)
    }

    override fun make(pos: Position) = DivAPLFunctionImpl(pos.withName("divide"))
}

class NotAPLFunction : APLFunctionDescriptor {
    class NotAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(
                pos,
                a,
                { x -> notOp(x, pos1Arg).makeAPLNumber() },
                { x -> notOp(x.toLong(), pos1Arg).makeAPLNumber() },
                { x ->
                    if (x.imaginary == 0.0) {
                        notOp(x.real.toLong(), pos1Arg).makeAPLNumber()
                    } else {
                        throwAPLException(APLIncompatibleDomainsException("Operation not supported for complex", pos1Arg))
                    }
                })
        }

        private fun notOp(v: Long, pos: Position): Long {
            val result = when (v) {
                0L -> 1L
                1L -> 0L
                else -> throwAPLException(APLIncompatibleDomainsException("Operation not supported for value", pos))
            }
            return result
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            if (axis != null) {
                throwAPLException(AxisNotSupported(pos))
            }
            val a1 = a.arrayify()
            if (a1.dimensions.size != 1) {
                throwAPLException(InvalidDimensionsException("Left argument must be a scalar or a 1-dimensional array", pos2Arg))
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

        override fun combine1ArgLong(a: Long) = notOp(a, pos1Arg)

        override fun deriveBitwise() = BitwiseNotFunction()

        override val optimisationFlags get() = OptimisationFlags(OPTIMISATION_FLAG_1ARG_LONG)

        override fun name1Arg() = "not"
        override fun name2Arg() = "without"
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
                { x -> hypot(x.real, x.imaginary).makeAPLNumber() })
        }

        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> opLong(x, y).makeAPLNumber() },
                { x, y -> opDouble(x, y).makeAPLNumber() },
                { _, _ -> TODO("Not implemented") })
        }

        private fun opLong(x: Long, y: Long) =
            if (x == 0L) y else (y % x).let { result -> (if (x < 0) -result else result) }

        private fun opDouble(x: Double, y: Double) =
            if (x == 0.0) y else (y % x).let { result -> (if (x < 0) -result else result) }

        override fun combine2ArgLong(a: Long, b: Long) = opLong(a, b)

        override val optimisationFlags get() = OptimisationFlags(OPTIMISATION_FLAG_2ARG_LONG_LONG)
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
                { x -> E.pow(x).makeAPLNumber() })
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
                { x, y -> x.pow(y).makeAPLNumber() })
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
                { x -> complexFloor(x).makeAPLNumber() })
        }

        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> if (x < y) x.makeAPLNumber() else y.makeAPLNumber() },
                { x, y -> if (x < y) x.makeAPLNumber() else y.makeAPLNumber() },
                { x, y -> (if (x.real < y.real || (x.real == y.real && x.imaginary < y.imaginary)) x else y).makeAPLNumber() },
                { x, y -> if (x < y) APLChar(x) else APLChar(y) })
        }

        override fun combine2ArgLong(a: Long, b: Long) = if (a < b) a else b
        override fun combine2ArgDouble(a: Double, b: Double) = if (a < b) a else b

        override val optimisationFlags get() = OptimisationFlags(OPTIMISATION_FLAG_2ARG_LONG_LONG or OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE)
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
                { x -> complexCeiling(x).makeAPLNumber() })
        }

        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> if (x > y) x.makeAPLNumber() else y.makeAPLNumber() },
                { x, y -> if (x > y) x.makeAPLNumber() else y.makeAPLNumber() },
                { x, y -> (if (x.real > y.real || (x.real == y.real && x.imaginary > y.imaginary)) x else y).makeAPLNumber() },
                { x, y -> if (x > y) APLChar(x) else APLChar(y) })
        }

        override fun combine2ArgLong(a: Long, b: Long) = if (a > b) a else b
        override fun combine2ArgDouble(a: Double, b: Double) = if (a > b) a else b

        override val optimisationFlags get() = OptimisationFlags(OPTIMISATION_FLAG_2ARG_LONG_LONG or OPTIMISATION_FLAG_2ARG_DOUBLE_DOUBLE)
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
                { x -> x.ln().makeAPLNumber() })
        }

        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y ->
                    if (x < 0 || y < 0) {
                        y.toDouble().toComplex().log(x.toDouble()).makeAPLNumber()
                    } else {
                        log(y.toDouble(), x.toDouble()).makeAPLNumber()
                    }
                },
                { x, y -> if (x < 0 || y < 0) y.toComplex().log(x.toComplex()).makeAPLNumber() else log(y, x).makeAPLNumber() },
                { x, y -> y.log(x).makeAPLNumber() })
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
                { x -> complexSin(x).makeAPLNumber() })
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
                { x -> complexCos(x).makeAPLNumber() })
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
                                            { x, y -> opLong(x, y).makeAPLNumber() },
                                            { x, y ->
                                                when {
                                                    x == 0.0 || y == 0.0 -> APLLONG_0
                                                    x == 1.0 || y == 1.0 -> APLLONG_1
                                                    else -> (x * (y / floatGcd(x, y))).makeAPLNumber()
                                                }
                                            },
                                            { x, y -> (y * (x / complexGcd(x, y))).nearestGaussian().makeAPLNumber() })
        }

        private fun opLong(x: Long, y: Long): Long {
            return when {
                x == 0L || y == 0L -> 0L
                x == 1L && x == 1L -> 1L
                else -> (x * (y / integerGcd(x, y)))
            }
        }

        override fun combine2ArgLong(a: Long, b: Long) = opLong(a, b)

        override fun deriveBitwise() = BitwiseAndFunction()

        override fun identityValue() = APLLONG_1

        override val optimisationFlags get() = OptimisationFlags(OPTIMISATION_FLAG_2ARG_LONG_LONG)
    }

    override fun make(pos: Position) = AndAPLFunctionImpl(pos)
}

class NandAPLFunction : APLFunctionDescriptor {
    class NandAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> opLong(x, y).makeAPLNumber() },
                { x, y -> opDouble(x, y).makeAPLNumber() },
                { _, _ -> throwIllegalArgument() })
        }

        private fun opLong(a: Long, b: Long) = when {
            a == 0L && b == 0L -> 1L
            a == 0L && b == 1L -> 1L
            a == 1L && b == 0L -> 1L
            a == 1L && b == 1L -> 0L
            else -> throwIllegalArgument()
        }

        private fun opDouble(a: Double, b: Double): Long {
            val x0 = a.toLong()
            val y0 = b.toLong()
            return when {
                x0 == 0L && y0 == 0L -> 1L
                x0 == 0L && y0 == 1L -> 1L
                x0 == 1L && y0 == 0L -> 1L
                x0 == 1L && y0 == 1L -> 0L
                else -> throwIllegalArgument()
            }
        }

        override fun combine2ArgLong(a: Long, b: Long) = opLong(a, b)

        override fun deriveBitwise() = BitwiseNandFunction()

        private fun throwIllegalArgument(): Nothing {
            throwAPLException(APLIllegalArgumentException("Arguments to nand must be 0 or 1", pos))
        }

        override val optimisationFlags get() = OptimisationFlags(OPTIMISATION_FLAG_2ARG_LONG_LONG)
    }

    override fun make(pos: Position) = NandAPLFunctionImpl(pos)
}

class NorAPLFunction : APLFunctionDescriptor {
    class NorAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> opLong(x, y).makeAPLNumber() },
                { x, y -> opDouble(x, y).makeAPLNumber() },
                { _, _ -> throwIllegalArgument() })
        }

        private fun opLong(x: Long, y: Long): Long {
            return when {
                x == 0L && y == 0L -> 1L
                x == 0L && y == 1L -> 0L
                x == 1L && y == 0L -> 0L
                x == 1L && y == 1L -> 0L
                else -> throwIllegalArgument()
            }
        }

        private fun opDouble(x: Double, y: Double): Long {
            val x0 = x.toLong()
            val y0 = y.toLong()
            return when {
                x0 == 0L && y0 == 0L -> 1L
                x0 == 0L && y0 == 1L -> 0L
                x0 == 1L && y0 == 0L -> 0L
                x0 == 1L && y0 == 1L -> 0L
                else -> throwIllegalArgument()
            }
        }

        override fun combine2ArgLong(a: Long, b: Long) = opLong(a, b)

        override fun deriveBitwise() = BitwiseNorFunction()

        private fun throwIllegalArgument(): Nothing {
            throwAPLException(APLIllegalArgumentException("Arguments to nor must be 0 or 1", pos))
        }
    }

    override fun make(pos: Position) = NorAPLFunctionImpl(pos)
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
                                            { x, y -> opLong(x, y).makeAPLNumber() },
                                            { x, y -> opDouble(x, y).makeAPLNumber() },
                                            { x, y -> complexGcd(x, y).makeAPLNumber() })
        }

        private fun opLong(x: Long, y: Long) = when {
            x == 0L && y == 0L -> 0
            (x == 0L || x == 1L) && y == 1L -> 1
            x == 1L && (y == 0L || y == 1L) -> 1
            else -> integerGcd(x, y)
        }

        private fun opDouble(x: Double, y: Double) = when {
            x == 0.0 && y == 0.0 -> 0.0
            (x == 0.0 || x == 1.0) && y == 1.0 -> 1.0
            x == 1.0 && (y == 0.0 || y == 1.0) -> 1.0
            else -> floatGcd(x, y)
        }

        override fun combine2ArgLong(a: Long, b: Long) = opLong(a, b)

        override fun identityValue() = APLLONG_0
        override fun deriveBitwise() = BitwiseOrFunction()

        override val optimisationFlags get() = OptimisationFlags(OPTIMISATION_FLAG_2ARG_LONG_LONG)
    }

    override fun make(pos: Position) = OrAPLFunctionImpl(pos)
}

class BinomialAPLFunction : APLFunctionDescriptor {
    class BinomialAPLFunctionImpl(pos: Position) : MathNumericCombineAPLFunction(pos) {
        override fun numberCombine1Arg(a: APLNumber): APLValue {
            return singleArgNumericRelationOperation(pos, a,
                                                     { x -> doubleGamma((x + 1).toDouble()).makeAPLNumber() },
                                                     { x -> doubleGamma(x + 1.0).makeAPLNumber() },
                                                     { x -> complexGamma(x + 1.0).makeAPLNumber() })
        }

        override fun numberCombine2Arg(a: APLNumber, b: APLNumber): APLValue {
            return numericRelationOperation(pos,
                                            a,
                                            b,
                                            { x, y -> doubleBinomial(x.toDouble(), y.toDouble()).makeAPLNumber() },
                                            { x, y -> doubleBinomial(x, y).makeAPLNumber() },
                                            { x, y -> complexBinomial(x, y).makeAPLNumber() })
        }
    }

    override fun make(pos: Position) = BinomialAPLFunctionImpl(pos)
}
