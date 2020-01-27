package array

abstract class APLNumber : APLSingleValue() {
    override fun formatted() = asDouble().toString()
    override fun toString() = "APLLong(${formatted()})"
}

class APLLong(val value: Long) : APLNumber() {
    override fun asDouble() = value.toDouble()
}

class APLDouble(val value: Double) : APLNumber() {
    override fun asDouble() = value
}
