package array.builtins

import array.*
import array.csv.readCsv

class PrintAPLFunction : APLFunctionDescriptor {
    class PrintAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            printValue(context, a, FormatStyle.PLAIN)
            return a
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val plainSym = context.engine.internSymbol("plain")
            val prettySym = context.engine.internSymbol("pretty")
            val readSym = context.engine.internSymbol("read")

            val style = when (val styleName = a.ensureSymbol().value) {
                plainSym -> FormatStyle.PLAIN
                prettySym -> FormatStyle.PRETTY
                readSym -> FormatStyle.READABLE
                else -> throw APLIllegalArgumentException("Invalid print style: ${styleName.symbolName}", pos)
            }
            printValue(context, b, style)
            return b
        }

        private fun printValue(context: RuntimeContext, a: APLValue, style: FormatStyle) {
            context.engine.standardOutput.writeString(a.formatted(style))
        }
    }

    override fun make(pos: Position) = PrintAPLFunctionImpl(pos)
}

class ReadCSVFunction : APLFunctionDescriptor {
    class ReadCSVFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val source = openCharFile(arrayAsStringValue(a, pos))
            try {
                return readCsv(source)
            } finally {
                source.close()
            }
        }
    }

    override fun make(pos: Position) = ReadCSVFunctionImpl(pos)
}

class LoadFunction : APLFunctionDescriptor {
    class LoadFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val requestedFile = arrayAsStringValue(a, pos)
            val file = context.engine.resolveLibraryFile(requestedFile) ?: requestedFile
            val engine = context.engine
            engine.withSavedNamespace {
                val instr = engine.parseWithTokenGenerator(TokenGenerator(engine, FileSourceLocation(file)))
                return instr.evalWithContext(context.link())
            }
        }
    }

    override fun make(pos: Position) = LoadFunctionImpl(pos)
}

class HttpRequestFunction : APLFunctionDescriptor {
    class HttpRequestFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val url = arrayAsStringValue(a, pos)
            val result = httpRequest(url)
            return makeAPLString(result.content)
        }
    }

    override fun make(pos: Position) = HttpRequestFunctionImpl(pos)
}
