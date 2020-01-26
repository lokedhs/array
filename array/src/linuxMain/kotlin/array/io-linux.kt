package array

actual class StringCharacterProvider actual constructor(val s: String) : CharacterProvider {
    private var pos = 0

    override fun nextCodepoint() = if(pos >= s.length) null else s[pos++].toInt()

    override fun revertLastChars(n: Int) {
        if(n > pos) {
            throw IndexOutOfBoundsException("Attempt to move before beginning of string. n=$n, pos=$pos")
        }
        pos -= n
    }
}
