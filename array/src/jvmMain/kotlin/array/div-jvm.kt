package array

import java.util.concurrent.atomic.AtomicReferenceArray

actual fun sleepMillis(time: Long) {
    Thread.sleep(time)
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
