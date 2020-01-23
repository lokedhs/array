package array

fun main() {
    val engine = Engine()
    val result = engine.parseString("10 20 foo")
    println("result = ${result}")
}

private fun printArray(message: String, a: APLArray) {
    println("${message}:")
    for (i in 0 until a.dimensions[0]) {
        println("a[${i}] = ${a.valueAt(i).formatted()}")
    }
}
