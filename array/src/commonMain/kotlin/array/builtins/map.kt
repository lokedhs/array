package array.builtins

import array.*

class MapAPLFunction : APLFunctionDescriptor {
    class MapAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val a1 = ensureKeyValuesArray(a, pos)
            val content = ArrayList<Pair<Any, APLValue>>()
            repeat(a1.dimensions[0]) { i ->
                val key = a1.valueAt(i * 2).collapse()
                val value = a1.valueAt(i * 2 + 1).collapse()
                content.add(Pair(key.makeKey(), value))
            }
            return APLMap(ImmutableMap2.makeFromContent(content))
        }
    }

    override fun make(pos: Position) = MapAPLFunctionImpl(pos)
}

class MapLookupResult(val map: APLMap, val indexes: APLValue) : APLArray() {
    override val dimensions = indexes.dimensions

    override fun valueAt(p: Int): APLValue {
        val key = indexes.valueAt(p).unwrapDeferredValue()
        return map.lookupValue(key)
    }


    override fun unwrapDeferredValue(): APLValue {
        // This is copied from reduce. The same concerns as described in that comment are applicable in this function.
        if (dimensions.isEmpty()) {
            val v = valueAt(0).unwrapDeferredValue()
            if (v is APLSingleValue) {
                return v
            }
        }
        return this
    }
}

class MapGetAPLFunction : APLFunctionDescriptor {
    class MapGetAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val map = ensureMap(a, pos)
            val bValue = b.unwrapDeferredValue()
            return if (bValue is APLSingleValue) {
                map.lookupValue(bValue)
            } else {
                MapLookupResult(map, bValue)
            }
        }
    }

    override fun make(pos: Position) = MapGetAPLFunctionImpl(pos)
}

private fun ensureKeyValuesArray(a: APLValue, pos: Position): APLValue {
    fun raiseDimensionError() {
        throw APLIllegalArgumentException(
            "Right argument to map should be either a rank-1 array with 2 elements or a rank-2 array with 2 columns",
            pos)
    }

    return if (a.dimensions.size == 1) {
        unless(a.dimensions[0] == 2) {
            raiseDimensionError()
        }
        ResizedArray(dimensionsOfSize(1, 2), a)
    } else {
        unless(a.dimensions.size == 2 && a.dimensions[1] == 2) {
            raiseDimensionError()
        }
        a
    }
}

private fun ensureMap(a: APLValue, pos: Position): APLMap {
    val map = a.unwrapDeferredValue()
    if (map !is APLMap) {
        throw IncompatibleTypeException("Left argument must be a map", pos)
    }
    return map
}

class MapPutAPLFunction : APLFunctionDescriptor {
    class MapPutAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val map = ensureMap(a, pos)
            val b1 = ensureKeyValuesArray(b, pos)
            if (b1.dimensions[0] > 0) {
                val content = ArrayList<Pair<APLValue, APLValue>>()
                repeat(b1.dimensions[0]) { i ->
                    val key = b1.valueAt(i * 2).collapse()
                    val value = b1.valueAt(i * 2 + 1).collapse()
                    content.add(Pair(key, value))
                }
                return map.updateValues(content)
            } else {
                return a
            }
        }
    }

    override fun make(pos: Position) = MapPutAPLFunctionImpl(pos)
}
