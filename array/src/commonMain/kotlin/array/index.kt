package array

import array.builtins.IotaArray
import array.builtins.unwrapEnclosedSingleValue

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
        return unwrapEnclosedSingleValue(this)
    }
}

class ArrayIndex(val content: Instruction, val indexInstr: Instruction, pos: Position) : Instruction(pos) {
    override fun evalWithContext(context: RuntimeContext): APLValue {
        val indexValue = indexInstr.evalWithContext(context)
        val contentValue = content.evalWithContext(context).unwrapDeferredValue()

        val aDimensions = contentValue.dimensions

        return if (contentValue is APLMap) {
            contentValue.lookupValue(indexValue)
        } else {
            lookupFromArray(indexValue, contentValue, aDimensions)
        }
    }

    private fun lookupFromArray(
        indexValue: APLValue,
        contentValue: APLValue,
        aDimensions: Dimensions): IndexedArrayValue {
        val indexAsList = indexValue.listify()
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
}
