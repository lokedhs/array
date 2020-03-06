package array.builtins

import array.*

class EqualsAPLFunction : MathCombineAPLFunction() {
    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
        if ((a is APLChar && b !is APLChar) || a !is APLChar && b is APLChar) {
            return makeBoolean(false)
        } else {
            return numericRelationOperation(
                a,
                b,
                { x, y -> makeBoolean(x == y) },
                { x, y -> makeBoolean(x == y) },
                { x, y -> makeBoolean(x == y) })
        }
    }

    override fun identityValue() = APLLong(1)
}

class NotEqualsAPLFunction : MathCombineAPLFunction() {
    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
        if ((a is APLChar && b !is APLChar) || a !is APLChar && b is APLChar) {
            return makeBoolean(true)
        } else {
            return numericRelationOperation(
                a,
                b,
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
    fnChar: ((aChar: Int, bChar: Int) -> APLValue)? = null
): APLValue {
    if (a is APLNumber && b is APLNumber) {
        return if (a is APLDouble || b is APLDouble) {
            fnDouble(a.asDouble(), b.asDouble())
        } else {
            fnLong(a.asLong(), b.asLong())
        }
    } else if (a is APLChar && b is APLChar) {
        if (fnChar == null) {
            throw IncompatibleTypeException("Function cannot be used with characters")
        }
        return fnChar(a.value, b.value)
    } else {
        throw IncompatibleTypeException("Incompatible argument types")
    }
}
