package array.builtins

import array.*
import array.csv.readCsv

class ReadFunction : APLFunctionDescriptor {
    class ReadFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val file = a.toStringValue(pos)
            val result = ArrayList<APLValue>()
            try {
                openCharFile(file).use { provider ->
                    provider.lines().forEach { s ->
                        result.add(APLString(s))
                    }
                }
                return APLArrayList(dimensionsOfSize(result.size), result)
            } catch (e: MPFileNotFoundException) {
                throwAPLException(
                    TagCatch(
                        APLSymbol(context.engine.internSymbol("fileNotFound", context.engine.keywordNamespace)),
                        APLString(file)))
            }
        }
    }

    override fun make(pos: Position) = ReadFunctionImpl(pos)
}


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
            val (data, headers) = when (args.listSize()) {
                1 -> Pair(APLString.make(""), emptyMap())
                2 -> Pair(args.listElement(1), emptyMap())
                3 -> Pair(args.listElement(1), ensureHeaderArray(args.listElement(2), pos))
                else -> throwAPLException(
                    APLIllegalArgumentException(
                        "Function requires 1-3 arguments, ${args.listSize()} arguments were passed.",
                        pos))
            }
            val result = httpPost(url, data.asByteArray(pos), headers)
            return APLString.make(result.content)
        }

        private fun ensureHeaderArray(headerArg: APLValue, pos: Position): Map<String, String> {
            if (headerArg.rank != 2 || headerArg.dimensions[1] != 2) {
                throw APLIllegalArgumentException("Headers list should be a rank-2 array with 2 columns")
            }
            val result = HashMap<String, String>()
            for (i in 0 until headerArg.dimensions[0]) {
                val key = headerArg.valueAt(i * 2).toStringValue(pos)
                val value = headerArg.valueAt(i * 2 + 1).toStringValue(pos)
                result[key] = value
            }
            return result
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
            val content = readDirectoryContent(file.toStringValue(pos))
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
