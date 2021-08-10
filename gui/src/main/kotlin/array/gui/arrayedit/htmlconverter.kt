package array.gui.arrayedit

import array.APLArrayImpl
import array.APLValue
import array.csv.stringToAplValue
import array.dimensionsOfSize
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

fun main() {
    val html = """
        <html>
          <head>
            <title>Some title</title>
          </head>
          <body>
            <table>
              <tr>
                <td>1</td>
                <td>2</td>
              </tr>
              <tr>
                <td>3</td>
                <td>4</td>
              </tr>
            </table>
          </body>
        </html> 
    """.trimIndent()
    val doc = Jsoup.parse(html)
    val tableData = htmlTableToArray(doc)
}

fun parseAsHtmlTable(doc: Document): List<List<String>>? {
    val body = doc.body()
    if (body.childrenSize() != 1) return null
    val tableNode = body.child(0)
    if (tableNode.tagName() != "table") return null
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
    if (content.size == 0) return null
    val numRows = content.size
    val numCols = content[0].size
    if (numCols == 0) return null
    val list = ArrayList<APLValue>()
    content.forEach { rowData ->
        rowData.forEach { value ->
            list.add(stringToAplValue(value))
        }
    }
    return APLArrayImpl(dimensionsOfSize(numRows, numCols), list.toTypedArray())
}
