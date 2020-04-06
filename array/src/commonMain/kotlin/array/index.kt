package array

private class IndexedArrayValue(val content: APLValue, val indexValue: Array<AxisSelection>) : APLArray() {
    private val dimensions: Dimensions
    private val destToSourceAxis: IntArray
    private val constantOffset: Int

    init {
        val contentMult = content.dimensions().multipliers()

        var offset = 0
        val a = ArrayList<Int>()
        val destAxis = ArrayList<Int>()
        indexValue.forEachIndexed { i, selection ->
            if (selection.multiIndex != null) {
                a.add(selection.multiIndex.size)
                destAxis.add(contentMult[i])
            } else {
                offset += contentMult[i] * selection.singleIndex!!
            }
        }
        dimensions = Dimensions(a.toIntArray())
        destToSourceAxis = destAxis.toIntArray()
        constantOffset = offset
    }

    override fun dimensions() = dimensions

    override fun valueAt(p: Int): APLValue {
        val positionArray = dimensions.positionFromIndex(p)
        var result = constantOffset
        for (i in positionArray.indices) {
            val positionInAxis = positionArray[i]
            val selection = indexValue[i]
            val m = selection.multiIndex
            result += destToSourceAxis[i] * if (m != null) m[positionInAxis] else selection.singleIndex!!
        }
        return content.valueAt(result)
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

private class AxisSelection {
    val singleIndex: Int?
    val multiIndex: IntArray?

    constructor(s: Int) {
        singleIndex = s
        multiIndex = null
    }

    constructor(m: IntArray) {
        singleIndex = null
        multiIndex = m
    }
}

class ArrayIndex(val content: Instruction, val indexInstr: Instruction, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val contentValue = content.evalWithContext(context)
        val indexValue = indexInstr.evalWithContext(context)

        val indexAsList = convertToList(indexValue)
        if (indexAsList.listSize() != contentValue.dimensions().size) {
            throw InvalidDimensionsException("Rank of argument does not match index. Argument=${contentValue.dimensions().size}, index=${indexAsList.listSize()}",
                pos)
        }
        val axis = Array(indexAsList.listSize()) { i ->
            val v = indexAsList.listElement(i).unwrapDeferredValue()
            val d = v.dimensions()
            when (d.size) {
                0 -> AxisSelection(v.ensureNumber(pos).asInt())
                1 -> AxisSelection(IntArray(d[0]) { i2 -> v.valueAt(i2).ensureNumber(pos).asInt() })
                else -> throw InvalidDimensionsException("Invalid dimension in array index argument ${i}", pos)
            }
        }

        return IndexedArrayValue(contentValue, axis)
    }

    private fun convertToList(value: APLValue): APLList {
        val v = value.unwrapDeferredValue()
        return if (v is APLList) {
            v
        } else {
            APLList(listOf(v))
        }
    }
}
