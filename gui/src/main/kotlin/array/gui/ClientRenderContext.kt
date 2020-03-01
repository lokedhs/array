package array.gui

import array.APLValue
import array.Engine
import javafx.scene.text.Font

interface ClientRenderContext {
    fun engine(): Engine
    fun font(): Font
    fun valueClickCallback(value: APLValue)
    fun extendedInput(): ExtendedCharsKeyboardInput
}
