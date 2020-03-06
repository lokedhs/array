package array

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.fgets
import platform.posix.stdin

actual class StringCharacterProvider actual constructor(val s: String) : CharacterProvider {
    private var pos = 0

    override fun nextCodepoint() = if (pos >= s.length) null else s[pos++].toInt()
    override fun close() {}
}

class KeyboardInputNative : KeyboardInput {
    override fun readString(prompt: String): String? {
        print(prompt)
        memScoped {
            val bufSize = 10240
            val buf = allocArray<ByteVar>(bufSize)
            val ret = fgets(buf, bufSize, stdin)
            return if (ret != null) {
                ret.toKString()
            } else {
                null
            }
        }
    }
}

//class KeyboardInputLibedit : KeyboardInput {
//    override fun readString(prompt: String): String? {
//        val result = libedit.readline(prompt)
//        return if (result == null) {
//            null
//        } else {
//            val resultConverted = result.toKString()
//            free(result.rawValue)
//            resultConverted
//        }
//    }
//}

actual fun makeKeyboardInput(): KeyboardInput {
    return KeyboardInputNative()
}

actual fun readFile(name: String): CharacterProvider {
    //return FileCharacterProvider(name)
    TODO("File reading not implemented in native mode yet")
}
