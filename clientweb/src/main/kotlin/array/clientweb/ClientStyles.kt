package array.clientweb

import kotlinx.css.*
import styled.StyleSheet

object ClientStyles : StyleSheet("ClientStyles", isStatic = true) {
    val textContainer by css {
        padding(5.px)

        backgroundColor = rgb(255, 255, 255)
        color = rgb(0, 0, 0)
    }

    val textInput by css {
        margin(vertical = 5.px)

        fontSize = 14.px
    }

    val table by css {
        borderCollapse = BorderCollapse.collapse
        td {
            borderWidth = 1.px
            borderColor = Color.black
            borderStyle = BorderStyle.solid
            padding(all = 2.px)
        }
    }
}
