package array.gui

import javafx.event.EventHandler
import javafx.scene.control.TextInputControl
import javafx.scene.input.KeyEvent

class ExtendedCharsKeyboardInput {
    val keymap: Map<KeyDescriptor, String>

    init {
        keymap = hashMapOf(
            // First row
            makeKeyDescriptor("`") to "◊",
            makeKeyDescriptor("1") to "¨", makeKeyDescriptor("!", Flag.SHIFT) to "⌶",
            makeKeyDescriptor("2") to "¯", makeKeyDescriptor("@", Flag.SHIFT) to "⍫",
            makeKeyDescriptor("3") to "<", makeKeyDescriptor("#", Flag.SHIFT) to "⍒",
            makeKeyDescriptor("4") to "≤", makeKeyDescriptor("$", Flag.SHIFT) to "⍋",
            makeKeyDescriptor("5") to "=", makeKeyDescriptor("%", Flag.SHIFT) to "⌽",
            makeKeyDescriptor("6") to "≥", makeKeyDescriptor("^", Flag.SHIFT) to "⍉",
            makeKeyDescriptor("7") to ">", makeKeyDescriptor("&", Flag.SHIFT) to "⊖",
            makeKeyDescriptor("8") to "≠", makeKeyDescriptor("*", Flag.SHIFT) to "⍟",
            makeKeyDescriptor("9") to "∨", makeKeyDescriptor("(", Flag.SHIFT) to "⍱",
            makeKeyDescriptor("0") to "∧", makeKeyDescriptor(")", Flag.SHIFT) to "⍲",
            makeKeyDescriptor("-") to "×", makeKeyDescriptor("_", Flag.SHIFT) to "!",
            makeKeyDescriptor("=") to "÷", makeKeyDescriptor("+", Flag.SHIFT) to "⌹",
            // Second row
            // q is unassigned
            makeKeyDescriptor("w") to "⍵", makeKeyDescriptor("W", Flag.SHIFT) to "⍹",
            makeKeyDescriptor("e") to "∊", makeKeyDescriptor("E", Flag.SHIFT) to "⍷",
            makeKeyDescriptor("r") to "⍴",
            makeKeyDescriptor("t") to "∼", makeKeyDescriptor("T", Flag.SHIFT) to "⍨",
            makeKeyDescriptor("y") to "↑", makeKeyDescriptor("Y", Flag.SHIFT) to "¥",
            makeKeyDescriptor("u") to "↓",
            makeKeyDescriptor("i") to "⍳", makeKeyDescriptor("I", Flag.SHIFT) to "⍸",
            makeKeyDescriptor("o") to "○", makeKeyDescriptor("O", Flag.SHIFT) to "⍥",
            makeKeyDescriptor("p") to "⋆", makeKeyDescriptor("P", Flag.SHIFT) to "⍣",
            makeKeyDescriptor("[") to "←", makeKeyDescriptor("{", Flag.SHIFT) to "⍞",
            makeKeyDescriptor("]") to "→", makeKeyDescriptor("}", Flag.SHIFT) to "⍬",
            makeKeyDescriptor("\\") to "⊢", makeKeyDescriptor("|", Flag.SHIFT) to "⊣",
            // Third row
            makeKeyDescriptor("a") to "⍺", makeKeyDescriptor("A", Flag.SHIFT) to "⍶",
            makeKeyDescriptor("s") to "⌈",
            makeKeyDescriptor("d") to "⌊",
            makeKeyDescriptor("f") to "_", makeKeyDescriptor("F", Flag.SHIFT) to "⍫",
            makeKeyDescriptor("g") to "∇",
            makeKeyDescriptor("h") to "∆", makeKeyDescriptor("H", Flag.SHIFT) to "⍙",
            makeKeyDescriptor("j") to "∘", makeKeyDescriptor("J", Flag.SHIFT) to "⍤",
            makeKeyDescriptor("k") to "'", makeKeyDescriptor("K", Flag.SHIFT) to "⌺",
            makeKeyDescriptor("l") to "⎕", makeKeyDescriptor("L", Flag.SHIFT) to "⌷",
            makeKeyDescriptor(";") to "⍎", makeKeyDescriptor(":", Flag.SHIFT) to "≡",
            makeKeyDescriptor("'") to "⍕", makeKeyDescriptor("\"", Flag.SHIFT) to "≢",
            // Fourth row
            makeKeyDescriptor("z") to "⊂",
            makeKeyDescriptor("x") to "⊃", makeKeyDescriptor("X", Flag.SHIFT) to "χ",
            makeKeyDescriptor("c") to "∩", makeKeyDescriptor("C", Flag.SHIFT) to "⍧",
            makeKeyDescriptor("v") to "∪",
            makeKeyDescriptor("b") to "⊥", makeKeyDescriptor("B", Flag.SHIFT) to "£",
            makeKeyDescriptor("n") to "⊤", makeKeyDescriptor("N", Flag.SHIFT) to "λ",
            makeKeyDescriptor("m") to "|",
            makeKeyDescriptor(",") to "⍝", makeKeyDescriptor("<", Flag.SHIFT) to "⍪",
            makeKeyDescriptor(".") to "⍝", makeKeyDescriptor(">", Flag.SHIFT) to "⍀",
            makeKeyDescriptor("/") to "⌿", makeKeyDescriptor("?", Flag.SHIFT) to "⍠")
    }

    fun addEventHandlerToNode(node: TextInputControl) {
        node.onKeyTyped = EventHandler { event -> handleKeyTyped(node, event) }
    }

    private fun handleKeyTyped(node: TextInputControl, event: KeyEvent) {
        if (event.isAltDown) {
            val flags = if (event.isShiftDown) arrayOf(Flag.SHIFT) else emptyArray()
            val desc = keymap[makeKeyDescriptor(event.character, *flags)]
            if (desc != null) {
                node.insertText(node.caretPosition, desc)
                event.consume()
            }
        }
    }

    enum class Flag {
        SHIFT
    }

    private fun makeKeyDescriptor(character: String, vararg flags: Flag): KeyDescriptor {
        return KeyDescriptor(character, flags.any { it == Flag.SHIFT })
    }

    data class KeyDescriptor(val character: String, val shift: Boolean)
}
