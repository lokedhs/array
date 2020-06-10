package array

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicReferenceArray

actual fun sleepMillis(time: Long) {
    runBlocking {
        delay(time)
    }
}

class JvmMPAtomicRefArray<T>(size: Int) : MPAtomicRefArray<T> {
    private val content = AtomicReferenceArray<T>(size)

    override operator fun get(index: Int): T? = content[index]

    override fun compareAndExchange(index: Int, expected: T?, newValue: T?): T? {
        return content.compareAndExchange(index, expected, newValue)
    }
}

actual fun <T> makeAtomicRefArray(size: Int): MPAtomicRefArray<T> {
    return JvmMPAtomicRefArray(size)
}

actual fun <T> runBlockingCompat(fn: suspend CoroutineScope.() -> T): T {
    //return runBlocking(EmptyCoroutineContext, fn)
    return runBlocking { fn() }
}
