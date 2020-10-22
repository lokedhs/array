package array

import array.msofficereader.LoadExcelFileFunction

actual fun platformInit(engine: Engine) {
    engine.registerFunction(engine.internSymbol("loadExcelFile"), LoadExcelFileFunction())

    engine.addModule(JsonAPLModule())
}
