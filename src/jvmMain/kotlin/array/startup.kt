package array

fun main(args: Array<String>) {
    val engine = Engine()
    val instr = engine.parseString("print 1 ◊ print 3+5 ◊ 1 2 3 4")
    val result = instr.evalWithEngine(engine)
    println("result = ${result.formatted()}")
}
