package array

import kotlin.math.ceil
import kotlin.math.min

interface ParallelTaskResult

interface ParallelTask {
    fun computeResult(context: RuntimeContext): Either<ParallelTaskResult, Throwable>
}

abstract class ParallelTaskList {
    val tasks = ArrayList<ParallelTask>()
    abstract fun finaliseCompute(results: ArrayList<Either<ParallelTaskResult, Throwable>>): APLValue
}

interface ParallelSupported {
    fun computeParallelTasks1Arg(context: RuntimeContext, numTasks: Int, a: APLValue, axis: APLValue?): ParallelTaskList
    fun computeParallelTasks2Arg(workUnitSize: Int): ParallelTaskList
}

// Test code:
// {io:println "start:",⍕⍵ ◊ time:sleep 2 ◊ io:println "end:",⍕⍵ ◊ ⍵+100}¨ ⍳10

/**
 * Implementation of parallel task that does not perform parallelisation.
 */
class SimpleParallelTaskList1Arg(val fn: APLFunction, val a: APLValue, val axis: APLValue?) : ParallelTaskList() {
    init {
        tasks.add(SimpleParallelTask())
    }

    override fun finaliseCompute(results: ArrayList<Either<ParallelTaskResult, Throwable>>): APLValue {
        assertx(results.size == 1)
        val res = results[0]
        when (res) {
            is Either.Left -> return (res.value as SimpleParallelTaskResult).result
            is Either.Right -> throw res.value
        }
    }

    private inner class SimpleParallelTask : ParallelTask {
        override fun computeResult(context: RuntimeContext): Either<ParallelTaskResult, Throwable> {
            return try {
                val res = fn.eval1Arg(context, a, axis)
                Either.Left(SimpleParallelTaskResult(res))
            } catch (e: Exception) {
                Either.Right(e)
            }
        }
    }

    private class SimpleParallelTaskResult(val result: APLValue) : ParallelTaskResult
}

class ConstantParallelTaskList(val value: APLValue) : ParallelTaskList() {
    override fun finaliseCompute(results: ArrayList<Either<ParallelTaskResult, Throwable>>): APLValue {
        return value
    }
}

class ParallelCompressTaskList(val value: APLValue, numTasks: Int) : ParallelTaskList() {
    private val labels = value.labels
    private val dimensions = value.dimensions
    private val size = dimensions.contentSize()

    init {
        val r = size % numTasks
        val unitSize = size / numTasks
        var start = 0
        repeat(min(numTasks, size)) { i ->
            val n = if (i < r) unitSize + 1 else unitSize
            tasks.add(ParallelCompressTask(start, start + n))
            start += n
        }
    }

    override fun finaliseCompute(results: ArrayList<Either<ParallelTaskResult, Throwable>>): APLValue {
        val list = ArrayList<APLValue>()
        results.forEach { v ->
            when (v) {
                is Either.Left -> list.addAll((v.value as ParallelCompressResult).result)
                is Either.Right -> throw v.value // TODO: We only throw the first found exception
            }
        }
        val result = CollapsedArrayImpl(dimensions, list.toTypedArray())
        return if (labels == null) {
            result
        } else {
            LabelledArray(result, labels)
        }
    }

    private inner class ParallelCompressTask(val start: Int, val end: Int) : ParallelTask {
        override fun computeResult(context: RuntimeContext): Either<ParallelTaskResult, Throwable> {
            return try {
                val res = ArrayList<APLValue>()
                for (i in start until end) {
                    val v = value.valueAt(i).collapseInt()
                    res.add(v)
                }
                Either.Left(ParallelCompressResult(res))
            } catch (e: Exception) {
                Either.Right(e)
            }
        }
    }

    private inner class ParallelCompressResult(val result: List<APLValue>) : ParallelTaskResult

    companion object {
        fun make(value: APLValue, numTasks: Int): ParallelTaskList {
            return if (value.dimensions.size == 0) {
                return ConstantParallelTaskList(value)
            } else {
                ParallelCompressTaskList(value, numTasks)
            }
        }
    }
}

class ParallelOp : APLOperatorOneArg {
    override fun combineFunction(fn: APLFunction, operatorAxis: Instruction?, pos: Position): APLFunctionDescriptor {
        if (fn !is ParallelSupported) {
            throw ParallelNotSupported(pos)
        }
        return ParallelHandler(fn)
    }
}

private class ParallelHandler(val derived: ParallelSupported, val numTasksWeightFactor: Double = 10.0) : APLFunctionDescriptor {
    inner class ParallelHandlerImpl(pos: Position) : APLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
            val parallelTaskList = derived.computeParallelTasks1Arg(
                context,
                ceil(context.engine.backgroundDispatcher.numThreads * numTasksWeightFactor).toInt(),
                a, axis)
            val dispatcher = context.engine.backgroundDispatcher
            val tasks = parallelTaskList.tasks.map { task ->
                dispatcher.start {
                    context.engine.withThreadLocalAssigned {
                        task.computeResult(context)
                    }
                }
            }
            val results = ArrayList<Either<ParallelTaskResult, Throwable>>()
            tasks.forEach { task ->
                val res = task.await()
                results.add(res)
            }
            return parallelTaskList.finaliseCompute(results)
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
            TODO("Will be implemented later")
        }
    }

    override fun make(pos: Position) = ParallelHandlerImpl(pos)
}
