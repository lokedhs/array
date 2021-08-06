package array.gui.arrayedit

import array.APLValue

class ArrayEditorRow(value: APLValue, rowIndexOffset: Int, numCols: Int) {
    val values = Array(numCols) { i -> value.valueAt(rowIndexOffset + i) }
}
