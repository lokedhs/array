package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class MapTest : APLTest() {
    @Test
    fun simpleMap() {
        parseAPLExpression("map 3 2 ⍴ \"foo\" 1 \"bar\" 2 \"a\" 3").let { result ->
            assertTrue(result is APLMap)
            assertEquals(3, result.elementCount())
            assertSimpleNumber(1, result.lookupValue(APLString("foo")))
            assertSimpleNumber(2, result.lookupValue(APLString("bar")))
            assertSimpleNumber(3, result.lookupValue(APLString("a")))
        }
    }

    @Test
    fun oneElementMap() {
        parseAPLExpression("map \"foo\" 1").let { result ->
            assertTrue(result is APLMap)
            assertEquals(1, result.elementCount())
            assertSimpleNumber(1, result.lookupValue(APLString("foo")))
        }
    }

    @Test
    fun missingElementsReturnsNull() {
        parseAPLExpression("map 3 2 ⍴ \"foo\" 1 \"bar\" 2 \"a\" 3").let { result ->
            assertTrue(result is APLMap)
            assertEquals(3, result.elementCount())
            assertSimpleNumber(1, result.lookupValue(APLString("foo")))
            assertSimpleNumber(2, result.lookupValue(APLString("bar")))
            assertSimpleNumber(3, result.lookupValue(APLString("a")))
            assertAPLNull(result.lookupValue(APLString("x")))
        }
    }

    @Test
    fun mixedKeyTypes() {
        parseAPLExpression("map 3 2 ⍴ 1 2 @a 3 (3 2 ⍴ 1 2 3 4 5 6) 4").let { result ->
            assertTrue(result is APLMap)
            assertEquals(3, result.elementCount())
            assertSimpleNumber(2, result.lookupValue(1.makeAPLNumber()))
            assertSimpleNumber(3, result.lookupValue(APLChar('a'.code)))
            assertSimpleNumber(
                4,
                result.lookupValue(
                    APLArrayImpl(
                        dimensionsOfSize(3, 2),
                        arrayOf(1, 2, 3, 4, 5, 6).map { v -> v.makeAPLNumber() }.toTypedArray())))
        }
    }

    @Test
    fun stringValues() {
        parseAPLExpression("map 2 2 ⍴ \"foo\" \"a\" \"bar\" \"b\"").let { result ->
            assertTrue(result is APLMap)
            assertEquals(2, result.elementCount())
            assertString("a", result.lookupValue(APLString("foo")))
            assertString("b", result.lookupValue(APLString("bar")))
        }
    }

    @Test
    fun lookupTest() {
        val result = parseAPLExpression("(map 4 2 ⍴ 1 2 3 4 \"foo\" \"a\" \"bar\" \"b\") mapGet 1")
        assertSimpleNumber(2, result)
    }

    @Test
    fun updateTest() {
        val result = parseAPLExpression(
            """
            |a ← map 2 2 ⍴ "foo" "abc" "bar" "bcd"
            |b ← a mapPut "foo" "update"
            |↑b mapGet ⊂"foo"
        """.trimMargin())
        assertString("update", result)
    }

    @Test
    fun insertTest() {
        val result = parseAPLExpression(
            """
            |a ← map 2 2 ⍴ "foo" "abc" "bar" "bcd"
            |b ← a mapPut "foo2" "insert"
            |↑b mapGet ⊂"foo2"
        """.trimMargin())
        assertString("insert", result)
    }

    @Test
    fun removeTest() {
        val result = parseAPLExpression(
            """
            |a ← map 2 2 ⍴ "foo" "abc" "bar" "bcd"
            |b ← a mapRemove ⊂"foo"
            |↑b mapGet ⊂"foo"
        """.trimMargin())
        assertAPLNull(result)
    }

    @Test
    fun putAndRemove() {
        val result = parseAPLExpression(
            """
            |a ← map 2 2 ⍴ "foo" "abc" "bar" "bcd"
            |b ← a mapPut "foo2" "added"
            |c ← b mapRemove ⊂"foo2"
            |↑c mapGet ⊂"foo2"
        """.trimMargin())
        assertAPLNull(result)
    }

    @Test
    fun putMultiple() {
        val result = parseAPLExpression(
            """
            |a ← map 2 2 ⍴ "foo" "abc" "bar" "bcd"
            |b ← a mapPut 2 2 ⍴ "foo2" "added2" "foo3" "added3"
            |b mapGet "bar" "foo2" "foo3"
        """.trimMargin())
        assertDimension(dimensionsOfSize(3), result)
        assertString("bcd", result.valueAt(0))
        assertString("added2", result.valueAt(1))
        assertString("added3", result.valueAt(2))
    }

    @Test
    fun mapArraySyntaxGetSimple() {
        val result = parseAPLExpression(
            """
            |a ← map 2 2 ⍴ "foo" "abc" "bar" "bcd"
            |a["foo"]
            """.trimMargin())
        assertString("abc", result)
    }

    @Test
    fun mapArraySyntaxNotFound() {
        val result = parseAPLExpression(
            """
            |a ← map 2 2 ⍴ "foo" "abc" "bar" "bcd"
            |a["abcde"]
            """.trimMargin())
        assertAPLNull(result)
    }

    @Test
    fun readKeyValues() {
        parseAPLExpression("a ← map 2 2 ⍴ \"foo\" \"abc\" \"bar\" \"bcd\" ◊ mapToArray a").let { result ->
            fun findKeyAndValue(v: APLValue, findKey: String, findValue: String) {
                for (i in 0 until v.dimensions[0]) {
                    val key = v.valueAt(v.dimensions.indexFromPosition(intArrayOf(i, 0))).toStringValue()
                    val value = v.valueAt(v.dimensions.indexFromPosition(intArrayOf(i, 1))).toStringValue()
                    if (key == findKey) {
                        assertEquals(findValue, value, "Value for key does not match")
                        return
                    }
                }
                fail("Could not find key: '${findKey}'")
            }

            assertDimension(dimensionsOfSize(2, 2), result)
            findKeyAndValue(result, "foo", "abc")
            findKeyAndValue(result, "bar", "bcd")
        }
    }
}
