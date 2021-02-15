package array.repl

import array.*
import array.options.ArgParser
import array.options.InvalidOption
import array.options.Option

fun runRepl(args: Array<String>) {
    val argParser = ArgParser(
        Option("lib-path", true, "Location of the KAP standard library"),
        Option("load", true, "File to load"),
        Option("no-repl", false, "Don't start the REPL"))
    val argResult = try {
        argParser.parse(args)
    } catch (e: InvalidOption) {
        println("Error: ${e.message}")
        argParser.printHelp()
        return
    }
    val keyboardInput = makeKeyboardInput()
    val engine = Engine().apply {
        standardOutput = object : CharacterOutput {
            override fun writeString(s: String) {
                print(s)
            }
        }
    }
    argResult["lib-path"]?.let { libPath ->
        val libPathType = fileType(libPath)
        if (libPathType != null && libPathType === FileNameType.DIRECTORY) {
            engine.addLibrarySearchPath(libPath)
        } else {
            println("Warning: ${libPath} is not a directory, ignoring")
        }
    }
    argResult["load"]?.let { file ->
        val loadFileType = fileType(file)
        when (loadFileType) {
            FileNameType.FILE -> engine.parseAndEval(FileSourceLocation(file), false).collapse()
            FileNameType.DIRECTORY -> println("Warning: ${file} is a directory")
            else -> println("Warning: ${file} is not a valid file")
        }
    }

    if (!argResult.containsKey("no-repl")) {
        val prompt = "> "
        while (true) {
            val line = keyboardInput.readString(prompt) ?: break
            val stringTrimmed = line.trim()
            if (stringTrimmed != "") {
                try {
                    val result = engine.parseAndEval(StringSourceLocation(line), false)
                    println(result.formatted(FormatStyle.PRETTY))
                } catch (e: APLGenericException) {
                    println(e.formattedError())
                }
            }
        }
    }
}
