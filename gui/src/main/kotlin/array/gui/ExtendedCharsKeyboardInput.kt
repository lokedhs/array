package array.gui

import javafx.event.EventHandler
import javafx.scene.control.TextInputControl
import javafx.scene.input.KeyEvent

class ExtendedCharsKeyboardInput {
    private val keymap: Map<KeyDescriptor, String>

    init {
        keymap = hashMapOf(
            // First row
            KeyDescriptor("`") to "◊",
            KeyDescriptor("1") to "¨", KeyDescriptor("!") to "⌶",
            KeyDescriptor("2") to "¯", KeyDescriptor("@") to "⍫",
            KeyDescriptor("3") to "<", KeyDescriptor("#") to "⍒",
            KeyDescriptor("4") to "≤", KeyDescriptor("$") to "⍋",
            KeyDescriptor("5") to "=", KeyDescriptor("%") to "⌽",
            KeyDescriptor("6") to "≥", KeyDescriptor("^") to "⍉",
            KeyDescriptor("7") to ">", KeyDescriptor("&") to "⊖",
            KeyDescriptor("8") to "≠", KeyDescriptor("*") to "⍟",
            KeyDescriptor("9") to "∨", KeyDescriptor("(") to "⍱",
            KeyDescriptor("0") to "∧", KeyDescriptor(")") to "⍲",
            KeyDescriptor("-") to "×", KeyDescriptor("_") to "!",
            KeyDescriptor("=") to "÷", KeyDescriptor("+") to "⌹",
            // Second row
            // q is unassigned
            KeyDescriptor("w") to "⍵", KeyDescriptor("W") to "⍹",
            KeyDescriptor("e") to "∊", KeyDescriptor("E") to "⍷",
            KeyDescriptor("r") to "⍴",
            KeyDescriptor("t") to "∼", KeyDescriptor("T") to "⍨",
            KeyDescriptor("y") to "↑", KeyDescriptor("Y") to "¥",
            KeyDescriptor("u") to "↓",
            KeyDescriptor("i") to "⍳", KeyDescriptor("I") to "⍸",
            KeyDescriptor("o") to "○", KeyDescriptor("O") to "⍥",
            KeyDescriptor("p") to "⋆", KeyDescriptor("P") to "⍣",
            KeyDescriptor("[") to "←", KeyDescriptor("{") to "⍞",
            KeyDescriptor("]") to "→", KeyDescriptor("}") to "⍬",
            KeyDescriptor("\\") to "⊢", KeyDescriptor("|") to "⊣",
            // Third row
            KeyDescriptor("a") to "⍺", KeyDescriptor("A") to "⍶",
            KeyDescriptor("s") to "⌈",
            KeyDescriptor("d") to "⌊",
            KeyDescriptor("f") to "_", KeyDescriptor("F") to "⍫",
            KeyDescriptor("g") to "∇",
            KeyDescriptor("h") to "∆", KeyDescriptor("H") to "⍙",
            KeyDescriptor("j") to "∘", KeyDescriptor("J") to "⍤",
            KeyDescriptor("k") to "'", KeyDescriptor("K") to "⌺",
            KeyDescriptor("l") to "⎕", KeyDescriptor("L") to "⌷",
            KeyDescriptor(";") to "⍎", KeyDescriptor(":") to "≡",
            KeyDescriptor("'") to "⍕", KeyDescriptor("\"") to "≢",
            // Fourth row
            KeyDescriptor("z") to "⊂",
            KeyDescriptor("x") to "⊃", KeyDescriptor("X") to "χ",
            KeyDescriptor("c") to "∩", KeyDescriptor("C") to "⍧",
            KeyDescriptor("v") to "∪",
            KeyDescriptor("b") to "⊥", KeyDescriptor("B") to "£",
            KeyDescriptor("n") to "⊤",
            KeyDescriptor("m") to "|",
            KeyDescriptor(",") to "⍝", KeyDescriptor("<") to "⍪",
            KeyDescriptor(".") to "⍝", KeyDescriptor(">") to "⍀",
            KeyDescriptor("/") to "⌿", KeyDescriptor("?") to "⍠"
        )
    }

    fun addEventHandlerToNode(node: TextInputControl) {
        node.onKeyTyped = EventHandler { event -> handleKeyTyped(node, event) }
    }

    private fun handleKeyTyped(node: TextInputControl, event: KeyEvent) {
        if (event.isAltDown) {
            val desc = keymap[KeyDescriptor(event.character)]
            if(desc != null) {
                node.insertText(node.caretPosition, desc)
                event.consume()
            }
        }
    }

    private data class KeyDescriptor(val character: String)
}
