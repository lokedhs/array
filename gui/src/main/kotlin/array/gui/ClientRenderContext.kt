package array.gui

import array.Engine
import javafx.scene.text.Font

interface ClientRenderContext {
    fun engine(): Engine
    fun font(): Font
    fun extendedInput(): ExtendedCharsKeyboardInput
}
