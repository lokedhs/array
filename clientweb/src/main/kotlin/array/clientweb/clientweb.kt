package array.clientweb

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.InputType
import kotlinx.html.div
import kotlinx.html.dom.append
import kotlinx.html.input
import kotlinx.html.onSubmit
import org.w3c.dom.Node

fun main() {
    window.onload = { document.body?.sayHello() }
}

fun Node.sayHello() {
    append {
        div {
            +"Web client for KAP"
        }
        div {
            input(type = InputType.text) {
                +"foo"
            }
        }
    }
}
