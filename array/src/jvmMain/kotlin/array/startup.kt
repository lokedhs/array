package array

import array.rendertext.encloseInBox

fun main() {
    val engine = Engine()
    val instr = engine.parseString("10000 + 1 2 3 4 5")
    val result = instr.evalWithContext(engine.makeRuntimeContext())
    println("result =\n${result.formatted()}")
}
