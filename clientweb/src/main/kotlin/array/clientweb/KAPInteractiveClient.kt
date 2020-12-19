package array.clientweb

import array.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.css.*
import kotlinx.html.HtmlBlockTag
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
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

data class HistoryResult(val input: String, val result: APLValue?, val errorMessage: String?) : RState
data class ClientState(val inputText: String, val history: List<HistoryResult>) : RState

@OptIn(ExperimentalJsExport::class)
@JsExport
class KAPInteractiveClient(props: ClientProps) : RComponent<ClientProps, ClientState>(props) {

    private val engine = Engine()

    init {
        engine.addLibrarySearchPath("standard-lib")
        try {
            engine.parseAndEval(StringSourceLocation("use(\"standard-lib.kap\")"), false)
        } catch (e: APLGenericException) {
            console.log("Error loading standard-lib: ${e.message}")
        }
        state = ClientState(props.name, ArrayList())
    }

    override fun componentDidUpdate(prevProps: ClientProps, prevState: ClientState, snapshot: Any) {
        val body = document.body
        if (body != null) {
            window.scrollTo(0.0, body.scrollHeight.toDouble())
        }
    }

    override fun RBuilder.render() {
        styledP {
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
                    styledDiv {
                        css {
                            cursor = Cursor.pointer
                        }
                        val input = entry.input
                        +input
                        attrs {
                            onClickFunction = { event ->
                                setState(ClientState(input, state.history))
                            }
                        }
                    }
                    if (entry.result != null) {
                        styledDiv {
                            renderValue(entry.result)
                        }
                    } else {
                        if (entry.errorMessage == null) {
                            throw RuntimeException("No result nor error message")
                        }
                        styledDiv {
                            css {
                                color = Color.red
                            }
                            +entry.errorMessage
                        }
                    }
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
                    val keyEvent: dynamic = event
                    if (keyEvent.key == "Enter") {
                        val inputText = state.inputText
                        var historyResult: HistoryResult
                        try {
                            val result = engine.parseAndEval(StringSourceLocation(inputText), false).collapse()
                            historyResult = HistoryResult(inputText, result, null)
                        } catch (e: APLGenericException) {
                            historyResult = HistoryResult(inputText, null, e.formattedError())
                        }
                        val history = ArrayList(state.history)
                        history.add(historyResult)
                        setState(ClientState(inputText, history))
                    }
                }
            }
        }
    }

    private fun StyledDOMBuilder<HtmlBlockTag>.renderValue(value: APLValue) {
        val d = value.dimensions
        when {
            value is APLSingleValue -> +value.formatted(FormatStyle.PRETTY)
            value.isStringValue() -> +value.formatted(FormatStyle.PRETTY)
            d.size == 0 -> genericTableRender(1, 1, { _, _ -> value.valueAt(0) })
            d.size == 1 -> genericTableRender(1, d[0], { _, col -> value.valueAt(col) })
            d.size == 2 -> renderTableValue2D(value)
            else -> styledPre { +value.formatted(FormatStyle.PRETTY) }
        }
    }

    private fun StyledDOMBuilder<HtmlBlockTag>.renderTableValue2D(value: APLValue) {
        val d = value.dimensions
        val multipliers = d.multipliers()
        genericTableRender(d[0], d[1], { row, col -> value.valueAt(d.indexFromPosition(intArrayOf(row, col), multipliers)) })
    }

    private fun StyledDOMBuilder<HtmlBlockTag>.genericTableRender(
        numRows: Int,
        numCols: Int,
        reader: (row: Int, col: Int) -> APLValue) {
        styledTable {
            css {
                +ClientStyles.table
            }
            styledTbody {
                repeat(numRows) { row ->
                    styledTr {
                        repeat(numCols) { col ->
                            styledTd {
                                renderValue(reader(row, col))
                            }
                        }
                    }
                }
            }
        }
    }
}
