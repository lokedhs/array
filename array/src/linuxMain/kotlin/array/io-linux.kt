package array

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap.free
import kotlinx.cinterop.toKString
import platform.posix.fgets
import platform.posix.stdin

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
    override fun readString(prompt: String): String? {
        print(prompt)
        memScoped {
            val bufSize = 10240
            val buf = allocArray<ByteVar>(bufSize)
            val ret = fgets(buf, bufSize, stdin)
            return if (ret != null) {
                ret.toKString().forEachIndexed { i, ch -> println("$i = $ch") }
                ret.toKString()
            } else {
                null
            }
        }
    }
}

class KeyboardInputLibedit : KeyboardInput {
    override fun readString(prompt: String): String? {
        val result = libedit.readline(prompt)
        return if(result == null) {
            null
        } else {
            val resultConverted = result.toKString()
            free(result.rawValue)
            resultConverted
        }
    }
}

actual fun makeKeyboardInput(): KeyboardInput {
//    return KeyboardInputNative()
    return KeyboardInputLibedit()
}
