package array

import array.builtins.TagCatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.fail

class IOAPLTest : APLTest() {
    @Test
    fun plainReaddir() {
        parseAPLExpression("x ← io:readdir \"test-data/readdir-test/\" ◊ x[⍋x;]").let { result ->
            assertDimension(dimensionsOfSize(2, 1), result)
            assertString("a.txt", result.valueAt(0))
            assertString("file2.txt", result.valueAt(1))
        }
    }

    @Test
    fun readdirWithSize() {
        parseAPLExpression("x ← :size io:readdir \"test-data/readdir-test/\" ◊ x[⍋x;]").let { result ->
            assertDimension(dimensionsOfSize(2, 2), result)
            assertString("a.txt", result.valueAt(0))
            assertSimpleNumber(12, result.valueAt(1))
            assertString("file2.txt", result.valueAt(2))
            assertSimpleNumber(27, result.valueAt(3))
        }
    }

    @Test
    fun readCharacterFile() {
        parseAPLExpression("io:read \"test-data/multi.txt\"").let { result ->
            val expected = listOf("foo", "bar", "test", "abcdef", "testtest", "  testline", "", "aa", "ab", "ac", "ad")
            assertEquals(1, result.dimensions.size)
            assertEquals(expected.size, result.dimensions[0])
            for (i in expected.indices) {
                assertEquals(expected[i], result.valueAt(i).toStringValue())
            }
        }
    }

    @Test
    fun readMissingFile() {
        val engine = Engine()
        try {
            engine.parseAndEval(
                StringSourceLocation("io:read \"test-data/this-file-should-not-be-found-as-well\""),
                true
            ).collapse()
            fail("Read should not succeed")
        } catch (e: TagCatch) {
            val tag = e.tag.ensureSymbol().value
            assertSame(engine.internSymbol("fileNotFound", engine.keywordNamespace), tag)
        }
    }

    @Test
    fun closeObjectTest() {
        val (result, out) = parseWithClosableValue(
            """
            |x ← makeClosable 0
            |io:print beforeValue ← closedFlag x
            |close x
            |io:print afterValue ← closedFlag x
            |beforeValue afterValue
            """.trimMargin())
        assertArrayContent(arrayOf(0, 1), result)
        assertEquals("01", out)
    }

    @Test
    fun automaticClose() {
        val (result, out) = parseWithClosableValue(
            """
            |leaveclose ⇐ close atLeave
            |x ← 0
            |{
            |  y ← leaveclose makeClosable 0
            |  io:print closedFlag y
            |  x ← y
            |} 0
            |io:print closedFlag x
            |0
            """.trimMargin())
        assertSimpleNumber(0, result)
        assertEquals("01", out)
    }

    fun parseWithClosableValue(expr: String): Pair<APLValue, String> {
        val engine = Engine()
        val out = StringBuilderOutput()
        engine.registerFunction(engine.internSymbol("closedFlag"), ClosedFlagFunction())
        engine.registerFunction(engine.internSymbol("makeClosable"), MakeClosable())
        engine.standardOutput = out
        engine.registerClosableHandler(ClosableTestValueHandler)
        val result = engine.parseAndEval(StringSourceLocation(expr), true).collapse()
        return Pair(result, out.buf.toString())
    }

    object ClosableTestValueHandler : ClosableHandler<ClosableTestValue> {
        override fun close(value: ClosableTestValue) {
            if (value.closed) {
                throw IllegalStateException("Value is already closed")
            }
            value.closed = true
        }
    }

    class ClosableTestValue : APLSingleValue() {
        var closed = false

        override val aplValueType get() = APLValueType.INTERNAL
        override fun formatted(style: FormatStyle) = "TestValue(${if (closed) "closed" else "open"}"
        override fun compareEquals(reference: APLValue) = this === reference
        override fun makeKey() = APLValueKeyImpl(this, this)
    }

    class ClosedFlagFunction : APLFunctionDescriptor {
        class ClosedFlagFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
            override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
                val a0 = a.unwrapDeferredValue()
                if (a0 !is ClosableTestValue) {
                    throwAPLException(APLIllegalArgumentException("Unexpected type", pos))
                }
                return if (a0.closed) APLLONG_1 else APLLONG_0
            }
        }

        override fun make(pos: Position) = ClosedFlagFunctionImpl(pos)
    }

    class MakeClosable : APLFunctionDescriptor {
        class MakeClosableImpl(pos: Position) : NoAxisAPLFunction(pos) {
            override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
                return ClosableTestValue()
            }
        }

        override fun make(pos: Position) = MakeClosableImpl(pos)
    }
}
