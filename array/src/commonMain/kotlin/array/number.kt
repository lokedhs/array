package array

abstract class APLNumber : APLSingleValue() {
    override fun formatted() = asDouble().toString()
    override fun toString() = "APLNumber(${formatted()})"
}

class APLLong(val value: Long) : APLNumber() {
    override fun asDouble() = value.toDouble()
    override fun toString() = "APLLong(${formatted()})"
}

class APLDouble(val value: Double) : APLNumber() {
    override fun asDouble() = value
    override fun toString() = "APLDouble(${formatted()})"
}
