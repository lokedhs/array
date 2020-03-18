package array.gui.styledarea

open class TextStyle(val type: Type = Type.DEFAULT) {
    enum class Type {
        DEFAULT,
        PROMPT,
        INPUT
    }
}
