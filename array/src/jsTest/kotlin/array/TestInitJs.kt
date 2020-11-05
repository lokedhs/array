package array

var jsFilesystem: dynamic = js("require('fs')")

actual fun nativeTestInit() {
//    fun initDirectory(dir: String, base: String) {
//        val files = jsFilesystem.readdirSync("${dir}/${base}") as Array<String>
//        files.forEach { name ->
//            val n = "${base}/${name}"
//            val content = jsFilesystem.readFileSync("${dir}/${n}")
//            registeredFiles[n] = content
//        }
//    }

    fun readFileRecurse(fsDir: String, dir: RegisteredEntry.Directory) {
        val files = jsFilesystem.readdirSync(fsDir) as Array<String>
        files.forEach { name ->
            val newName = "${fsDir}/${name}"
            val result = jsFilesystem.statSync(newName)
            when {
                result.isDirectory() -> readFileRecurse(newName, dir.createDirectory(name, false))
                result.isFile() -> {
                    val content = jsFilesystem.readFileSync(newName)
                    dir.registerFile(name, content)
                }
            }
        }
    }

    fun initDirectory(fsDir: String, base: String) {
        readFileRecurse("${fsDir}/${base}", registeredFilesRoot.createDirectory(base, errorIfExists = false))
    }

    initDirectory("../../../../array", "standard-lib")
    initDirectory("../../../../array", "test-data")
}
