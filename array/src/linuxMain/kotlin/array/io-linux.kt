package array

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.errno
import platform.posix.fgets
import platform.posix.stdin
import platform.posix.strerror

actual class StringCharacterProvider actual constructor(val s: String) : CharacterProvider {
    private var pos = 0

    override fun nextCodepoint() = if (pos >= s.length) null else s[pos++].toInt()

    override fun revertLastChars(n: Int) {
        if (n > pos) {
            throw IndexOutOfBoundsException("Attempt to move before beginning of string. n=$n, pos=$pos")
        }
        pos -= n
    }
}

class KeyboardInputNative : KeyboardInput {
    override fun readString(): String {
        memScoped {
            val bufSize = 10240
            val buf = allocArray<ByteVar>(bufSize)
            val ret = fgets(buf, bufSize, stdin)
            if (ret == null) {
                val msg = strerror(errno)?.toKString() ?: "Null error message"
                throw RuntimeException("Error reading from stdin: $msg")
            }
            ret.toKString().forEachIndexed { i, ch -> println("$i = $ch") }
            return ret.toKString()
        }
    }
}

actual fun makeKeyboardInput(): KeyboardInput {
    return KeyboardInputNative()
}
