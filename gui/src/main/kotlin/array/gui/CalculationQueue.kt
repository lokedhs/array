package array.gui

import array.*
import java.util.concurrent.LinkedTransferQueue
import java.util.concurrent.TransferQueue

class WrappedException(cause: Throwable) : APLGenericException("JVM exception while evaluating expression: ${cause.message}", null, cause)

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
                } catch (e: InterruptedException) {
                    throw e
                } catch (e: APLGenericException) {
                    Either.Right(e)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Either.Right(WrappedException(e))
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
