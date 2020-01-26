package array.gui

import javafx.event.EventHandler
import javafx.scene.control.TextField
import javafx.scene.input.KeyEvent
import javafx.scene.text.Font
import java.io.BufferedInputStream
import java.io.FileInputStream

class ExtendedCharsInputField : TextField() {
    private val keymap: Map<KeyDescriptor, String>

    init {
        onKeyTyped = EventHandler { event -> handleKeyTyped(event) }

        keymap = hashMapOf(
            // First row
            KeyDescriptor("1") to "¨",
            KeyDescriptor("2") to "¯",
            KeyDescriptor("-") to "×",
            KeyDescriptor("=") to "÷",
            // Second row
            KeyDescriptor("w") to "⍵", KeyDescriptor("W") to "⍹",
            KeyDescriptor("r") to "⍴",
            KeyDescriptor("y") to "↑",
            KeyDescriptor("u") to "↓",
            KeyDescriptor("i") to "⍳",
            KeyDescriptor("[") to "←",
            // Third row
            KeyDescriptor("a") to "⍺", KeyDescriptor("A") to "⍶",
            KeyDescriptor(";") to "⍎", KeyDescriptor(":") to "≡",
            KeyDescriptor("'") to "⍕", KeyDescriptor("\"") to "≢"
        )
    }

    private fun handleKeyTyped(event: KeyEvent) {
        if (event.isAltDown) {
            keymap[KeyDescriptor(event.character)]?.let { insertText(caretPosition, it) }
            event.consume()
        }
    }

    private data class KeyDescriptor(val character: String)
}
