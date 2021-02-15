package array

import array.json.backendSupportsJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonTest : APLTest() {
    @Test
    fun readJson() {
        unless(backendSupportsJson) { return }

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
        unless(backendSupportsJson) { return }

        parseAPLExpression("json:readString \"{\\\"a\\\":10,\\\"b\\\":\\\"c\\\"}\"").let { result ->
            assertTrue(result is APLMap)
            assertEquals(2, result.elementCount())
            assertSimpleNumber(10, result.lookupValue(APLString("a")))
            assertString("c", result.lookupValue(APLString("b")))
        }
    }

}
