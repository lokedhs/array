package array.repl

import array.Engine
import array.FormatStyle
import array.StringSourceLocation
import array.makeKeyboardInput
import array.options.ArgParser
import array.options.Option

fun runRepl(args: Array<String>) {
    val argResult = ArgParser(Option("lib-path", true)).parse(args)
    val keyboardInput = makeKeyboardInput()
    val engine = Engine()

    if (argResult.containsKey("lib-path")) {
        val path = argResult["lib-path"]!!
        engine.addLibrarySearchPath(path)
    }

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
