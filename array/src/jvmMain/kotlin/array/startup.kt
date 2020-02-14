package array

fun main() {
    val engine = Engine()
    val instr = engine.parseString("1 2 3 4 ,[0] 10 11 12 13 14 15 16")
    val result = instr.evalWithContext(engine.makeRuntimeContext())
    println("result =\n${result.formatted()}")
}
