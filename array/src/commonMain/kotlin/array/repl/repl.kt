package array.repl

import array.Engine
import array.FormatStyle
import array.StringSourceLocation
import array.makeKeyboardInput

fun runRepl(args: Array<String>) {
    val keyboardInput = makeKeyboardInput()
    val engine = Engine()
    val prompt = "> "
    while (true) {
        val line = keyboardInput.readString(prompt) ?: break
        val stringTrimmed = line.trim()
        if (stringTrimmed != "") {
            val result = engine.parseAndEval(StringSourceLocation(line), false)
            println(result.formatted(FormatStyle.PRETTY))
        }
    }
}
