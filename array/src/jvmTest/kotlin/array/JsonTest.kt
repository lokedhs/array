package array

import array.json.parseAPLToJson
import com.google.gson.Gson
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JsonTest : APLTest() {
    @Test
    fun readJson() {
        parseAPLExpression("json:read \"test-data/json-test.json\"").let { result ->
            assertTrue(result is APLMap)
            assertEquals(7, result.elementCount())
            assertSimpleNumber(1, result.lookupValue(APLString("foo")))
            result.lookupValue(APLString("someArray")).let { inner ->
                assertDimension(dimensionsOfSize(5), inner)
                assertSimpleNumber(1, inner.valueAt(0))
                assertSimpleNumber(2, inner.valueAt(1))
                assertSimpleNumber(3, inner.valueAt(2))
                assertSimpleNumber(4, inner.valueAt(3))
                inner.valueAt(4).let { internalList ->
                    assertDimension(dimensionsOfSize(2), internalList)
                    assertArrayContent(arrayOf(5, 6), internalList)
                }
            }
            assertString("foo test", result.lookupValue(APLString("someString")))
            assertSimpleNumber(1, result.lookupValue(APLString("booleanValue")))
            assertSimpleNumber(0, result.lookupValue(APLString("booleanValue2")))
            result.lookupValue(APLString("recursiveMap")).let { inner ->
                assertTrue(inner is APLMap)
                assertSimpleNumber(1, inner.lookupValue(APLString("a")))
                assertSimpleNumber(2, inner.lookupValue(APLString("b")))
            }
            assertAPLNull(result.lookupValue(APLString("nullValue0")))
        }
    }

    @Test
    fun readJsonFromString() {
        parseAPLExpression("json:readString \"{\\\"a\\\":10,\\\"b\\\":\\\"c\\\"}\"").let { result ->
            assertTrue(result is APLMap)
            assertEquals(2, result.elementCount())
            assertSimpleNumber(10, result.lookupValue(APLString("a")))
            assertString("c", result.lookupValue(APLString("b")))
        }
    }

    @Test
    fun encodeSingleInt() {
        val output = StringBuilderOutput()
        val (result, engine) = parseAPLExpression2("2")
        parseAPLToJson(engine, result, output, null)
        val jsonResult = Gson().fromJson(output.buf.toString(), Int::class.java)
        assertEquals(2, jsonResult)
    }

    @Test
    fun encodeSingleString() {
        val output = StringBuilderOutput()
        val (result, engine) = parseAPLExpression2("\"abc\"")
        parseAPLToJson(engine, result, output, null)
        val jsonResult = Gson().fromJson(output.buf.toString(), String::class.java)
        assertEquals("abc", jsonResult)
    }

    @Test
    fun escapeStringOutput() {
        val output = StringBuilderOutput()
        val (result, engine) = parseAPLExpression2("\"abc\\\"a\"")
        parseAPLToJson(engine, result, output, null)
        val jsonResult = Gson().fromJson(output.buf.toString(), String::class.java)
        assertEquals("abc\"a", jsonResult)
    }

    @Test
    fun encodeSingleDouble() {
        val output = StringBuilderOutput()
        val (result, engine) = parseAPLExpression2("2.1")
        parseAPLToJson(engine, result, output, null)
        val jsonResult = Gson().fromJson(output.buf.toString(), Double::class.java)
        assertTrue(jsonResult >= 2.09 && jsonResult <= 2.11)
    }

    @Test
    fun encodeSingleBoolean() {
        fun testBoolean(name: String, v: Boolean) {
            val output = StringBuilderOutput()
            val (result, engine) = parseAPLExpression2(name)
            parseAPLToJson(engine, result, output, null)
            val jsonResult = Gson().fromJson(output.buf.toString(), Boolean::class.java)
            assertEquals(v, jsonResult)
        }
        testBoolean(":true", true)
        testBoolean(":false", false)
    }

    @Test
    fun encodeSingleNull() {
        val output = StringBuilderOutput()
        val (result, engine) = parseAPLExpression2(":null")
        parseAPLToJson(engine, result, output, null)
        val jsonResult = Gson().fromJson(output.buf.toString(), String::class.java)
        assertNull(jsonResult)
    }

    @Test
    fun encodeNumberArray() {
        val output = StringBuilderOutput()
        val (result, engine) = parseAPLExpression2("1 2 3 4")
        parseAPLToJson(engine, result, output, null)
        val jsonResult = Gson().fromJson(output.buf.toString(), List::class.java)
        assertEquals(listOf(1.0, 2.0, 3.0, 4.0), jsonResult)
    }

    @Test
    fun encodeObjectAsHash() {
        val output = StringBuilderOutput()
        val (result, engine) = parseAPLExpression2(
            """
            |map 3 2 ⍴ "foo" 1 "bar" 12 "xyz" ("test" "content" "asd")
            """.trimMargin())
        parseAPLToJson(engine, result, output, null)
        val x = output.buf.toString()
        println("res: +${x}+")
        val jsonResult = Gson().fromJson(x, Map::class.java)
        assertEquals(3, jsonResult.size)
        jsonResult["foo"].let { entry ->
            assertEquals(1.0, entry)
        }
        jsonResult["bar"].let { entry ->
            assertEquals(12.0, entry)
        }
        jsonResult["xyz"].let { entry ->
            assertEquals(listOf("test", "content", "asd"), entry)
        }
    }
}
