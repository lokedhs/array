package array

import array.sql.SQLAPLException
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class SQLTest : APLTest() {
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
        assertDimension(dimensionsOfSize(3, 2), result)
        assertSimpleNumber(1, result.valueAt(0))
        assertString("foo", result.valueAt(1))
        assertSimpleNumber(2, result.valueAt(2))
        assertString("testing", result.valueAt(3))
        assertSimpleNumber(3, result.valueAt(4))
        assertString("xx", result.valueAt(5))
    }

    @Test
    fun updatePrepared0() {
        val result = parseAPLExpression(
            """
            |c ← sql:connect "jdbc:sqlite::memory:"
            |c sql:update "create table foo (a int primary key, b varchar(10))"
            |statement ← c sql:prepare "insert into foo values (?, ?)"
            |statement sql:updatePrepared 1 "foo"
            |statement sql:updatePrepared 2 "bar"
            |statement sql:updatePrepared 3 "test message"
            |result ← c sql:query "select * from foo order by a"
            |close statement
            |result
            """.trimMargin())
        assertDimension(dimensionsOfSize(3, 2), result)
        assertSimpleNumber(1, result.valueAt(0))
        assertString("foo", result.valueAt(1))
        assertSimpleNumber(2, result.valueAt(2))
        assertString("bar", result.valueAt(3))
        assertSimpleNumber(3, result.valueAt(4))
        assertString("test message", result.valueAt(5))
    }

    @Test
    fun updatePrepared1() {
        val result = parseAPLExpression(
            """
            |c ← sql:connect "jdbc:sqlite::memory:"
            |c sql:update "create table foo (a int primary key, b varchar(10))"
            |statement ← c sql:prepare "insert into foo values (?, ?)"
            |statement sql:updatePrepared 3 2 ⍴ 1 "foo" 2 "bar" 3 "test message"
            |result ← c sql:query "select * from foo order by a"
            |close statement
            |result
            """.trimMargin())
        assertDimension(dimensionsOfSize(3, 2), result)
        assertSimpleNumber(1, result.valueAt(0))
        assertString("foo", result.valueAt(1))
        assertSimpleNumber(2, result.valueAt(2))
        assertString("bar", result.valueAt(3))
        assertSimpleNumber(3, result.valueAt(4))
        assertString("test message", result.valueAt(5))
    }

    @Test
    fun preparedQuery() {
        val result = parseAPLExpression(
            """
            |c ← sql:connect "jdbc:sqlite::memory:"
            |c sql:update "create table foo (a int primary key, b varchar(10))"
            |c sql:update "insert into foo values (1,'foo')"
            |c sql:update "insert into foo values (2,'testing')"
            |c sql:update "insert into foo values (3,'xx')"
            |c sql:update "insert into foo values (4,'testing2')"
            |c sql:update "insert into foo values (5,'testing-found')"
            |statement ← c sql:prepare "select a, b from foo where a = ?"
            |result ← statement sql:queryPrepared ,5
            |close statement
            |result
            """.trimMargin())
        assertDimension(dimensionsOfSize(1, 2), result)
        assertSimpleNumber(5, result.valueAt(0))
        assertString("testing-found", result.valueAt(1))
    }

    // p ← db sql:prepare "insert into foo values (?,?,?)"
    // p sql:updatePrepared (⍪100+⍳n) , (⍪{"foo",⍕⍵}¨⍳n) , ?n⍴10000

    // db ← sql:connect "jdbc:sqlite:/home/elias/foo.db"
    //Connection(url=jdbc:sqlite:/home/elias/foo.db)
    //n ← 500
    //500
    //p ← db sql:prepare "insert into foo values (?,?,?)"
    //PreparedStatement(insert into foo values (?,?,?)
    // parameters=null)
    //p sql:updatePrepared (⍪80000+⍳n) , (⍪{"foo",⍕⍵}¨⍳n) , ?n⍴10000
    //⍬
    //time:measureTime { p sql:updatePrepared (⍪90000+⍳n) , (⍪{"foo",⍕⍵}¨⍳n) , ?n⍴10000 }
    //Total time: 11.57

    @Test
    fun preparedQueryFailWithIncorrectDimension() {
        assertFailsWith<SQLAPLException> {
            parseAPLExpression(
                """
            |c ← sql:connect "jdbc:sqlite::memory:"
            |c sql:update "create table foo (a int primary key, b varchar(10))"
            |c sql:update "insert into foo values (1,'foo')"
            |c sql:update "insert into foo values (2,'testing')"
            |c sql:update "insert into foo values (3,'xx')"
            |c sql:update "insert into foo values (4,'testing2')"
            |c sql:update "insert into foo values (5,'testing-found')"
            |statement ← c sql:prepare "select a, b from foo where a = ?"
            |result ← statement sql:queryPrepared 5
            |sql:closePreparedStatement statement
            |result
            """.trimMargin())
        }
    }
}
