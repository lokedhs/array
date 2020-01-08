package array

interface APLValue {
    fun add(a: APLValue): APLValue
}

interface APLNumber : APLValue {
    fun asNumber(): Number
}

class APLInteger(val value: Int) : APLNumber {
    override fun asNumber() = value

    override fun add(a: APLValue): APLValue {
        return when(a) {
            is APLInteger -> APLInteger(value + a.value)
            is APLDouble -> a.add(this)
            else -> throw IncompatibleTypeException("Attempt to add an integer to a non-number")
        }
    }
}

class APLDouble(val value: Double) : APLNumber {
    override fun asNumber() = value

    override fun add(a: APLValue): APLValue {
        return when(a) {
            is APLDouble -> APLDouble(value + a.value)
            is APLInteger -> APLDouble(value + a.value)
            else -> throw IncompatibleTypeException("Attempt to add an integer to a non-number")
        }
    }
}

interface APLArray : APLValue {
    fun dimensions(): Array<Int>
    fun rank() = this.dimensions().size
}

class APLArrayImpl : APLArray {
    val dimensions: Array<Int>
    val values: Array<APLValue>

    constructor(dimensions: Array<Int>, init: (Int) -> APLValue) {
        this.dimensions = dimensions
        val n = dimensions.reduce { a, b -> a * b }
        values = Array(n, init)
    }

    override fun dimensions() = dimensions

    override fun add(a: APLValue): APLValue {
        return when(a) {
            is APLArray -> {
                unless(rank() == a.rank()) {
                    throw IncompatibleTypeException("Arrays are of different dimensions")
                }
                ArraySum(this, a)
            }
            is APLNumber -> {
                
            }
            else -> throw IncompatibleTypeException("Can't add an array to an object of different type")
        }
    }

    override fun toString() = Arrays.toString(values)
}

class ArraySum(val a: APLArray, val b: APLArray): APLArray {
    override fun dimensions() = a.dimensions() // Both arrays are of the same dimension

    override fun add(a: APLValue): APLValue {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

fun make_simple_array(vararg elements: APLValue) = APLArrayImpl(arrayOf(elements.size)) { n -> elements.get(n) }

fun iota(n: Int) = APLArrayImpl(arrayOf(n)) { v -> APLInteger(v) }
