package array

import array.json.parseAPLToJson
import com.google.gson.Gson
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JsonTestJVM : APLTest() {
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
        val jsonResult = Gson().fromJson(output.buf.toString(), Map::class.java)
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
