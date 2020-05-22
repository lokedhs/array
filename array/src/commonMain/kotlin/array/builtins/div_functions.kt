package array.builtins

import array.*

class TypeofFunction : APLFunctionDescriptor {
    class TypeofFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val v = a.unwrapDeferredValue()
            return APLSymbol(context.engine.internSymbol(v.aplValueType.typeName))
        }
    }

    override fun make(pos: Position) = TypeofFunctionImpl(pos)
}

class IsLocallyBoundFunction : APLFunctionDescriptor {
    class IsLocallyBoundFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val v = a.unwrapDeferredValue()
            return makeBoolean(context.lookupVar(v.ensureSymbol(pos).value, true) != null)
        }
    }

    override fun make(pos: Position) = IsLocallyBoundFunctionImpl(pos)
}

class CompFunction : APLFunctionDescriptor {
    class CompFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            return a.collapse()
        }
    }

    override fun make(pos: Position): APLFunction {
        return CompFunctionImpl(pos)
    }
}
