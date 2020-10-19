package array

import array.complex.Complex

abstract class APLNumber : APLSingleValue() {
    override fun toString() = "APLNumber(${formatted(FormatStyle.PRETTY)})"
    override fun ensureNumber(pos: Position?) = this

    abstract fun asDouble(): Double
    abstract fun asLong(): Long
    abstract fun asComplex(): Complex

    abstract fun isComplex(): Boolean

    open fun asInt(): Int {
        val l = asLong()
        return if (l >= Int.MIN_VALUE && l <= Int.MAX_VALUE) {
            l.toInt()
        } else {
            throw IncompatibleTypeException("Value does not fit in an int: $l")
        }
    }

    override fun asBoolean() = asInt() != 0
}

class APLLong(val value: Long) : APLNumber() {
    override val aplValueType: APLValueType get() = APLValueType.INTEGER
    override fun asDouble() = value.toDouble()
    override fun asLong() = value
    override fun asComplex() = Complex(value.toDouble())
    override fun isComplex() = false
    override fun formatted(style: FormatStyle) =
        when (style) {
            FormatStyle.PLAIN -> value.toString()
            FormatStyle.READABLE -> value.toString()
            FormatStyle.PRETTY -> value.toString()
        }

    override fun compareEquals(reference: APLValue) = reference is APLLong && value == reference.value

    override fun compare(reference: APLValue, pos: Position?): Int {
        return when (reference) {
            is APLLong -> value.compareTo(reference.value)
            is APLDouble -> value.compareTo(reference.value)
            is APLComplex -> throwComplexComparisonException(pos)
            else -> super.compare(reference, pos)
        }
    }

    override fun toString() = "APLLong(${formatted(FormatStyle.PRETTY)})"

    override fun makeKey() = APLValue.APLValueKeyImpl(this, value)

    override fun asBoolean() = value != 0L
}

private fun throwComplexComparisonException(pos: Position?): Nothing {
    throw APLEvalException("Complex numbers does not have a total order", pos)
}

class APLDouble(val value: Double) : APLNumber() {
    override val aplValueType: APLValueType get() = APLValueType.FLOAT
    override fun asDouble() = value
    override fun asLong() = value.toLong()
    override fun asComplex() = Complex(value)
    override fun isComplex() = false

    override fun formatted(style: FormatStyle) =
        when (style) {
            FormatStyle.PLAIN -> value.toString()
            FormatStyle.PRETTY -> {
                // Kotlin native doesn't have a decent formatter, so we'll take the easy way out:
                // We'll check if the value fits in a Long and if it does, use it for rendering.
                // This is the easiest way to avoid displaying a decimal point for integers.
                // Let's hope this changes sooner rather than later.
                if (value.rem(1) == 0.0 && value <= Long.MAX_VALUE && value >= Long.MIN_VALUE) {
                    value.toLong().toString()
                } else {
                    value.toString()
                }
            }
            FormatStyle.READABLE -> if (value < 0) "¯" + (-value).toString() else value.toString()
        }

    override fun compareEquals(reference: APLValue) = reference is APLDouble && value == reference.value

    override fun compare(reference: APLValue, pos: Position?): Int {
        return when (reference) {
            is APLLong -> value.compareTo(reference.value)
            is APLDouble -> value.compareTo(reference.value)
            is APLComplex -> throwComplexComparisonException(pos)
            else -> super.compare(reference, pos)
        }
    }

    override fun toString() = "APLDouble(${formatted(FormatStyle.PRETTY)})"

    override fun makeKey() = APLValue.APLValueKeyImpl(this, value)

    override fun asBoolean() = value != 0.0
}

class NumberComplexException(value: Complex, pos: Position? = null) : IncompatibleTypeException("Number is complex: ${value}", pos)

class APLComplex(val value: Complex) : APLNumber() {
    override val aplValueType: APLValueType get() = APLValueType.COMPLEX

    override fun asDouble(): Double {
        if (value.imaginary != 0.0) {
            throw NumberComplexException(value)
        }
        return value.real
    }

    override fun asLong(): Long {
        if (value.imaginary != 0.0) {
            throw NumberComplexException(value)
        }
        return value.real.toLong()
    }

    override fun compareEquals(reference: APLValue) = reference is APLComplex && value == reference.value

    override fun asComplex() = value
    override fun isComplex() = value.imaginary != 0.0

    override fun formatted(style: FormatStyle) =
        when (style) {
            FormatStyle.PLAIN -> formatToAPL()
            FormatStyle.PRETTY -> formatToAPL()
            FormatStyle.READABLE -> formatToAPL()
        }

    private fun formatToAPL() = "${value.real}J${value.imaginary}"

    override fun makeKey() = APLValue.APLValueKeyImpl(this, value)

    override fun asBoolean() = value != Complex.ZERO
}

val APLLONG_0 = APLLong(0)
val APLLONG_1 = APLLong(1)

fun Int.makeAPLNumber() = APLLong(this.toLong())
fun Long.makeAPLNumber() = APLLong(this)
fun Double.makeAPLNumber() = APLDouble(this)
fun Complex.makeAPLNumber() = if (this.imaginary == 0.0) APLDouble(real) else APLComplex(this)
