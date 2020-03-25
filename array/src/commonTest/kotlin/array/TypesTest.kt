package array

import kotlin.test.Test
import kotlin.test.assertSame

class TypesTest : APLTest() {
    @Test
    fun testInteger() {
        testResultType("typeof 10", APLValueType.INTEGER)
        testResultType("typeof ¯10", APLValueType.INTEGER)
        testResultType("typeof 100", APLValueType.INTEGER)
        testResultType("typeof 0", APLValueType.INTEGER)
    }

    @Test
    fun testDouble() {
        testResultType("typeof 1.2", APLValueType.FLOAT)
        testResultType("typeof 10.", APLValueType.FLOAT)
        testResultType("typeof 1.0", APLValueType.FLOAT)
        testResultType("typeof 0.0", APLValueType.FLOAT)
        testResultType("typeof 100.0", APLValueType.FLOAT)
        testResultType("typeof ¯1.0", APLValueType.FLOAT)
        testResultType("typeof ¯1.2", APLValueType.FLOAT)
        testResultType("typeof ¯1.", APLValueType.FLOAT)
        testResultType("typeof 1+1.2", APLValueType.FLOAT)
    }

    @Test
    fun testComplex() {
        testResultType("typeof 1J2", APLValueType.COMPLEX)
        testResultType("typeof 1.2J2.4", APLValueType.COMPLEX)
        testResultType("typeof 100+1J2", APLValueType.COMPLEX)
    }

    @Test
    fun testChar() {
        testResultType("typeof 0 ⌷ \"foo\"", APLValueType.CHAR)
        testResultType("typeof 0 0 ⌷ 2 2 ⍴ \"foox\"", APLValueType.CHAR)
    }

    @Test
    fun testArray() {
        testResultType("typeof 0⍴1", APLValueType.ARRAY)
        testResultType("typeof 1 2 3 4 5 6", APLValueType.ARRAY)
        testResultType("typeof ⍳100", APLValueType.ARRAY)
        testResultType("typeof (⍳100) × ⍳100", APLValueType.ARRAY)
        testResultType("typeof ⍬", APLValueType.ARRAY)
    }

    @Test
    fun testSymbol() {
        testResultType("typeof 'foo", APLValueType.SYMBOL)
    }

    @Test
    fun testLambdaFunction() {
        testResultType("typeof λ { ⍺+⍵+1 }", APLValueType.LAMBDA_FN)
    }

    private fun testResultType(expression: String, expectedResultSym: APLValueType) {
        val engine = Engine()
        val instr = engine.parseString(expression)
        val result = instr.evalWithContext(RuntimeContext(engine))
        assertSymbol(engine, expectedResultSym.typeName, result)
    }

    private fun assertSymbol(engine: Engine, expected: String, value: APLValue) {
        val v = value.ensureSymbol()
        assertSame(engine.internSymbol(expected), v.value)
    }
}
