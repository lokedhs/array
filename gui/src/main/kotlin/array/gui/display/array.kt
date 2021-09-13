package array.gui.display

import array.APLValue
import array.AxisLabel
import array.dimensionsOfSize
import array.gui.Client
import javafx.fxml.FXMLLoader
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.layout.*
import javafx.scene.paint.Color
import java.net.URL

fun makeArrayMultiDimension(client: Client, value: APLValue): Node {
    val loader = FXMLLoader(ArrayMultiDimensionController::class.java.getResource("array.fxml"))
    val parent = loader.load<BorderPane>()
    val controller = loader.getController<ArrayMultiDimensionController>()

    val grid = GridPane()
    parent.center = grid
    grid.border = Border(BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths(1.0)))
    val d = value.dimensions
    val labelArray = value.labels?.labels
    updateGrid(client, grid, value, IntArray(d.size - 2) { 0 }, labelArray?.get(d.size - 1), labelArray?.get(d.size - 2))

    return parent
}

fun updateGrid(client: Client, grid: GridPane, value: APLValue, prefixPos: IntArray, colLabels: List<AxisLabel?>?, rowLabels: List<AxisLabel?>?) {
    val dimensions = value.dimensions
    val numRows = dimensions[dimensions.size - 2]
    val numCols = dimensions[dimensions.size - 1]
    repeat(numRows) { rowIndex ->
        repeat(numCols) { colIndex ->
            val v = value.valueAt(dimensions.indexFromPosition(IntArray(dimensions.size) { i ->
                when {
                    i < dimensions.size - 2 -> prefixPos[i]
                    i == dimensions.size - 2 -> rowIndex
                    i == dimensions.size - 1 -> colIndex
                    else -> throw IllegalStateException("Unexpected index")
                }
            }))
            val label = makeArrayNode(client, v)
            GridPane.setRowIndex(label, rowIndex)
            GridPane.setColumnIndex(label, colIndex)
            GridPane.setMargin(label, Insets(3.0, 3.0, 3.0, 3.0))
            GridPane.setHalignment(label, HPos.RIGHT)
            grid.children.add(label)
        }
    }
}

class ArrayMultiDimensionController {
    @JvmField
    var hbox: HBox? = null
}
