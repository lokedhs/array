package array

var jsFilesystem: dynamic = js("require('fs')")

actual fun nativeTestInit() {
    fun initDirectory(dir: String, base: String) {
        val files = jsFilesystem.readdirSync("${dir}/${base}") as Array<String>
        files.forEach { name ->
            val n = "${base}/${name}"
            val content = jsFilesystem.readFileSync("${dir}/${n}")
            registeredFiles[n] = content
        }
    }
    initDirectory("../../../../array", "standard-lib")
    initDirectory("../../../../array", "test-data")
}
