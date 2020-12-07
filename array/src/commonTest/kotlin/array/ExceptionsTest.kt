package array

import array.builtins.TagCatch
import kotlin.test.*

class ExceptionsTest : APLTest() {
    @Test
    fun simpleException() {
        parseAPLExpression("{1→'foo}catch 1 2 ⍴ 'foo λ{2+⍺}").let { result ->
            assertSimpleNumber(3, result)
        }
    }

    @Test
    fun exceptionHandlerTagCheck() {
        parseAPLExpression2("{1→'foo}catch 1 2 ⍴ 'foo λ{⍵}").let { (result, engine) ->
            assertSymbolName(engine, "foo", result)
        }
    }

    @Test
    fun multipleTagHandlers() {
        parseAPLExpression("{1→'foo}catch 4 2 ⍴ 'xyz λ{2+⍺} 'test123 λ{3+⍺} 'bar λ{4+⍺} 'foo λ{5+⍺}").let { result ->
            assertSimpleNumber(6, result)
        }
    }

    @Test
    fun unmatchedTag() {
        assertFailsWith<TagCatch> {
            parseAPLExpression("{1→'foo}catch 1 2 ⍴ 'bar λ{2+⍺}")
        }
    }

    @Test
    fun throwWithoutTagHandler() {
        assertFailsWith<TagCatch> {
            parseAPLExpression("2 + 1→'foo")
        }
    }

    @Test
    fun stackTrace() {
        try {
            parseAPLExpression(
                """
            |∇ foo (x) {
            |  x[1;2]
            |}
            |
            |∇ bar (y) {
            |  foo y
            |}
            |
            |bar 1 2 3 4
            """.trimMargin()
            )
            assertTrue(false, "Exception was expected")
        } catch (ex: APLEvalException) {
            val callStack = ex.callStack
            assertNotNull(callStack)
            assertEquals(2, callStack.size)
        }
    }
}
