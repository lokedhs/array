package array.builtins

import array.*

private fun toUnicodeCodepoint(valueAt: APLValue): APLValue {
    val v = valueAt.unwrapDeferredValue()
    return when (v) {
        is APLChar -> v.value.makeAPLNumber()
        is APLSingleValue -> v
        else -> ToUnicodeValue(v)
    }
}

private class ToUnicodeValue(val value: APLValue) : APLArray() {
    override val dimensions = value.dimensions

    override fun valueAt(p: Int): APLValue {
        return toUnicodeCodepoint(value.valueAt(p))
    }
}

class MakeCodepoints : APLFunctionDescriptor {
    class MakeCodepointsImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return toUnicodeCodepoint(a)
        }
    }

    override fun make(pos: Position) = MakeCodepointsImpl(pos)
}

class UnicodeModule : KapModule {
    override val name = "unicode"

    override fun init(engine: Engine) {
        val namespace = engine.makeNamespace("unicode")
        engine.registerFunction(namespace.internAndExport("toCodepoints"), MakeCodepoints())
    }
}
