package array.clientweb

import kotlinx.browser.document
import kotlinx.browser.window
import react.dom.render

@OptIn(ExperimentalJsExport::class)
fun main() {
    window.onload = {
        render(document.getElementById("root")) {
            child(KAPInteractiveClient::class) {
                attrs {
                    name = ""
                }
            }
        }
    }
}
