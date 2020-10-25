package array.clientweb

import array.Engine
import array.FormatStyle
import array.StringSourceLocation
import kotlinx.css.LinearDimension
import kotlinx.css.width
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onKeyPressFunction
import org.w3c.dom.HTMLInputElement
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import styled.*

external interface ClientProps : RProps {
    var name: String
}

data class HistoryResult(val input: String, val result: String) : RState
data class ClientState(val inputText: String, val history: List<HistoryResult>) : RState

@OptIn(ExperimentalJsExport::class)
@JsExport
class KAPInteractiveClient(props: ClientProps) : RComponent<ClientProps, ClientState>(props) {

    val engine = Engine()

    init {
        state = ClientState(props.name, ArrayList())
    }

    override fun RBuilder.render() {
        styledDiv {
            +"""
             Type a KAP statement in the input box and press return to execute it.
             Note that evaluation is performed asynchronously,
             which will block the browser while the command is run.
             """.trimIndent()
        }
        styledDiv {
            css {
                +ClientStyles.textContainer
            }
            state.history.forEach { entry ->
                styledHr { }
                styledDiv {
                    styledDiv { +entry.input }
                    styledPre { +entry.result }
                }
            }
        }
        styledInput {
            css {
                width = LinearDimension.fillAvailable
            }
            attrs {
                type = InputType.text
                value = state.inputText
                onChangeFunction = { event ->
                    setState(ClientState((event.target as HTMLInputElement).value, state.history))
                }
                onKeyPressFunction = { event ->
                    console.log("event = ${event}, type = ${event.type}")
                    val keyEvent: dynamic = event
                    console.log("event.key = ${keyEvent.key}")
                    if (keyEvent.key == "Enter") {
                        val inputText = state.inputText
                        val result = engine.parseAndEval(StringSourceLocation(inputText), false)
                        val history = ArrayList(state.history)
                        val s = result.formatted(FormatStyle.PRETTY)
                        history.add(HistoryResult(inputText, s))
                        setState(ClientState(inputText, history))
                    }
                }
            }
        }
    }
}
