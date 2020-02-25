package array.gui

import array.APLValue
import javafx.scene.text.Font

interface ClientRenderContext {
    fun font(): Font
    fun valueClickCallback(value: APLValue)
}
