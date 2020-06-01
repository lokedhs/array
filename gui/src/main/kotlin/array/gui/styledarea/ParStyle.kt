package array.gui.styledarea

/**
 * Holds information about the style of a paragraph.
 */
class ParStyle(val type: ParStyleType = ParStyleType.NORMAL) {
    override fun toString(): String {
        return "ParStyle[type=${type}]"
    }


    enum class ParStyleType {
        NORMAL,
        INDENT,
        OUTPUT
    }
}
