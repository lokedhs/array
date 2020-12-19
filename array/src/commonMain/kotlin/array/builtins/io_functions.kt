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
            val engine = context.engine
            val plainSym = engine.internSymbol("plain", engine.keywordNamespace)
            val prettySym = engine.internSymbol("pretty", engine.keywordNamespace)
            val readSym = engine.internSymbol("read", engine.keywordNamespace)

            val style = when (val styleName = a.ensureSymbol().value) {
                plainSym -> FormatStyle.PLAIN
                prettySym -> FormatStyle.PRETTY
                readSym -> FormatStyle.READABLE
                else -> throwAPLException(APLIllegalArgumentException("Invalid print style: ${styleName.symbolName}", pos))
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
            val source = openCharFile(a.toStringValue(pos))
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
            val requestedFile = a.toStringValue(pos)
            val file = context.engine.resolveLibraryFile(requestedFile) ?: requestedFile
            val engine = context.engine
            engine.withSavedNamespace {
                return engine.parseAndEval(FileSourceLocation(file), true)
            }
        }
    }

    override fun make(pos: Position) = LoadFunctionImpl(pos)
}

class HttpRequestFunction : APLFunctionDescriptor {
    class HttpRequestFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val url = a.toStringValue(pos)
            val result = httpRequest(url)
            return APLString.make(result.content)
        }
    }

    override fun make(pos: Position) = HttpRequestFunctionImpl(pos)
}

class HttpPostFunction : APLFunctionDescriptor {
    class HttpPostFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val args = a.listify()
            val url = args.listElement(0).toStringValue(pos)
            val data = when (args.listSize()) {
                1 -> APLString.make("")
                2 -> args.listElement(1)
                else -> throwAPLException(APLIllegalArgumentException("Function requires one or two arguments", pos))
            }
            val result = httpPost(url, data.asByteArray())
            return APLString.make(result.content)
        }
    }

    override fun make(pos: Position) = HttpPostFunctionImpl(pos)
}

class ReaddirFunction : APLFunctionDescriptor {
    class ReaddirFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return loadContent(context, a, emptyList())
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            return loadContent(context, b, parseOutputTypes(context, a))
        }

        private fun loadContent(context: RuntimeContext, file: APLValue, selectors: List<OutputType>): APLValue {
            val content = readDirectoryContent(file.toStringValue())
            val numCols = 1 + selectors.size
            val d = dimensionsOfSize(content.size, numCols)
            val valueList = Array(d.contentSize()) { i ->
                val row = i / numCols
                val col = i % numCols
                val pathEntry = content[row]
                if (col == 0) {
                    APLString.make(pathEntry.name)
                } else {
                    when (selectors[col - 1]) {
                        OutputType.SIZE -> pathEntry.size.makeAPLNumber()
                        OutputType.TYPE -> pathEntryTypeToAPL(context, pathEntry.type)
                    }
                }
            }
            return APLArrayImpl(d, valueList)
        }

        private fun pathEntryTypeToAPL(context: RuntimeContext, type: FileNameType): APLValue {
            val sym = when (type) {
                FileNameType.FILE -> context.engine.internSymbol("file", context.engine.keywordNamespace)
                FileNameType.DIRECTORY -> context.engine.internSymbol("directory", context.engine.keywordNamespace)
                FileNameType.UNDEFINED -> context.engine.internSymbol("undefined", context.engine.keywordNamespace)
            }
            return APLSymbol(sym)
        }

        private fun parseOutputTypes(context: RuntimeContext, value: APLValue): List<OutputType> {
            val keywordToType = OutputType.values()
                .map { outputType -> Pair(context.engine.internSymbol(outputType.selector, context.engine.keywordNamespace), outputType) }
                .toMap()

            val result = ArrayList<OutputType>()
            val asArray = value.arrayify()
            if (asArray.dimensions.size != 1) {
                throwAPLException(InvalidDimensionsException("Selector must be a scalar or a rank-1 array", pos))
            }
            asArray.iterateMembers { v ->
                val collapsed = v.collapse()
                if (collapsed !is APLSymbol) {
                    throwAPLException(APLIllegalArgumentException("Selector must be a symbol", pos))
                }
                val found =
                    keywordToType[collapsed.value]
                        ?: throwAPLException(APLIllegalArgumentException("Illegal selector: ${collapsed.value.nameWithNamespace()}", pos))
                result.add(found)
            }
            return result
        }
    }

    override fun make(pos: Position) = ReaddirFunctionImpl(pos)

    private enum class OutputType(val selector: String) {
        SIZE("size"),
        TYPE("type")
    }
}
