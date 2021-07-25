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

expect fun <T : Any> makeMPThreadLocal(type: KClass<T>): MPThreadLocal<T>

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
expect fun indexesFromRegexpMatchGroup(group: MatchGroup): Pair<Int, Int>
