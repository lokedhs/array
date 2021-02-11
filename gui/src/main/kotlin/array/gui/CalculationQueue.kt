package array.gui

import array.APLValue
import array.Either
import array.Engine
import array.SourceLocation
import java.util.concurrent.LinkedTransferQueue
import java.util.concurrent.TransferQueue

class CalculationQueue(val engine: Engine) {
    private val queue: TransferQueue<Request> = LinkedTransferQueue()
    private val thread = Thread { computeLoop() }

    private fun computeLoop() {
        try {
            while (!Thread.interrupted()) {
                val request = queue.take()
                val queueResult = try {
                    val result = engine.parseAndEval(request.source, request.linkNewContext).collapse()
                    Either.Left(result)
                } catch (e: Exception) {
                    Either.Right(e)
                }
                request.callback(queueResult)
            }
        } catch (e: InterruptedException) {
            println("Closing calculation queue")
        }
    }

    private class Request(
        val source: SourceLocation,
        val linkNewContext: Boolean,
        val callback: (Either<APLValue, Exception>) -> Unit
    )

    fun pushRequest(source: SourceLocation, linkNewContext: Boolean, fn: (Either<APLValue, Exception>) -> Unit) {
        queue.add(Request(source, linkNewContext, fn))
    }

    fun start() {
        thread.start()
    }

    fun stop() {
        thread.interrupt()
        thread.join()
    }
}
