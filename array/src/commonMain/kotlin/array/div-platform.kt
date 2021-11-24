package array

import kotlin.reflect.KClass

expect fun sleepMillis(time: Long)

interface MPAtomicRefArray<T> {
    operator fun get(index: Int): T?

    fun compareAndExchange(index: Int, expected: T?, newValue: T?): T?

    @Suppress("IfThenToElvis")
    fun checkOrUpdate(index: Int, fn: () -> T): T {
        val old = get(index)
        if (old != null) {
            return old
        }
        val update = fn()
        val v = compareAndExchange(index, null, update)
        return if (v == null) update else v
    }
}

expect fun <T> makeAtomicRefArray(size: Int): MPAtomicRefArray<T>

interface MPThreadLocal<T> {
    var value: T?
}

expect fun <T : Any> makeMPThreadLocalBackend(type: KClass<T>): MPThreadLocal<T>

inline fun <reified T : Any> makeMPThreadLocal(): MPThreadLocal<T> {
    return makeMPThreadLocalBackend(T::class)
}

/**
 * Format a double in a standardised way. A value with zero decimal part should be rendered as 4.0.
 * This is needed because Javascript does not include the decimal by default.
 */
expect fun Double.formatDouble(): String

/**
 * Return the current time in number of milliseconds
 */
expect fun currentTime(): Long

class RegexpParseException(message: String, cause: Throwable) : Exception(message, cause)

expect fun toRegexpWithException(string: String, options: Set<RegexOption>): Regex

expect fun numCores(): Int

interface BackgroundTask<T> {
    fun await(): T
}

//expect fun <T> BackgroundTask<T>.make(fn: () -> T): BackgroundTask<T>

interface MPThreadPoolExecutor {
    val numThreads: Int
    fun <T> start(fn: () -> T): BackgroundTask<T>
}

class SingleThreadedThreadPoolExecutor : MPThreadPoolExecutor {
    override val numThreads get() = 1

    override fun <T> start(fn: () -> T): BackgroundTask<T> {
        return object : BackgroundTask<T> {
            override fun await(): T {
                return fn()
            }
        }
    }
}

expect fun makeBackgroundDispatcher(numThreads: Int): MPThreadPoolExecutor
