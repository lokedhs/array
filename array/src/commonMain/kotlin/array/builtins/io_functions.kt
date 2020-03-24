package array.builtins

import array.*
import array.csv.readCsv

class PrintAPLFunction : APLFunctionDescriptor {
    class PrintAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            printValue(context, a, APLValue.FormatStyle.PLAIN)
            return a
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val plainSym = context.engine.internSymbol("plain")
            val prettySym = context.engine.internSymbol("pretty")
            val readSym = context.engine.internSymbol("read")

            val styleName = a.ensureSymbol().value
            val style = when {
                styleName == plainSym -> APLValue.FormatStyle.PLAIN
                styleName == prettySym -> APLValue.FormatStyle.PRETTY
                styleName == readSym -> APLValue.FormatStyle.READABLE
                else -> throw APLIllegalArgumentException("Invalid print style: ${styleName.symbolName}")
            }
            printValue(context, b, style)
            return b
        }

        private fun printValue(context: RuntimeContext, a: APLValue, style: APLValue.FormatStyle) {
            context.engine.standardOutput.writeString(a.formatted(style))
        }
    }

    override fun make(pos: Position) = PrintAPLFunctionImpl(pos)
}

class ReadCSVFunction : APLFunctionDescriptor {
    class ReadCSVFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val source = openCharFile(arrayAsStringValue(a))
            try {
                return readCsv(source)
            } finally {
                source.close()
            }
        }
    }

    override fun make(pos: Position) = ReadCSVFunctionImpl(pos)
}
