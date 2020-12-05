package array

import array.builtins.IotaArray

private class IndexedArrayValue(val content: APLValue, indexValue: Array<Either<Int, IntArrayValue>>) : APLArray() {
    class AxisValueAndOffset(
        val sourceIndex: Int,
        val source: IntArrayValue,
        val sourceMultipliers: IntArray,
        val multiplier: Int)

    override val dimensions: Dimensions
    private val destToSourceAxis: List<AxisValueAndOffset>
    private val constantOffset: Int

    init {
        val contentMult = content.dimensions.multipliers()

        var offset = 0
        val a = ArrayList<Int>()
        val destAxis = ArrayList<AxisValueAndOffset>()
        var outputAxis = 0
        indexValue.forEachIndexed { i, selection ->
            when (selection) {
                is Either.Left -> {
                    offset += contentMult[i] * selection.value
                }
                is Either.Right -> {
                    selection.value.dimensions.dimensions.forEach { v ->
                        a.add(v)
                    }
                    destAxis.add(
                        AxisValueAndOffset(
                            outputAxis,
                            selection.value,
                            selection.value.dimensions.multipliers(),
                            contentMult[i]))
                    outputAxis += selection.value.dimensions.size
                }
            }
        }
        dimensions = Dimensions(a.toIntArray())
        destToSourceAxis = destAxis
        constantOffset = offset
    }

    override fun valueAt(p: Int): APLValue {
        val positionArray = dimensions.positionFromIndex(p)
        var result = constantOffset
        destToSourceAxis.forEach { dts ->
            val srcCoords = IntArray(dts.source.rank) { i -> positionArray[dts.sourceIndex + i] }
            val srcAxisPos = dts.source.intValueAt(dts.source.dimensions.indexFromPosition(srcCoords, dts.sourceMultipliers))
            result += srcAxisPos * dts.multiplier
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

class ArrayIndex(val content: Instruction, val indexInstr: Instruction, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val indexValue = indexInstr.evalWithContext(context)
        val contentValue = content.evalWithContext(context)

        val aDimensions = contentValue.dimensions

        val indexAsList = convertToList(indexValue)
        if (indexAsList.listSize() != contentValue.dimensions.size) {
            throwAPLException(
                InvalidDimensionsException(
                    "Rank of argument does not match index. Argument=${aDimensions.size}, index=${indexAsList.listSize()}",
                    pos))
        }
        val axis = Array(indexAsList.listSize()) { i ->
            val v = indexAsList.listElement(i).unwrapDeferredValue().let { result ->
                if (result is APLEmpty) {
                    IotaArray(intArrayOf(aDimensions[i]))
                } else {
                    result
                }
            }
            val d = v.dimensions
            if (d.size == 0) {
                Either.Left(v.ensureNumber(pos).asInt()
                    .also { posAlongAxis -> checkAxisPositionIsInRange(posAlongAxis, aDimensions, i, pos) })
            } else {
                Either.Right(IntArrayValue.fromAPLValue(v, pos).also { selectionArray ->
                    selectionArray.values.forEach { posAlongAxis ->
                        checkAxisPositionIsInRange(posAlongAxis, aDimensions, i, pos)
                    }
                })
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
