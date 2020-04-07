package array

class StringBuilderOutput : CharacterOutput {
    val buf = StringBuilder()

    override fun writeString(s: String) {
        buf.append(s)
    }
}
