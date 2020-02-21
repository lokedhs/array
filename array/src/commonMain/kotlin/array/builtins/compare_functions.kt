package array.builtins

import array.*

class EqualsAPLFunction : MathCombineAPLFunction() {
    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
        if (a is APLNumber && b is APLNumber) {
            return numericRelationOperation(
                a,
                b,
                { x, y -> makeBoolean(x == y) },
                { x, y -> makeBoolean(x == y) })
        } else {
            throw IncompatibleTypeException("Incompatible argument types")
        }
    }
}

class NotEqualsAPLFunction : MathCombineAPLFunction() {
    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
        if (a is APLNumber && b is APLNumber) {
            return numericRelationOperation(
                a,
                b,
                { x, y -> makeBoolean(x != y) },
                { x, y -> makeBoolean(x != y) })
        } else {
            throw IncompatibleTypeException("Incompatible argument types")
        }
    }
}

class LessThanAPLFunction : MathCombineAPLFunction() {
    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
        if (a is APLNumber && b is APLNumber) {
            return numericRelationOperation(
                a,
                b,
                { x, y -> makeBoolean(x < y) },
                { x, y -> makeBoolean(x < y) })
        } else {
            throw IncompatibleTypeException("Incompatible argument types")
        }
    }
}

class GreaterThanAPLFunction : MathCombineAPLFunction() {
    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
        if (a is APLNumber && b is APLNumber) {
            return numericRelationOperation(
                a,
                b,
                { x, y -> makeBoolean(x > y) },
                { x, y -> makeBoolean(x > y) })
        } else {
            throw IncompatibleTypeException("Incompatible argument types")
        }
    }
}

class LessThanEqualAPLFunction : MathCombineAPLFunction() {
    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
        if (a is APLNumber && b is APLNumber) {
            return numericRelationOperation(
                a,
                b,
                { x, y -> makeBoolean(x <= y) },
                { x, y -> makeBoolean(x <= y) })
        } else {
            throw IncompatibleTypeException("Incompatible argument types")
        }
    }
}

class GreaterThanEqualAPLFunction : MathCombineAPLFunction() {
    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue {
        if (a is APLNumber && b is APLNumber) {
            return numericRelationOperation(
                a,
                b,
                { x, y -> makeBoolean(x >= y) },
                { x, y -> makeBoolean(x >= y) })
        } else {
            throw IncompatibleTypeException("Incompatible argument types")
        }
    }
}

fun makeBoolean(value: Boolean): APLValue {
    return APLLong(if (value) 1 else 0)
}

fun numericRelationOperation(
    a: APLNumber,
    b: APLNumber,
    fnLong: (al: Long, bl: Long) -> APLValue,
    fnDouble: (ad: Double, bd: Double) -> APLValue
): APLValue {
    return if (a is APLDouble || b is APLDouble) {
        fnDouble(a.asDouble(), b.asDouble())
    } else {
        fnLong(a.asLong(), b.asLong())
    }
}
