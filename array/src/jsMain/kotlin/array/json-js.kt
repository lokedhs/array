package array.json

import array.*

actual val backendSupportsJson = true

actual fun parseJsonToAPL(input: CharacterProvider): APLValue {
    val buf = StringBuilder()
    input.lines().forEach { line ->
        buf.append(line)
        buf.append("\n")
    }
    val json = JSON.parse<Any>(buf.toString())
    return parseEntry(json)
}

private fun parseEntry(value: Any?): APLValue {
    if (value == null) {
        return APLNullValue.APL_NULL_INSTANCE
    }
    return when (value) {
        is Array<*> -> parseArray(value)
        is String -> APLString(value)
        is Int -> value.makeAPLNumber()
        is Long -> value.makeAPLNumber()
        is Double -> value.makeAPLNumber()
        is Boolean -> if (value) APLLONG_1 else APLLONG_0
        else -> parseObject(value.asDynamic())
    }
}

private fun parseArray(value: Array<*>): APLValue {
    val content = ArrayList<APLValue>()
    value.forEach { m ->
        content.add(parseEntry(m))
    }
    return APLArrayImpl(dimensionsOfSize(content.size), content.toTypedArray())
}

private fun parseObject(value: dynamic): APLValue {
    val content = ArrayList<Pair<APLValue.APLValueKey, APLValue>>()
    val keysFn = js("Object.keys")
    val keyArray = keysFn(value)
    keyArray.forEach { k ->
        if (k !is String) {
            throw JsonParseException("Key is not a string: ${k}")
        }
        content.add(APLString(k.unsafeCast<String>()).makeKey() to parseEntry(value[k]))
    }
    return APLMap(ImmutableMap2.makeFromContent(content))
}
