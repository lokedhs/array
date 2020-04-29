package array.repl

import array.APLValue
import array.Engine
import array.RuntimeContext
import array.makeKeyboardInput

fun runRepl() {
    val keyboardInput = makeKeyboardInput()
    val engine = Engine()
    val prompt = "> "
    val context = RuntimeContext(engine, null)
    while (true) {
        val line = keyboardInput.readString(prompt) ?: break
        val stringTrimmed = line.trim()
        if (stringTrimmed != "") {
            val parsed = engine.parseString(line)
            val result = parsed.evalWithContext(context)
            println(result.formatted(APLValue.FormatStyle.PRETTY))
        }
    }
}
