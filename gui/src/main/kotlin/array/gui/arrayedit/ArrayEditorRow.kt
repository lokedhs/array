package array.gui.arrayedit

import array.APLValue

class ArrayEditorRow(value: APLValue, rowIndex: Int, numCols: Int) {
    val values = Array(numCols) { i -> value.valueAt(rowIndex * numCols + i) }
}
