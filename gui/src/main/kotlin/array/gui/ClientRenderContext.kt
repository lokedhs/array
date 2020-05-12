package array.gui

import array.Engine
import array.RuntimeContext
import javafx.scene.text.Font

interface ClientRenderContext {
    fun engine(): Engine
    fun runtimeContext(): RuntimeContext
    fun font(): Font
    fun extendedInput(): ExtendedCharsKeyboardInput
}
