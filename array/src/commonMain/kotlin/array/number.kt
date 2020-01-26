package array

abstract class APLNumber : APLSingleValue() {
    override fun formatted() = asNumber().toString()
    override fun toString() = "APLLong(${formatted()})"
}

class APLLong(val value: Long) : APLNumber() {
    override fun asNumber() = value

    override fun add(a: APLValue): APLValue {
        return when (a) {
            is APLLong -> APLLong(value + a.value)
            is APLDouble -> a.add(this)
            is APLArray -> a.add(this)
            else -> throw IncompatibleTypeException("Attempt to add an integer to a non-number")
        }
    }
}

class APLDouble(val value: Double) : APLNumber() {
    override fun asNumber() = value

    override fun add(a: APLValue): APLValue {
        return when (a) {
            is APLDouble -> APLDouble(value + a.value)
            is APLLong -> APLDouble(value + a.value)
            else -> throw IncompatibleTypeException("Attempt to add an integer to a non-number")
        }
    }
}
