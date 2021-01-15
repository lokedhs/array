package array.sql

import array.*
import java.sql.Connection
import java.sql.DriverManager

class SQLModule : KapModule {
    override val name get() = "sql"

    override fun init(engine: Engine) {
        val ns = engine.makeNamespace("sql")
        engine.registerFunction(ns.internAndExport("connect"), SQLConnectFunction())
        engine.registerFunction(ns.internAndExport("query"), SQLQueryFunction())
        engine.registerFunction(ns.internAndExport("update"), SQLUpdateFunction())
    }
}

class SQLAPLException(message: String, pos: Position? = null) : APLEvalException(message, pos)

class SQLConnectionValue(val conn: Connection, val description: String) : APLSingleValue() {
    override val aplValueType get() = APLValueType.INTERNAL

    override fun formatted(style: FormatStyle) = "Connection(${description})"

    override fun compareEquals(reference: APLValue): Boolean = reference is SQLConnectionValue && reference.conn == conn

    override fun makeKey(): APLValue.APLValueKey = APLValue.APLValueKeyImpl(this, conn)
}

fun APLValue.ensureSQLConnectionValue(pos: Position? = null): SQLConnectionValue {
    if (this !is SQLConnectionValue) {
        throw APLIllegalArgumentException("Value is not a valid SQL connection", pos)
    }
    return this
}

class SQLConnectFunction : APLFunctionDescriptor {
    class SQLConnectFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val connectionUrl = a.toStringValue(pos)
            val conn = DriverManager.getConnection(connectionUrl)
            return SQLConnectionValue(conn, "url=${connectionUrl}")
        }
    }

    override fun make(pos: Position) = SQLConnectFunctionImpl(pos)
}

class SQLQueryFunction : APLFunctionDescriptor {
    class SQLQueryFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val query = b.toStringValue(pos)
            a.ensureSQLConnectionValue(pos).conn.createStatement().use { statement ->
                statement.executeQuery(query).use { result ->
                    val metaData = result.metaData
                    val colCount = metaData.columnCount
                    val resultData = ArrayList<APLValue>()
                    while (result.next()) {
                        repeat(colCount) { colIndex ->
                            resultData.add(parseEntry(result.getObject(colIndex + 1), colIndex, pos))
                        }
                    }
                    return APLArrayImpl(dimensionsOfSize(resultData.size / colCount, colCount), resultData.toTypedArray())
                }
            }
        }

        private fun parseEntry(value: Any, colIndex: Int, pos: Position): APLValue {
            return when (value) {
                is Byte -> value.toLong().makeAPLNumber()
                is Short -> value.toLong().makeAPLNumber()
                is Int -> value.toLong().makeAPLNumber()
                is Long -> value.makeAPLNumber()
                is Char -> APLChar(value.toInt())
                is String -> APLString(value)
                else -> throw SQLAPLException("Cannot convert value ${value} to an APL Value (column ${colIndex + 1} in result", pos)
            }
        }
    }

    override fun make(pos: Position) = SQLQueryFunctionImpl(pos)
}

class SQLUpdateFunction : APLFunctionDescriptor {
    class SQLUpdateFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val query = b.toStringValue(pos)
            a.ensureSQLConnectionValue(pos).conn.createStatement().use { statement ->
                val result = statement.executeUpdate(query)
                return result.makeAPLNumber()
            }
        }
    }

    override fun make(pos: Position) = SQLUpdateFunctionImpl(pos)
}
