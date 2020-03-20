package array.builtins

import array.*

class TypeofFunction : APLFunctionDescriptor {
    class TypeofFunctionImpl(pos: Position) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            val v = a.unwrapDeferredValue()
            return APLSymbol(context.engine.internSymbol(v.aplValueType.typeName))
        }
    }

    override fun make(pos: Position) = TypeofFunctionImpl(pos)
}
