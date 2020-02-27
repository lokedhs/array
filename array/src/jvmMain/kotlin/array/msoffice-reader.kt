package array.msofficereader

import array.*
import org.apache.poi.ss.usermodel.*
import java.io.File

fun readExcelFile(name: String): APLValue {
    val workbook = WorkbookFactory.create(File(name))
    val evaluator = workbook.creationHelper.createFormulaEvaluator()
    val sheet = workbook.getSheetAt(0)
    if (sheet.physicalNumberOfRows == 0) {
        return APLNullValue()
    }

    val lastRowIndex = sheet.lastRowNum
    val rows = ArrayList<List<APLValue>>()
    for (i in 0..lastRowIndex) {
        val row = readRow(sheet.getRow(i), evaluator)
        rows.add(row)
    }

    val width = rows.maxValueBy { it.size }
    val value = APLArrayImpl(intArrayOf(rows.size, width)) { i ->
        val rowIndex = i / width
        val colIndex = i % width
        val row = rows[rowIndex]
        if (colIndex < row.size) {
            row[colIndex]
        } else {
            APLNullValue()
        }
    }
    return value
}

fun readRow(row: Row, evaluator: FormulaEvaluator): List<APLValue> {
    val cellList = ArrayList<APLValue>()
    val lastCellIndex = row.lastCellNum
    for (i in 0..lastCellIndex) {
        val cell = row.getCell(i)
        val value = if (cell == null) APLNullValue() else cellToAPLValue(cell, evaluator)
        cellList.add(value)
    }
    return cellList
}

fun cellToAPLValue(cell: Cell, evaluator: FormulaEvaluator): APLValue {
    return when (cell.cellType) {
        CellType.FORMULA -> parseEvaluatedCell(cell, evaluator)
        CellType.BOOLEAN -> if (cell.booleanCellValue) APLLong(1) else APLLong(0)
        CellType.BLANK -> APLLong(0)
        CellType.NUMERIC -> APLDouble(cell.numericCellValue)
        CellType.STRING -> makeAPLString(cell.stringCellValue)
        else -> throw IllegalStateException("Unknown cell type: ${cell.cellType}")
    }
}

fun parseEvaluatedCell(cell: Cell, evaluator: FormulaEvaluator): APLValue {
    val v = evaluator.evaluate(cell)
    return when (cell.cellType) {
        CellType.FORMULA -> throw IllegalStateException("The result of an evaluation should not be a formula")
        CellType.BOOLEAN -> if (v.booleanValue) APLLong(1) else APLLong(0)
        CellType.BLANK -> APLLong(0)
        CellType.NUMERIC -> APLDouble(v.numberValue)
        CellType.STRING -> makeAPLString(v.stringValue)
        else -> throw IllegalStateException("Unknown cell type: ${v.cellType}")
    }
}

class LoadExcelFileFunction : NoAxisAPLFunction() {
    override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
        return readExcelFile("/tmp/foo.xlsx")
    }

    override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
        TODO("not implemented")
    }
}
