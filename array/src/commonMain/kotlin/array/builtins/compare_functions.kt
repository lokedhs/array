package array.builtins

import array.*
import array.complex.Complex

class EqualsAPLFunction : APLFunctionDescriptor {
    class EqualsAPLFunctionImpl(pos: Position) : MathCombineAPLFunction(pos) {
        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return if ((a is APLChar && b !is APLChar) || a !is APLChar && b is APLChar) {
                makeBoolean(false)
            } else {
                numericRelationOperation(
                    pos,
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

    override fun make(pos: Position) = EqualsAPLFunctionImpl(pos)
}

class NotEqualsAPLFunction : APLFunctionDescriptor {
    class NotEqualsAPLFunctionImpl(pos: Position) : MathCombineAPLFunction(pos) {
        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return if ((a is APLChar && b !is APLChar) || a !is APLChar && b is APLChar) {
                makeBoolean(true)
            } else {
                numericRelationOperation(
                    pos,
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

    override fun make(pos: Position) = NotEqualsAPLFunctionImpl(pos)
}

class LessThanAPLFunction : APLFunctionDescriptor {
    class LessThanAPLFunctionImpl(pos: Position) : MathCombineAPLFunction(pos) {
        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> makeBoolean(x < y) },
                { x, y -> makeBoolean(x < y) },
                { x, y -> makeBoolean(if (x.real == y.real) x.imaginary < y.imaginary else x.real < y.real) },
                { x, y -> makeBoolean(x < y) })
        }

        override fun identityValue() = APLLong(0)
    }

    override fun make(pos: Position) = LessThanAPLFunctionImpl(pos)
}

class GreaterThanAPLFunction : APLFunctionDescriptor {
    class GreaterThanAPLFunctionImpl(pos: Position) : MathCombineAPLFunction(pos) {
        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> makeBoolean(x > y) },
                { x, y -> makeBoolean(x > y) },
                { x, y -> makeBoolean(if (x.real == y.real) x.imaginary > y.imaginary else x.real > y.real) },
                { x, y -> makeBoolean(x > y) })
        }

        override fun identityValue() = APLLong(0)
    }

    override fun make(pos: Position) = GreaterThanAPLFunctionImpl(pos)
}

class LessThanEqualAPLFunction : APLFunctionDescriptor {
    class LessThanEqualAPLFunctionImpl(pos: Position) : MathCombineAPLFunction(pos) {
        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> makeBoolean(x <= y) },
                { x, y -> makeBoolean(x <= y) },
                { x, y -> makeBoolean(if (x.real == y.real) x.imaginary <= y.imaginary else x.real < y.real) },
                { x, y -> makeBoolean(x <= y) })
        }

        override fun identityValue() = APLLong(1)
    }

    override fun make(pos: Position) = LessThanEqualAPLFunctionImpl(pos)
}

class GreaterThanEqualAPLFunction : APLFunctionDescriptor {
    class GreaterThanEqualAPLFunctionImpl(pos: Position) : MathCombineAPLFunction(pos) {
        override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
            return numericRelationOperation(
                pos,
                a,
                b,
                { x, y -> makeBoolean(x >= y) },
                { x, y -> makeBoolean(x >= y) },
                { x, y -> makeBoolean(if (x.real == y.real) x.imaginary >= y.imaginary else x.real > y.real) },
                { x, y -> makeBoolean(x >= y) })
        }

        override fun identityValue() = APLLong(1)
    }

    override fun make(pos: Position) = GreaterThanEqualAPLFunctionImpl(pos)
}

fun makeBoolean(value: Boolean): APLValue {
    return APLLong(if (value) 1 else 0)
}

fun numericRelationOperation(
    pos: Position,
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
                throw IncompatibleTypeException("Function cannot be used with characters", pos)
            }
            fnChar(a.value, b.value)
        }
        else -> throw IncompatibleTypeException("Incompatible argument types", pos)
    }
}

fun singleArgNumericRelationOperation(
    pos: Position,
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
                throw IncompatibleTypeException("Function cannot be used with characters", pos)
            } else {
                fnChar(a.value)
            }
        }
        else -> throw IncompatibleTypeException("Incompatible argument types", pos)
    }
}
