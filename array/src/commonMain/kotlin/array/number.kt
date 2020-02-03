package array

abstract class APLNumber : APLSingleValue() {
    override fun toString() = "APLNumber(${formatted()})"
    override fun ensureNumber() = this

    abstract fun asDouble(): Double
    abstract fun asLong(): Long

    open fun asInt(): Int {
        val l = asLong()
        return if (l >= Int.MIN_VALUE && l <= Int.MAX_VALUE) {
            l.toInt()
        } else {
            throw IncompatibleTypeException("Value does not fit in an int: $l")
        }
    }
}

class APLLong(val value: Long) : APLNumber() {
    override fun asDouble() = value.toDouble()
    override fun asLong() = value
    override fun formatted() = value.toString()
    override fun toString() = "APLLong(${formatted()})"
}

class APLDouble(val value: Double) : APLNumber() {
    override fun asDouble() = value
    override fun asLong() = value.toLong()
    override fun formatted(): String {
        // Kotlin native doesn't have a decent formatter, so we'll take the easy way out:
        // We'll check if the value fits in a Long and if it does, use it for rendering.
        // This is the easiest way to avoid displaying a decimal point for integers.
        // Let's hope this changes sooner rather than later.
        return if (value.rem(1) == 0.0 && value <= Long.MAX_VALUE && value >= Long.MIN_VALUE) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    override fun toString() = "APLDouble(${formatted()})"
}
