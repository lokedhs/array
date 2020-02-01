package array

abstract class APLNumber : APLSingleValue() {
    override fun toString() = "APLNumber(${formatted()})"
}

class APLLong(val value: Long) : APLNumber() {
    override fun asDouble() = value.toDouble()
    override fun formatted() = value.toString()
    override fun toString() = "APLLong(${formatted()})"
}

class APLDouble(val value: Double) : APLNumber() {
    override fun asDouble() = value
    override fun formatted(): String {
        // Kotlin native doesn't have a decent formatter, so we'll take the easy way out:
        // We'll check if the value fits in a Long and if it does, use it for rendering.
        // This is the easiest way to avoid displaying a decimal point for integers.
        // Let's hope this changes sooner rather than later.
        return if(value.rem(1) == 0.0 && value <= Long.MAX_VALUE && value >= Long.MIN_VALUE) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }
    override fun toString() = "APLDouble(${formatted()})"
}
