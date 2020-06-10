package array

import kotlinx.coroutines.CoroutineScope

expect fun sleepMillis(time: Long)

interface MPAtomicRefArray<T> {
    operator fun get(index: Int): T?

    fun compareAndExchange(index: Int, expected: T?, newValue: T?): T?
}

expect fun <T> makeAtomicRefArray(size: Int): MPAtomicRefArray<T>

expect fun <T> runBlockingCompat(fn: suspend CoroutineScope.() -> T): T
