package array

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import kotlin.system.measureTimeMillis

fun main() {
    val engine = Engine()
    engine.addLibrarySearchPath("standard-lib")
    engine.parseAndEval(StringSourceLocation("use(\"standard-lib.kap\")"), true)
    val srcString = """
            |{⍵+⍺}/⍳200000000
        """.trimMargin()
    println("Starting")
    val elapsed = measureTimeMillis {
        val result = engine.parseAndEval(StringSourceLocation(srcString), true)
        result.collapse()
    }
    println("Elapsed: ${elapsed / 1000.0}")
}

fun xxmain() {
    var m: PersistentMap<Any, String>
    println("Starting")
    val time = measureTimeMillis {
        m = persistentHashMapOf()
        repeat(10000000) { i ->
            m = m.put(i, "This a message in the hashmap: ${i}")
        }
    }
    println("Evaluation time: ${time / 1000.0}")
    println("Map has ${m.size} elements")
    println("Random element: ${m.get(1234)}")
    println("Random element: ${m.get(918211)}")
}

fun xmain() {
    val engine = Engine()
    engine.addLibrarySearchPath("standard-lib")
    engine.parseAndEval(StringSourceLocation("use(\"standard-lib.kap\")"), true)
    val output = StringBuilder()
    engine.standardOutput = object : CharacterOutput {
        override fun writeString(s: String) {
            output.append(s)
        }
    }

    val srcString = """
            |output:render2d 2 2 ⍴ "foo" "a" "test" "abcdefgh"
            """.trimMargin()
    val elapsed = measureTimeMillis {
        val result = engine.parseAndEval(StringSourceLocation(srcString), true)
        result.collapse()
        println("Result:\n${result.formatted(FormatStyle.PRETTY)}")
    }
    println("Output:")
    println(output.toString())
    println("Elapsed: ${elapsed / 1000.0}")
}
