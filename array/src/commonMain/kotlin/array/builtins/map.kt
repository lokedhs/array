package array.builtins

import array.*

class MapAPLFunction : APLFunctionDescriptor {
    class MapAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val a1 = ensureKeyValuesArray(a, pos)
            val content = ArrayList<Pair<APLValue.APLValueKey, APLValue>>()
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
        return unwrapEnclosedSingleValue(this)
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
        throwAPLException(
            APLIllegalArgumentException(
                "Right argument to map should be either a rank-1 array with 2 elements or a rank-2 array with 2 columns",
                pos))
    }

    return if (a.dimensions.size == 1) {
        unless(a.dimensions[0] == 2) {
            raiseDimensionError()
        }
        ResizedArrayImpls.makeResizedArray(dimensionsOfSize(1, 2), a)
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
        throwAPLException(IncompatibleTypeException("Left argument must be a map", pos))
    }
    return map
}

class MapPutAPLFunction : APLFunctionDescriptor {
    class MapPutAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val map = ensureMap(a, pos)
            val b1 = ensureKeyValuesArray(b, pos)
            return if (b1.dimensions[0] > 0) {
                val content = ArrayList<Pair<APLValue, APLValue>>()
                repeat(b1.dimensions[0]) { i ->
                    val key = b1.valueAt(i * 2).collapse()
                    val value = b1.valueAt(i * 2 + 1).collapse()
                    content.add(Pair(key, value))
                }
                map.updateValues(content)
            } else {
                a
            }
        }
    }

    override fun make(pos: Position) = MapPutAPLFunctionImpl(pos)
}

class MapRemoveAPLFunction : APLFunctionDescriptor {
    class MapRemoveAPLFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val map = ensureMap(a, pos)
            val b1 = b.arrayify().collapse()
            if (b1.dimensions.size != 1) {
                throwAPLException(InvalidDimensionsException("Right argument should be a scalar or a rank-1 array", pos))
            }
            val toRemove = ArrayList<APLValue>()
            b1.iterateMembers { value ->
                toRemove.add(value)
            }
            return map.removeValues(toRemove)
        }
    }

    override fun make(pos: Position) = MapRemoveAPLFunctionImpl(pos)
}

class MapKeyValuesFunction : APLFunctionDescriptor {
    class MapKeyValuesFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return a.ensureMap(pos).aplMapToArray()
        }
    }

    override fun make(pos: Position) = MapKeyValuesFunctionImpl(pos)
}
