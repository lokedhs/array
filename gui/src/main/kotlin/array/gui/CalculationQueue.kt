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
                request.processRequest()
            }
        } catch (e: InterruptedException) {
            println("Closing calculation queue")
        }
    }

    interface Request {
        fun processRequest()
    }

    inner class EvalAPLRequest(
        val source: SourceLocation,
        val linkNewContext: Boolean,
        val callback: (Either<APLValue, Exception>) -> Unit
    ) : Request {
        override fun processRequest() {
            val queueResult = try {
                val result = engine.withThreadLocalAssigned {
                    engine.parseAndEval(source, linkNewContext).collapse()
                }
                Either.Left(result)
            } catch (e: InterruptedException) {
                throw e
            } catch (e: APLGenericException) {
                Either.Right(e)
            } catch (e: Exception) {
                e.printStackTrace()
                Either.Right(WrappedException(e))
            }
            callback(queueResult)
        }
    }

    inner class ReadVariableRequest(val name: String, val callback: (APLValue?) -> Unit) : Request {
        override fun processRequest() {
            val sym = engine.currentNamespace.findSymbol(name, includePrivate = true)
            val result = if (sym == null) {
                null
            } else {
                engine.rootContext.environment.findBinding(sym)?.let { binding ->
                    engine.rootContext.getVar(binding)?.collapse()
                }
            }
            callback(result)
        }
    }

    inner class WriteVariableRequest(val name: String, val value: APLValue, val callback: (Exception?) -> Unit) : Request {
        override fun processRequest() {
            val sym = engine.currentNamespace.internSymbol(name)
            val binding = engine.rootContext.environment.findBinding(sym) ?: engine.rootContext.environment.bindLocal(sym)
            engine.rootContext.reinitRootBindings()
            engine.rootContext.setVar(binding, value)
            callback(null)
        }
    }

    fun pushRequest(source: SourceLocation, linkNewContext: Boolean, fn: (Either<APLValue, Exception>) -> Unit) {
        queue.add(EvalAPLRequest(source, linkNewContext, fn))
    }

    fun pushReadVariableRequest(name: String, callback: (APLValue?) -> Unit) {
        queue.add(ReadVariableRequest(name.trim(), callback))
    }

    fun pushWriteVariableRequest(name: String, value: APLValue, fn: (Exception?) -> Unit) {
        queue.add(WriteVariableRequest(name.trim(), value, fn))
    }

    fun start() {
        thread.start()
    }

    fun stop() {
        thread.interrupt()
        thread.join()
    }
}
