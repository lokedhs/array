package array

import org.junit.Test
import kotlin.test.assertSame

class SQLTest : APLTest(){
    @Test
    fun connectTest() {
        parseAPLExpression("sql:connect \"jdbc:sqlite::memory:\"").let { result ->
            assertSame(APLValueType.INTERNAL, result.aplValueType)
        }
    }

    @Test
    fun simpleQuery() {
        val result = parseAPLExpression(
            """
            |c ← sql:connect "jdbc:sqlite::memory:"
            |c sql:update "create table foo (a int primary key, b varchar(10))"
            |c sql:update "insert into foo values (1,'foo')"
            |c sql:update "insert into foo values (2,'testing')"
            |c sql:update "insert into foo values (3,'xx')"
            |c sql:query "select * from foo order by a"
            """.trimMargin())
        assertDimension(dimensionsOfSize(3,2), result)
        assertSimpleNumber(1, result.valueAt(0))
        assertString("foo", result.valueAt(1))
        assertSimpleNumber(2, result.valueAt(2))
        assertString("testing", result.valueAt(3))
        assertSimpleNumber(3, result.valueAt(4))
        assertString("xx", result.valueAt(5))
    }
}
