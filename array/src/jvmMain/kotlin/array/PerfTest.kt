package array

import java.lang.Thread.sleep
import kotlin.system.measureTimeMillis

fun main() {
    val engine = Engine()
    engine.addLibrarySearchPath("standard-lib")
    engine.parseAndEval(StringSourceLocation("use(\"standard-lib.kap\")"), true)
    val srcString = """
                |∇ range (low;high;v) { low+(⍳v)÷(v÷(high-low)) }
                |∇ m (x) { z←r←n←0 ◊ while((r ≤ 2) ∧ (n < 80)) { z ← x+z⋆2 ◊ r ← |z ◊ n←n+1} ◊ n÷80 }
                |m¨(0J1×range(-2;2;1000)) ∘.+ range(-2;2;1000)
            """.trimMargin()
    val elapsed = measureTimeMillis {
        val result = engine.parseAndEval(StringSourceLocation(srcString), true)
        result.collapse()
    }
    println("Elapsed: ${elapsed / 1000.0}")
}
