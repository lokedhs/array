package array

import kotlin.contracts.contract

open class APLGenericException(message: String, val pos: Position? = null, cause: Throwable? = null) : Exception(message, cause) {
    fun formattedError(): String {
        val exceptionText = message ?: "no message"
        return if (pos != null) {
            "Error at: ${pos.line + 1}:${pos.col + 1}: ${exceptionText}"
        } else {
            "Error: ${exceptionText}"
        }
    }
}

open class APLEvalException(message: String, pos: Position? = null) : APLGenericException(message, pos)
open class IncompatibleTypeException(message: String) : APLEvalException(message)
class InvalidDimensionsException(message: String, pos: Position? = null) : APLEvalException(message, pos)
class APLIndexOutOfBoundsException(message: String) : APLEvalException(message)
class IllegalNumberFormat(message: String) : APLEvalException(message)
class UnexpectedSymbol(ch: Int) : APLEvalException("Unexpected symbol: $ch")
class VariableNotAssigned(name: Symbol) : APLEvalException("Variable not assigned: $name")
class IllegalAxisException(axis: Int, dimensions: Dimensions) : APLEvalException("Axis $axis is not valid. Expected: ${dimensions.size}")
class APLIllegalArgumentException(message: String) : APLEvalException(message)
class APLIncompatibleDomainsException(message: String) : APLEvalException(message)
class Unimplemented1ArgException : APLEvalException("Function cannot be called with one argument")
class Unimplemented2ArgException : APLEvalException("Function cannot be called with two arguments")

open class ParseException(message: String) : APLGenericException(message)
class UnexpectedToken(token: Token) : ParseException("Unexpected token: $token")

inline fun unless(cond: Boolean, fn: () -> Unit) {
    if (!cond) {
        fn()
    }
}

fun plusMod(value: Long, divisor: Long): Long {
    val v = value % divisor
    return if (v < 0) divisor + v else v
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

        fun equals(a: IntArray, b: IntArray): Boolean {
            if (a === b) {
                return true
            }

            if (a.size != b.size) {
                return false
            }

            for (i in a.indices) {
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

@OptIn(kotlin.contracts.ExperimentalContracts::class)
fun assertx(condition: Boolean, message: String = "Assertion error") {
    contract { returns() implies condition }
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

inline fun <T, R : Comparable<R>> List<T>.maxValueBy(fn: (T) -> R): R {
    if (this.isEmpty()) {
        throw RuntimeException("call to maxValueBy on empty list")
    }
    var currMax: R? = null
    this.forEach { e ->
        val res = fn(e)
        if (currMax == null || res > currMax!!) {
            currMax = res
        }
    }
    return currMax!!
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

fun checkIndexRange(array: IntArray, index: Int) {
    if (index < 0 || index >= array.size) {
        throw IndexOutOfBoundsException("Index does not fit in array. index=${index}, size=${array.size}")
    }
}
