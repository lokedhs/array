package array.gui.styledarea

import array.gui.ClientRenderContext
import org.fxmisc.richtext.TextExt

open class TextStyle(val type: Type = Type.DEFAULT) {
    fun styleContent(content: TextExt, renderContext: ClientRenderContext) {
        content.font = renderContext.font()
        val css = when (type) {
            Type.ERROR -> "-fx-fill: #ff0000;"
            else -> null
        }
        if (css != null) {
            content.style = css
        }
    }

    enum class Type {
        DEFAULT,
        PROMPT,
        INPUT,
        LOG_INPUT,
        OUTPUT,
        RESULT,
        ERROR
    }
}
