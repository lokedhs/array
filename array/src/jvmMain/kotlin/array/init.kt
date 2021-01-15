package array

import array.msofficereader.LoadExcelFileFunction
import array.sql.SQLModule

actual fun platformInit(engine: Engine) {
    engine.registerFunction(engine.internSymbol("loadExcelFile"), LoadExcelFileFunction())
    engine.addModule(SQLModule())
}
