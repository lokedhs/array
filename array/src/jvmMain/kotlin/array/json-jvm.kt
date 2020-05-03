package array.json

import array.*
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

actual fun parseJsonToAPL(input: ByteProvider): APLValue {
    val gson = Gson()
    val jsonReader = gson.newJsonReader(InputStreamReader(ByteProviderInputStream(input), StandardCharsets.UTF_8))
    return parseEntry(jsonReader)
}

private fun parseEntry(jsonReader: JsonReader): APLValue {
    return when (jsonReader.peek()) {
        JsonToken.BEGIN_ARRAY -> parseArray(jsonReader)
        JsonToken.BEGIN_OBJECT -> parseObject(jsonReader)
        JsonToken.NUMBER -> parseNumber(jsonReader)
        JsonToken.STRING -> parseString(jsonReader)
        JsonToken.BOOLEAN -> parseBoolean(jsonReader)
        JsonToken.NULL -> parseNull(jsonReader)
        else -> TODO("Not implemented")
    }
}

private fun parseObject(reader: JsonReader): APLMap {
    val content = ArrayList<Pair<Any, APLValue>>()
    reader.beginObject()
    while (true) {
        if (reader.peek() == JsonToken.END_OBJECT) {
            break
        } else {
            val key = reader.nextName()
            val value = parseEntry(reader)
            content.add(APLString(key).makeKey() to value)
        }
    }
    reader.endObject()
    return APLMap(ImmutableMap.makeFromContent(content))
}

private fun parseArray(reader: JsonReader): APLArray {
    val content = ArrayList<APLValue>()
    reader.beginArray()
    while (true) {
        if (reader.peek() == JsonToken.END_ARRAY) {
            break
        } else {
            content.add(parseEntry(reader))
        }
    }
    reader.endArray()
    return APLArrayImpl(dimensionsOfSize(content.size), content.toTypedArray())
}

private fun parseNumber(reader: JsonReader): APLNumber {
    return reader.nextDouble().makeAPLNumber()
}

private fun parseString(reader: JsonReader): APLArray {
    return APLString(reader.nextString())
}

private fun parseBoolean(reader: JsonReader): APLNumber {
    return if (reader.nextBoolean()) 1.makeAPLNumber() else 0.makeAPLNumber()
}

private fun parseNull(reader: JsonReader): APLNullValue {
    reader.nextNull()
    return APLNullValue()
}

fun main() {
    val result = parseJsonToAPL(openFile("array/test-data/json-test.json"))
    if (result is APLMap) {
        result.content.forEach { key, value ->
            println("key: ${key}\nValue:\n${value.formatted(FormatStyle.PRETTY)}\n\n")
        }
    } else {
        println(result.formatted(FormatStyle.PRETTY))
    }
}
