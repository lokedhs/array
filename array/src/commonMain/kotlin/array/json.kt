package array.json

import array.*

class JsonEncodeException(message: String, pos: Position? = null) : APLEvalException(message, pos)

expect fun parseJsonToAPL(input: CharacterProvider): APLValue

fun parseAPLToJson(engine: Engine, input: APLValue, output: CharacterOutput, pos: Position?) {
    val v = input.collapse()

    fun throwJsonEncodingError(): Nothing {
        throwAPLException(JsonEncodeException("Value cannot be encoded to JSON: ${v.formatted(FormatStyle.PLAIN)}", pos))
    }

    fun writeKeyValuePair(key: APLValue, value: APLValue) {
        unless(key.isStringValue()) {
            throwAPLException(JsonEncodeException("Key is not a string: ${key.formatted(FormatStyle.PLAIN)}", pos))
        }
        val escapedKey = jsonEscape(key.toStringValue())
        output.writeString("\"${escapedKey}\":")
        parseAPLToJson(engine, value, output, pos)
    }

    when {
        v is APLMap -> {
            output.writeString("{")
            var first = true
            v.content.forEach { (key, value) ->
                if (first) {
                    first = false
                } else {
                    output.writeString(",")
                }
                writeKeyValuePair(key.value, value)
            }
            output.writeString("}")
        }
        v is APLDouble -> {
            output.writeString(v.value.toString())
        }
        v is APLLong -> {
            output.writeString(v.value.toString())
        }
        v is APLSymbol -> {
            val sym = v.value
            if (sym.namespace !== engine.keywordNamespace) {
                throwJsonEncodingError()
            }
            when (sym.symbolName) {
                "true" -> output.writeString("true")
                "false" -> output.writeString("false")
                "null" -> output.writeString("null")
                else -> throwJsonEncodingError()
            }
        }
        v.isStringValue() -> {
            output.writeString("\"")
            output.writeString(jsonEscape(v.toStringValue(pos)))
            output.writeString("\"")
        }
        v.dimensions.size == 1 -> {
            output.writeString("[")
            v.iterateMembersWithPosition { aplValue, i ->
                if (i > 0) {
                    output.writeString(",")
                }
                parseAPLToJson(engine, aplValue, output, pos)
            }
            output.writeString("]")
        }
        v.dimensions.size == 2 -> {
            if (v.dimensions[1] != 2) {
                throwAPLException(JsonEncodeException("Two-dimensional values must have 2 columns", pos))
            }
            output.writeString("{")
            for (i in 0 until v.dimensions.contentSize() / 2) {
                if (i > 0) {
                    output.writeString(",")
                }
                writeKeyValuePair(v.valueAt(i * 2), v.valueAt(i * 2 + 1))
            }
            output.writeString("}")
        }
        else -> {
            throwJsonEncodingError()
        }
    }
}

private fun jsonEscape(s: String): String {
    val buf = StringBuilder()
    s.forEach { ch ->
        when {
            ch == '\\' -> buf.append("\\\\")
            isPrintable(ch) -> buf.append(ch)
            else -> {
                buf.append("\\u")
                // Ugly, but without a formatting library this is what we get. Kotlin multiplatform really needs a printf implementation.
                val hexString = ch.toInt().toString(16)
                repeat(4 - hexString.length) {
                    buf.append("0")
                }
                buf.append(hexString)
            }
        }
    }
    return buf.toString()
}

private fun isPrintable(ch: Char): Boolean {
    return (ch in 'a'..'z') ||
            (ch in 'A'..'Z') ||
            (ch in 0x20.toChar()..0x3f.toChar())
}
