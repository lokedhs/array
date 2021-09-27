package array

import kotlin.system.measureTimeMillis

private fun benchmarkPrimes(): String {
    val srcString = """
            |+/ (⍳N) /⍨ {~0∊⍵|⍨1↓1+⍳⍵⋆0.5}¨ ⍳N←2000000
        """.trimMargin()
    // N←1000
    // Default: 0.548
    // Specialised find result value: 0.072
    // N←4000
    // With previous opt: 4.191
    return srcString
}

private fun benchmarkMultipleCall(): String {
    val srcString = """
            |f ⇐ {⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵+⍵}
            |({f 5}⍣10000000) 0
        """.trimMargin()
    // orig: 3.5761 (with jprofiler)
    // precomputed literals: 3.3647
    return srcString
}

fun main() {
    val engine = Engine()
    engine.addLibrarySearchPath("standard-lib")
    engine.parseAndEval(StringSourceLocation("use(\"standard-lib.kap\")"), true)
    val srcString = benchmarkMultipleCall()
    println("Starting")
    val iterations = 10
    val elapsed = measureTimeMillis {
        repeat(iterations) {
            val result = engine.parseAndEval(StringSourceLocation(srcString), true)
            result.collapse()
        }
    }
    println("Elapsed: ${elapsed / iterations.toDouble() / 1000.0}")
}
