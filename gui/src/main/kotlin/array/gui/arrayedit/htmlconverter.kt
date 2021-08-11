package array.gui.arrayedit

import array.*
import array.csv.stringToAplValue
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.NumberFormat
import java.util.*
import kotlin.collections.ArrayList

fun main() {
    val html = """
        <html>
          <head>
            <title>Some title</title>
          </head>
          <body>
            <div>Some extra text</div>
            <div>
              Some more text here
              <div>
                <table>
                  <tr>
                    <td>1,234</td>
                    <td>2,123,421.112</td>
                  </tr>
                  <tr>
                    <td>3[1]</td>
                    <td>4</td>
                  </tr>
                </table>
              </div>
            </div>
          </body>
        </html> 
    """.trimIndent()
    val doc = Jsoup.parse(html)
    val tableData = htmlTableToArray(doc)
    println("Result:\n${tableData}")
}

fun parseAsHtmlTable(doc: Document): List<List<String>>? {
    val body = doc.body()
    val tableNode = body.selectFirst("table") ?: return null
    val tbodyElements = tableNode.getElementsByTag("tbody")
    if (tbodyElements.size != 1) return null
    val tbody = tbodyElements[0]
    val result = ArrayList<List<String>>()
    tbody.children().forEach { row ->
        val rowElements = ArrayList<String>()
        row.children().forEach { n ->
            rowElements.add(n.text())
        }
        result.add(rowElements)
    }
    return result
}

fun htmlTableToArray(doc: Document): APLValue? {
    val content = parseAsHtmlTable(doc) ?: return null
    if (content.isEmpty()) return null
    val numRows = content.size
    val numCols = content.maxValueBy(List<String>::size)
    if (numCols == 0) return null
    val list = ArrayList<APLValue>()
    val format = NumberFormat.getInstance(Locale.UK)
    content.forEach { rowData ->
        rowData.forEach { value ->
            val parsed = try {
                when (val n = format.parse(value.trim())) {
                    is Short -> n.toLong().makeAPLNumber()
                    is Int -> n.makeAPLNumber()
                    is Long -> n.makeAPLNumber()
                    is Float -> n.toDouble().makeAPLNumber()
                    is Double -> n.makeAPLNumber()
                    else -> null
                }
            } catch(e: java.text.ParseException) {
                null
            }
            list.add(parsed ?: APLString(value))
        }
        repeat(numCols - rowData.size) {
            list.add(APLLONG_0)
        }
    }
    return APLArrayImpl(dimensionsOfSize(numRows, numCols), list.toTypedArray())
}
