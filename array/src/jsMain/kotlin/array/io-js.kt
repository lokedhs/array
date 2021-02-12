package array

actual class StringCharacterProvider actual constructor(private val s: String) : CharacterProvider {
    private var pos = 0

    override fun sourceName(): String? = null
    override fun nextCodepoint(): Int? {
        if (pos >= s.length) {
            return null
        }

        val ch = s[pos++]
        return when {
            ch.isHighSurrogate() -> {
                if (pos < s.length) {
                    val low = s[pos++]
                    if (low.isLowSurrogate()) {
                        makeCharFromSurrogatePair(ch, low)
                    } else {
                        throw IllegalStateException("A high surrogate should be followed by a low surrogate")
                    }
                } else {
                    throw IllegalStateException("End of string when low surrogate was expected")
                }
            }
            ch.isLowSurrogate() -> throw IllegalStateException("Unexpected low surrogate")
            else -> ch.toInt()
        }
    }

    override fun close() {}
}

class ByteArrayByteProvider(val content: ByteArray, val name: String? = null) : ByteProvider {
    override fun sourceName() = name

    var pos = 0

    override fun readByte(): Byte? {
        return if (pos >= content.size) null else content[pos++]
    }

    override fun close() {
    }
}

actual fun makeKeyboardInput(): KeyboardInput {
    TODO("Not implemented")
}

sealed class RegisteredEntry(val name: String) {
    class File(name: String, val content: ByteArray) : RegisteredEntry(name)

    class Directory(name: String) : RegisteredEntry(name) {
        val files = HashMap<String, RegisteredEntry>()

        fun find(path: String): RegisteredEntry? {
            val parts = splitName(path)
            return findPathElement(parts, false)
        }

        fun registerFile(path: String, content: ByteArray) {
            val parts = splitName(path)
            val dir = findPathElement(parts.subList(0, parts.size - 1), createDirs = true, lastElementIsDir = true)
            if (dir !is Directory) {
                throw MPFileException("Parent path does not represent a directory")
            }
            val namepart = parts.last()
            dir.files[namepart] = File(namepart, content)
        }

        private fun splitName(path: String) = path.split("/").filter { s -> s.isNotEmpty() }

        private fun findPathElement(parts: List<String>, createDirs: Boolean = false, lastElementIsDir: Boolean = false): RegisteredEntry? {
            if (parts.isEmpty()) {
                return this
            }
            var curr = this
            for (i in 0 until parts.size - 1) {
                val s = parts[i]
                var p = curr.files[s]
                if (p == null) {
                    if (createDirs) {
                        p = Directory(s)
                        curr.files[s] = p
                    } else {
                        return null
                    }
                } else if (p !is Directory) {
                    return null
                }
                curr = p
            }
            val s = parts.last()
            val f = curr.files[s]
            return when {
                f != null -> f
                createDirs && lastElementIsDir -> curr.createDirectory(s)
                else -> null
            }
        }

        fun createDirectory(name: String, errorIfExists: Boolean = true): Directory {
            if (errorIfExists && files.containsKey(name)) {
                throw MPFileException("Directory already exists: ${name}")
            }
            val dir = Directory(name)
            files[name] = dir
            return dir
        }
    }
}

val registeredFilesRoot = RegisteredEntry.Directory("/")

actual fun openFile(name: String): ByteProvider {
    val found = registeredFilesRoot.find(name) ?: throw MPFileNotFoundException("File not found: ${name}")
    if (found !is RegisteredEntry.File) {
        throw MPFileException("Pathname is not a file file: ${name}")
    }
    return ByteArrayByteProvider(found.content, name)
}

actual fun openCharFile(name: String): CharacterProvider {
    return ByteToCharacterProvider(openFile(name))
}

actual fun fileType(path: String): FileNameType? {
    val found = registeredFilesRoot.find(path)
    return when {
        found == null -> null
        found is RegisteredEntry.File -> FileNameType.FILE
        found is RegisteredEntry.Directory -> FileNameType.DIRECTORY
        else -> FileNameType.UNDEFINED
    }
}

actual fun readDirectoryContent(dirName: String): List<PathEntry> {
    val dir = registeredFilesRoot.find(dirName) ?: throw MPFileException("Path not found: ${dirName}")
    if (dir !is RegisteredEntry.Directory) throw MPFileException("Path does not indicate a directory name: ${dirName}")
    val result = ArrayList<PathEntry>()
    dir.files.values.forEach { file ->
        val e = when (file) {
            is RegisteredEntry.File -> PathEntry(file.name, file.content.size.toLong(), FileNameType.FILE)
            is RegisteredEntry.Directory -> PathEntry(file.name, 0, FileNameType.DIRECTORY)
        }
        result.add(e)
    }
    return result
}
