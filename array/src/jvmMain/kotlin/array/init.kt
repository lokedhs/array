package array

import array.msofficereader.MsOfficeModule
import array.sql.SQLModule

actual fun platformInit(engine: Engine) {
    engine.addModule(MsOfficeModule())
    engine.addModule(SQLModule())
}
