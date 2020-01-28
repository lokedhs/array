package array

fun main() {
    val engine = Engine()
    val instr = engine.parseString("print (6+1) 5 ⍴ 1 2 3 4 5")
    val result = instr.evalWithContext(engine.makeRuntimeContext())
    println("result = ${result.formatted()}")
}
