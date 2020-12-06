package array.clientweb

import array.registeredFilesRoot
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.xhr.XMLHttpRequest
import react.dom.render

@OptIn(ExperimentalJsExport::class)
fun main() {
    window.onload = {
        loadLibraries()
    }
}

var numOutstandingRequests = 0

fun loadLibraries() {
    loadLibFiles(
        "standard-lib/standard-lib.kap",
        "standard-lib/structure.kap",
        "standard-lib/output.kap",
        "standard-lib/time.kap",
        "standard-lib/math.kap")
}

private fun loadLibFiles(vararg names: String) {
    numOutstandingRequests = names.size
    names.forEach { name ->
        val http = XMLHttpRequest()
        http.open("GET", name)
        http.onload = {
            if (http.readyState == 4.toShort() && http.status == 200.toShort()) {
                registeredFilesRoot.registerFile(name, http.responseText.encodeToByteArray())
            } else {
                console.log("Error loading library file: ${name}. Code: ${http.status}")
            }
            if (--numOutstandingRequests == 0) {
                renderClient()
            }
        }
        http.send()
    }
}

var clientStarted = false

private fun renderClient() {
    if (!clientStarted) {
        clientStarted = true
        render(document.getElementById("root")) {
            child(KAPInteractiveClient::class) {
                attrs {
                    name = ""
                }
            }
        }
    }
}
