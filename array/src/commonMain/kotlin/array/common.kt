package array

open class APLEvalException(message: String) : Exception(message)
class IncompatibleTypeException(message: String) : APLEvalException(message)
class InvalidDimensionsException(message: String) : APLEvalException(message)
class APLIndexOutOfBoundsException(message: String) : APLEvalException(message)
class IllegalNumberFormat(message: String) : APLEvalException(message)
class UnexpectedSymbol(ch: Int) : APLEvalException("Unexpected symbol: $ch")
class UnexpectedToken(token: Token) : APLEvalException("Unexpected token: $token")
class VariableNotAssigned(name: Symbol) : APLEvalException("Variable not assigned: $name")
class IllegalAxisException(val axis: Int, val dimensions: Dimensions) : APLEvalException("Axis $axis is not valid. Expected: ${dimensions.size}")

inline fun unless(cond: Boolean, fn: () -> Unit) {
    if(!cond) {
        fn()
    }
}

class Arrays {
    companion object {
        fun <T> equals(a: Array<T>, b: Array<T>): Boolean {
            if(a === b) {
                return true
            }
            val aLength = a.size
            val bLength = b.size
            unless(aLength == bLength) {
                return false
            }

            for(i in 0 until aLength) {
                if(a[i] != b[i]) {
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
    if(!condition) {
        throw AssertionError(message)
    }
}

fun ensureValidAxis(axis: Int, dimensions: Dimensions) {
    if(axis < 0 || axis >= dimensions.size) {
        throw IllegalAxisException(axis, dimensions)
    }
}

inline fun <T,R> List<T>.reduceWithInitial(fn: (R, T) -> R, initial: R): R {
    var curr = initial
    for(element in this) {
        curr = fn(curr, element)
    }
    return curr
}
