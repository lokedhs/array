package array.builtins
//≠∵⌺⍨ ⍳30
import array.*

abstract class BitwiseCombineAPLFunction(pos: Position) : MathCombineAPLFunction(pos) {
    override val optimisationFlags get() = OptimisationFlags(OptimisationFlags.OPTIMISATION_FLAG_1ARG_LONG or OptimisationFlags.OPTIMISATION_FLAG_2ARG_LONG_LONG)

    override fun combine1Arg(a: APLSingleValue): APLValue = bitwiseCombine1Arg(a.ensureNumber(pos).asLong()).makeAPLNumber()
    override fun combine2Arg(a: APLSingleValue, b: APLSingleValue): APLValue =
        bitwiseCombine2Arg(a.ensureNumber(pos).asLong(), b.ensureNumber(pos).asLong()).makeAPLNumber()

    override fun combine1ArgLong(a: Long) = bitwiseCombine1Arg(a)
    override fun combine2ArgLong(a: Long, b: Long) = bitwiseCombine2Arg(a, b)

    open fun bitwiseCombine1Arg(a: Long): Long = throwAPLException(Unimplemented1ArgException(pos))
    open fun bitwiseCombine2Arg(a: Long, b: Long): Long = throwAPLException(Unimplemented2ArgException(pos))
}

class BitwiseOp : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, operatorAxis: Instruction?, pos: Position): APLFunctionDescriptor {
        if (operatorAxis != null) {
            throw AxisNotSupported(pos)
        }
        return fn.deriveBitwise() ?: throw BitwiseNotSupported(pos)
    }
}

class BitwiseAndFunction : APLFunctionDescriptor {
    class BitwiseAndFunctionImpl(pos: Position) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine2Arg(a: Long, b: Long) = a and b
    }

    override fun make(pos: Position) = BitwiseAndFunctionImpl(pos.withName("bitwise and"))
}

class BitwiseOrFunction : APLFunctionDescriptor {
    class BitwiseOrFunctionImpl(pos: Position) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine2Arg(a: Long, b: Long) = a or b
    }

    override fun make(pos: Position) = BitwiseOrFunctionImpl(pos.withName("bitwise or"))
}

class BitwiseXorFunction : APLFunctionDescriptor {
    class BitwiseXorFunctionImpl(pos: Position) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine2Arg(a: Long, b: Long) = a xor b
    }

    override fun make(pos: Position) = BitwiseXorFunctionImpl(pos.withName("bitwise xor"))
}

class BitwiseNotFunction : APLFunctionDescriptor {
    class BitwiseNotFunctionImpl(pos: Position) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine1Arg(a: Long) = a.inv()
    }

    override fun make(pos: Position) = BitwiseNotFunctionImpl(pos.withName("bitwise not"))
}

class BitwiseNandFunction : APLFunctionDescriptor {
    class BitwiseNandFunctionImpl(pos: Position) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine2Arg(a: Long, b: Long) = (a and b).inv()
    }

    override fun make(pos: Position) = BitwiseNandFunctionImpl(pos.withName("bitwise nand"))
}

class BitwiseNorFunction : APLFunctionDescriptor {
    class BitwiseNorFunctionImpl(pos: Position) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine2Arg(a: Long, b: Long) = (a or b).inv()
    }

    override fun make(pos: Position) = BitwiseNorFunctionImpl(pos.withName("bitwise nor"))
}

// TODO: Need to assign this to the appropriate parent function
class BitwiseCountBitsFunction : APLFunctionDescriptor {
    class BitwiseCountBitsFunctionImpl(pos: Position) : BitwiseCombineAPLFunction(pos) {
        override fun bitwiseCombine1Arg(a: Long): Long {
            var total = 0L
            repeat(64) { i ->
                if (a and (1L shl i) > 0) {
                    total++
                }
            }
            return total
        }
    }

    override fun make(pos: Position) = BitwiseCountBitsFunctionImpl(pos.withName("bitwise count bits"))
}
