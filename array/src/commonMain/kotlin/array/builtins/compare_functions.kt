package array.builtins

import array.*
import array.complex.Complex

class EqualsAPLFunction : MathCombineAPLFunction() {
    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
        return if ((a is APLChar && b !is APLChar) || a !is APLChar && b is APLChar) {
            makeBoolean(false)
        } else {
            numericRelationOperation(
                a,
                b,
                { x, y -> makeBoolean(x == y) },
                { x, y -> makeBoolean(x == y) },
                { x, y -> makeBoolean(x == y) },
                { x, y -> makeBoolean(x == y) })
        }
    }

    override fun identityValue() = APLLong(1)
}

class NotEqualsAPLFunction : MathCombineAPLFunction() {
    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
        return if ((a is APLChar && b !is APLChar) || a !is APLChar && b is APLChar) {
            makeBoolean(true)
        } else {
            numericRelationOperation(
                a,
                b,
                { x, y -> makeBoolean(x != y) },
                { x, y -> makeBoolean(x != y) },
                { x, y -> makeBoolean(x != y) },
                { x, y -> makeBoolean(x != y) })
        }
    }

    override fun identityValue() = APLLong(0)
}

class LessThanAPLFunction : MathCombineAPLFunction() {
    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
        return numericRelationOperation(
            a,
            b,
            { x, y -> makeBoolean(x < y) },
            { x, y -> makeBoolean(x < y) },
            { x, y -> makeBoolean(if (x.real == y.real) x.imaginary < y.imaginary else x.real < y.real) },
            { x, y -> makeBoolean(x < y) })
    }

    override fun identityValue() = APLLong(0)
}

class GreaterThanAPLFunction : MathCombineAPLFunction() {
    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
        return numericRelationOperation(
            a,
            b,
            { x, y -> makeBoolean(x > y) },
            { x, y -> makeBoolean(x > y) },
            { x, y -> makeBoolean(if (x.real == y.real) x.imaginary > y.imaginary else x.real > y.real) },
            { x, y -> makeBoolean(x > y) })
    }

    override fun identityValue() = APLLong(0)
}

class LessThanEqualAPLFunction : MathCombineAPLFunction() {
    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
        return numericRelationOperation(
            a,
            b,
            { x, y -> makeBoolean(x <= y) },
            { x, y -> makeBoolean(x <= y) },
            { x, y -> makeBoolean(if (x.real == y.real) x.imaginary <= y.imaginary else x.real < y.real) },
            { x, y -> makeBoolean(x <= y) })
    }

    override fun identityValue() = APLLong(1)
}

class GreaterThanEqualAPLFunction : MathCombineAPLFunction() {
    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
        return numericRelationOperation(
            a,
            b,
            { x, y -> makeBoolean(x >= y) },
            { x, y -> makeBoolean(x >= y) },
            { x, y -> makeBoolean(if (x.real == y.real) x.imaginary >= y.imaginary else x.real > y.real) },
            { x, y -> makeBoolean(x >= y) })
    }

    override fun identityValue() = APLLong(1)
}

fun makeBoolean(value: Boolean): APLValue {
    return APLLong(if (value) 1 else 0)
}

fun numericRelationOperation(
    a: APLSingleValue,
    b: APLSingleValue,
    fnLong: (al: Long, bl: Long) -> APLValue,
    fnDouble: (ad: Double, bd: Double) -> APLValue,
    fnComplex: (ac: Complex, bc: Complex) -> APLValue,
    fnChar: ((aChar: Int, bChar: Int) -> APLValue)? = null
): APLValue {
    return when {
        a is APLNumber && b is APLNumber -> {
            when {
                a is APLComplex || b is APLComplex -> fnComplex(a.asComplex(), b.asComplex())
                a is APLDouble || b is APLDouble -> fnDouble(a.asDouble(), b.asDouble())
                else -> fnLong(a.asLong(), b.asLong())
            }
        }
        a is APLChar && b is APLChar -> {
            if (fnChar == null) {
                throw IncompatibleTypeException("Function cannot be used with characters")
            }
            fnChar(a.value, b.value)
        }
        else -> throw IncompatibleTypeException("Incompatible argument types")
    }
}

fun singleArgNumericRelationOperation(
    a: APLSingleValue,
    fnLong: (a: Long) -> APLValue,
    fnDouble: (a: Double) -> APLValue,
    fnComplex: (a: Complex) -> APLValue,
    fnChar: ((a: Int) -> APLValue)? = null
): APLValue {
    return when (a) {
        is APLLong -> fnLong(a.asLong())
        is APLDouble -> fnDouble(a.asDouble())
        is APLComplex -> fnComplex(a.asComplex())
        is APLChar -> {
            if (fnChar == null) {
                throw IncompatibleTypeException("Function cannot be used with characters")
            } else {
                fnChar(a.value)
            }
        }
        else -> throw IncompatibleTypeException("Incompatible argument types")
    }
}
