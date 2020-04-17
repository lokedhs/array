package array

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
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
open class IncompatibleTypeException(message: String, pos: Position? = null) : APLEvalException(message, pos)
class InvalidDimensionsException(message: String, pos: Position? = null) : APLEvalException(message, pos)
class APLIndexOutOfBoundsException(message: String, pos: Position? = null) : APLEvalException("Index out of bounds: ${message}", pos)
class IllegalNumberFormat(message: String, pos: Position? = null) : APLEvalException(message, pos)
class UnexpectedSymbol(ch: Int, pos: Position? = null) : APLEvalException("Unexpected symbol: '${charToString(ch)}' (${ch})", pos)
class VariableNotAssigned(name: Symbol, pos: Position? = null) : APLEvalException("Variable not assigned: $name", pos)
class IllegalAxisException(axis: Int, dimensions: Dimensions, pos: Position? = null) :
    APLEvalException("Axis $axis is not valid. Expected: ${dimensions.size}", pos)

class AxisNotSupported(pos: Position) : APLEvalException("Function does not support axis specifier", pos)

class APLIllegalArgumentException(message: String, pos: Position? = null) : APLEvalException(message, pos)
class APLIncompatibleDomainsException(message: String, pos: Position? = null) : APLEvalException(message, pos)
class Unimplemented1ArgException(pos: Position? = null) : APLEvalException("Function cannot be called with one argument", pos)
class Unimplemented2ArgException(pos: Position? = null) : APLEvalException("Function cannot be called with two arguments", pos)

open class ParseException(message: String, pos: Position? = null) : APLGenericException(message, pos)
class UnexpectedToken(token: Token, pos: Position? = null) : ParseException("Unexpected token: $token", pos)
class IncompatibleTypeParseException(message: String, pos: Position? = null) : ParseException(message, pos)

@OptIn(ExperimentalContracts::class)
inline fun unless(cond: Boolean, fn: () -> Unit) {
    contract { callsInPlace(fn, InvocationKind.AT_MOST_ONCE) }
    if (!cond) {
        fn()
    }
}

fun Long.plusMod(divisor: Long): Long {
    val v = this % divisor
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
            buf.append(stringIntersperse(values.asSequence().map { it.toString() }, ", "))
            buf.append("]")
            return buf.toString()
        }
    }
}

@OptIn(ExperimentalContracts::class)
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

fun stringIntersperse(list: Sequence<String>, separator: String): String {
    val buf = StringBuilder()
    var first = true
    for (v in list) {
        if (first) {
            first = false
        } else {
            buf.append(separator)
        }
        buf.append(v)
    }
    return buf.toString()
}

fun checkAxisPositionIsInRange(posAlongAxis: Int, dimensions: Dimensions, axis: Int) {
    if (posAlongAxis < 0 || posAlongAxis >= dimensions[axis]) {
        throw APLIndexOutOfBoundsException("Position ${posAlongAxis} does not fit in dimensions ${Arrays.toString(dimensions.dimensions.toTypedArray())} axis ${axis}")
    }
}

sealed class Either<out A, out B> {
    class Left<A>(val value: A) : Either<A, Nothing>()
    class Right<B>(val value: B) : Either<Nothing, B>()
}

class Optional<out T> private constructor(val content: T?) {
    fun hasValue() = content != null

    fun valueOrThrow() = content ?: throw IllegalStateException("Value not assigned")

    fun <R> withValueIfExists(fn: (T) -> R): R? {
        return if (content != null) fn(content) else null
    }

    fun <R> withValueOrThrow(fn: (T) -> R): R {
        return fn(valueOrThrow())
    }

    fun <R> withValue(fnValue: (T) -> R, fnNoValue: () -> R): R {
        return if (content != null) {
            fnValue(content)
        } else {
            fnNoValue()
        }
    }

    companion object {
        fun <T> make(value: T) = Optional(value)
        fun empty() = Optional(null)
    }
}
