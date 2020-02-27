package array

import java.io.BufferedReader
import java.io.FileReader
import java.io.InputStreamReader
import java.io.Reader

actual class StringCharacterProvider actual constructor(private val s: String) : CharacterProvider {

    private val pushBackList = ArrayList<Int>()
    private var pos = 0

    override fun nextCodepoint(): Int? {
        return if (pos >= s.length) {
            null
        } else {
            val result = s.codePointAt(pos)
            pos = s.offsetByCodePoints(pos, 1)
            result
        }
    }

    override fun close() {}
}

class KeyboardInputJvm : KeyboardInput {
    private val reader = BufferedReader(InputStreamReader(System.`in`))

    override fun readString(prompt: String): String? {
        print(prompt)
        return reader.readLine()
    }
}

actual fun makeKeyboardInput(): KeyboardInput {
    return KeyboardInputJvm()
}

class InvalidCharacter : Exception()

class ReaderCharacterProvider(private val reader: Reader) : CharacterProvider {
    private var endOfFile = false

    override fun nextCodepoint(): Int? {
        if(endOfFile) {
            return null
        }

        val v = reader.read()
        return when {
            v == -1 -> {
                endOfFile = true
                null
            }
            Character.isHighSurrogate(v.toChar()) -> {
                val v2 = reader.read()
                if (!Character.isLowSurrogate(v2.toChar())) {
                    throw InvalidCharacter()
                }
                Character.toCodePoint(v.toChar(), v2.toChar())
            }
            else -> v
        }
    }

    override fun close() {
        reader.close()
    }

}

actual fun readFile(name: String): CharacterProvider {
    return ReaderCharacterProvider(BufferedReader(FileReader(name, Charsets.UTF_8)))
}
