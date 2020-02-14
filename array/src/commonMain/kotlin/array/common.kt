package array

open class APLEvalException(message: String) : Exception(message)
class IncompatibleTypeException(message: String) : APLEvalException(message)
class InvalidDimensionsException(message: String) : APLEvalException(message)
class APLIndexOutOfBoundsException(message: String) : APLEvalException(message)
class IllegalNumberFormat(message: String) : APLEvalException(message)
class UnexpectedSymbol(ch: Int) : APLEvalException("Unexpected symbol: $ch")
class UnexpectedToken(token: Token) : APLEvalException("Unexpected token: $token")
class VariableNotAssigned(name: Symbol) : APLEvalException("Variable not assigned: $name")
class IllegalAxisException(val axis: Int, val dimensions: Dimensions) :
    APLEvalException("Axis $axis is not valid. Expected: ${dimensions.size}")

inline fun unless(cond: Boolean, fn: () -> Unit) {
    if (!cond) {
        fn()
    }
}

class Arrays {
    companion object {
        fun <T> equals(a: Array<T>, b: Array<T>): Boolean {
            if (a === b) {
                return true
            }
            val aLength = a.size
            val bLength = b.size
            unless(aLength == bLength) {
                return false
            }

            for (i in 0 until aLength) {
                if (a[i] != b[i]) {
                    return false
                }
            }
            return true
        }

        fun toString(values: Array<*>): String {
            val buf = StringBuilder()
            buf.append("[")
            values.forEach { v ->
                buf.append(" ")
                buf.append(v.toString())
            }
            buf.append(" ]")
            return buf.toString()
        }
    }
}

fun assertx(condition: Boolean, message: String = "Assertion error") {
    if (!condition) {
        throw AssertionError(message)
    }
}

fun ensureValidAxis(axis: Int, dimensions: Dimensions) {
    if (axis < 0 || axis >= dimensions.size) {
        throw IllegalAxisException(axis, dimensions)
    }
}

fun resolveAxis(axisParam: APLValue?, arg: APLValue): Int {
    if (axisParam != null && axisParam.rank() != 1) {
        throw IncompatibleTypeException("Axis should be a single integer")
    }
    val v = if (axisParam == null) {
        arg.dimensions().size - 1
    } else {
        axisParam.ensureNumber().asInt()
    }
    ensureValidAxis(v, arg.dimensions())
    return v
}

inline fun <T, R> List<T>.reduceWithInitial(fn: (R, T) -> R, initial: R): R {
    var curr = initial
    for (element in this) {
        curr = fn(curr, element)
    }
    return curr
}

fun <T> stringIntersperse(list: List<T>, separator: String, fn: (T) -> String): String {
    val buf = StringBuilder()
    var first = true
    for (v in list) {
        if (first) {
            first = false
        } else {
            buf.append(separator)
        }
        buf.append(fn(v))
    }
    return buf.toString()
}

fun checkIndexRange(array: Array<*>, index: Int) {
    if (index < 0 || index >= array.size) {
        throw IndexOutOfBoundsException("Index does not fit in array. index=${index}, size=${array.size}")
    }
}

inline fun <reified T> copyArrayAndRemove(array: Array<T>, toRemove: Int): Array<T> {
    checkIndexRange(array, toRemove)
    return Array(array.size - 1) { index ->
        if (index < toRemove) array[index] else array[index + 1]
    }
}

inline fun <reified T> copyArrayAndInsert(array: Array<T>, pos: Int, newValue: T): Array<T> {
    return Array(array.size + 1) { index ->
        when {
            index < pos -> array[index]
            index > pos -> array[index - 1]
            else -> newValue
        }
    }
}
