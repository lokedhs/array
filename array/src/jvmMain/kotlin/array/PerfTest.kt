package array

import kotlin.system.measureTimeMillis

fun main() {
    val engine = Engine()
    engine.addLibrarySearchPath("standard-lib")
    engine.parseAndEval(StringSourceLocation("use(\"standard-lib.kap\")"), true)
    val srcString = """
            |∇ range (low;high;v) { low+((⍳v)×(high-low))÷v }
            |∇ m (x) { n←0 ◊ {n←n+1 ◊ x+⍵×⍵}⍣{(2<|⍺) ∨ n≥50} 0 ◊ n }
            |m¨(0J1×range(-2;2;1000)) ∘.+ range(-2;2;1000)
            """.trimMargin()
    val elapsed = measureTimeMillis {
        val result = engine.parseAndEval(StringSourceLocation(srcString), true)
        result.collapse()
    }
    println("Elapsed: ${elapsed / 1000.0}")
}
