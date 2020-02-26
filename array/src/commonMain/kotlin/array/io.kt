package array

interface CharacterProvider {
    fun nextCodepoint(): Int?
    fun revertLastChars(n: Int)
}

expect class StringCharacterProvider(s: String) : CharacterProvider

interface KeyboardInput {
    fun readString(prompt: String): String?
}

expect fun makeKeyboardInput(): KeyboardInput
