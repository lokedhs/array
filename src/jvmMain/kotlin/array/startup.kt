package array

fun main(args: Array<String>) {
    val engine = Engine()
    val instr = engine.parseString("print 1000 + 1 2 3 4 + 10 11 12 13")
    val result = instr.evalWithEngine(engine)
    println("result = ${result.formatted()}")
}
