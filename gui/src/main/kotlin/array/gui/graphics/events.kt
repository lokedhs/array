package array.gui.graphics

import array.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent

interface KapWindowEvent {
    fun makeAPLValue(engine: Engine): APLValue?
}

class KapKeyEvent(val event: KeyEvent) : KapWindowEvent {
    companion object {
        val codeToKeyword: Map<KeyCode, String> = mapOf(
            KeyCode.UP to "up",
            KeyCode.DOWN to "down",
            KeyCode.LEFT to "left",
            KeyCode.RIGHT to "right",
            KeyCode.F1 to "f1",
            KeyCode.F2 to "f2",
            KeyCode.F3 to "f3",
            KeyCode.F4 to "f4",
            KeyCode.F5 to "f5",
            KeyCode.F6 to "f6",
            KeyCode.F7 to "f7",
            KeyCode.F8 to "f8",
            KeyCode.F9 to "f9",
            KeyCode.F10 to "f10",
            KeyCode.F11 to "f11",
            KeyCode.F12 to "f12")
    }

    override fun makeAPLValue(engine: Engine): APLValue? {
        if (event.text != "") {
            return APLString(event.text)
        }
        val keyword = codeToKeyword[event.code]
        if (keyword != null) {
            return APLSymbol(engine.keywordNamespace.internSymbol(keyword))
        }
        return null
    }
}

private fun winFromValue(graphicValue: APLValue, pos: Position): GraphicWindow {
    return graphicValue.unwrapDeferredValue().let { v ->
        if (v !is GraphicWindowAPLValue) {
            throwAPLException(APLIllegalArgumentException("Left argument must be a graphic window", pos))
        }
        v.window
    }
}

private fun parseAcceptedEventTypes(context: RuntimeContext, typesValue: APLValue, pos: Position): MutableList<EventType> {
    val keyEventType = context.engine.keywordNamespace.internSymbol("keypress")
    val acceptedTypes = mutableListOf<EventType>()
    typesValue.arrayify().iterateMembers { v ->
        if (v !is APLSymbol) {
            throwAPLException(APLIllegalArgumentException("Right argument must be a symbol or a list of symbols", pos))
        }
        val keywordSym = v.value
        when {
            keywordSym === keyEventType -> acceptedTypes.add(KapKeyEvent::class)
        }
    }
    return acceptedTypes
}

class ReadEventFunction : APLFunctionDescriptor {
    class ReadEventFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val win = winFromValue(a, pos)
            val acceptedTypes = parseAcceptedEventTypes(context, b, pos)
            while (true) {
                val event = win.nextEvent() ?: break
                if (acceptedTypes.contains(event::class)) {
                    val v = event.makeAPLValue(context.engine)
                    if (v != null) {
                        return v
                    }
                }
            }
            return APLNullValue.APL_NULL_INSTANCE
        }
    }

    override fun make(pos: Position) = ReadEventFunctionImpl(pos)
}

class ReadEventBlockingFunction : APLFunctionDescriptor {
    class ReadEventBlockingFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val win = winFromValue(a, pos)
            val acceptedTypes = parseAcceptedEventTypes(context, b, pos)
            while (true) {
                val event = win.waitForEvent()
                if (acceptedTypes.contains(event::class)) {
                    val v = event.makeAPLValue(context.engine)
                    if (v != null) {
                        return v
                    }
                }
            }
        }
    }

    override fun make(pos: Position) = ReadEventBlockingFunctionImpl(pos)
}

class EnableEventsFunction : APLFunctionDescriptor {
    class EnableEventsFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val win = winFromValue(a, pos)
            val acceptedTypes = parseAcceptedEventTypes(context, b, pos)
            acceptedTypes.forEach { v -> win.addEnabledEvent(v) }
            return APLNullValue.APL_NULL_INSTANCE
        }
    }

    override fun make(pos: Position) = EnableEventsFunctionImpl(pos)
}

class DisableEventsFunction : APLFunctionDescriptor {
    class DisableEventsFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val win = winFromValue(a, pos)
            val acceptedTypes = parseAcceptedEventTypes(context, b, pos)
            acceptedTypes.forEach { v -> win.removeEnabledEvent(v) }
            return APLNullValue.APL_NULL_INSTANCE
        }
    }

    override fun make(pos: Position) = DisableEventsFunctionImpl(pos)
}
