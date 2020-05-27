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
            return makeBoolean(context.isLocallyBound(v.ensureSymbol(pos).value))
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

class SleepFunction : APLFunctionDescriptor {
    class SleepFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val sleepTimeSeconds = a.ensureNumber(pos).asDouble()
            sleepMillis((sleepTimeSeconds * 1000).toLong())
            return sleepTimeSeconds.makeAPLNumber()
        }
    }

    override fun make(pos: Position) = SleepFunctionImpl(pos)
}

class TagCatch(val tag: APLValue, val data: APLValue) : RuntimeException()

class ThrowFunction : APLFunctionDescriptor {
    class ThrowFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            throw TagCatch(b, a)
        }
    }

    override fun make(pos: Position) = ThrowFunctionImpl(pos)
}
