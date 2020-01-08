package array

fun main() {
    val a = iota(5)
    val b = a.add(APLInteger(100))
    println("b = ${b}")
}
