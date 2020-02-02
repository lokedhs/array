package array

class IncompatibleTypeException(message: String) : Exception(message)
class InvalidDimensionsException(message: String) : Exception(message)
class APLIndexOutOfBoundsException(message: String) : Exception(message)
class IllegalNumberFormat(message: String) : Exception(message)
class UnexpectedSymbol(ch: Int) : Exception("Unexpected symbol: $ch")
class UnexpectedToken(token: Token) : Exception("Unexpected token: $token")
class VariableNotAssigned(name: Symbol) : Exception("Variable not assigned: $name")
class IllegalAxisException(val axis: Int, val dimensions: Dimensions) : Exception("Axis $axis is not valid. Expected: ${dimensions.size}")

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
