package array

fun main() {
    val a = iota(5)
    printArray("a", a)

    val int100 = APLInteger(100)
    val b = a.add(int100)
    printArray("b", b as APLArray)

    println("int100 = ${int100.formatted()}")

//    val c = int100.add(a)
//    printArray("c", c as APLArray)

    val engine = Engine()
    val result = engine.parseString("10 20 foo")
    println("result = ${result}")
}

private fun printArray(message: String, a: APLArray) {
    println("${message}:")
    for (i in 0 until a.dimensions()[0]) {
        println("a[${i}] = ${a.valueAt(i).formatted()}")
    }
}
