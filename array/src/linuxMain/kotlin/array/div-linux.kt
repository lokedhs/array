package array

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.*
import kotlin.native.concurrent.FreezableAtomicReference
import kotlin.native.concurrent.freeze
import kotlin.native.concurrent.isFrozen
import kotlin.reflect.KClass

actual fun sleepMillis(time: Long) {
    memScoped {
        val tms = alloc<timespec>()
        tms.tv_sec = time / 1000
        tms.tv_nsec = time.rem(1000) * 1000L * 1000L
        nanosleep(tms.ptr, null)
    }
}

class LinuxMPAtomicRefArray<T>(size: Int) : MPAtomicRefArray<T> {
    private val content = ArrayList<FreezableAtomicReference<T?>>(size)

    init {
        repeat(size) { content.add(FreezableAtomicReference(null)) }
    }

    override operator fun get(index: Int): T? = content[index].value

    override fun compareAndExchange(index: Int, expected: T?, newValue: T?): T? {
        val reference = content[index]
        if (reference.isFrozen) {
            newValue.freeze()
        }
        return reference.compareAndSwap(expected, newValue)
    }
}

actual fun <T> makeAtomicRefArray(size: Int): MPAtomicRefArray<T> {
    return LinuxMPAtomicRefArray(size)
}

actual fun <T : Any> makeMPThreadLocal(type: KClass<T>): MPThreadLocal<T> {
    // Use the single-threaded implementation as the native version doesn't support multi-threading yet
    return object : MPThreadLocal<T> {
        override var value: T? = null
    }
}

actual fun Double.formatDouble() = this.toString()

actual fun currentTime(): Long {
    memScoped {
        val value = alloc<timeval>()
        if (gettimeofday(value.ptr, null) != 0) {
            throw RuntimeException("Error getting time: ${strerror(errno)}")
        }
        return (value.tv_sec * 1000) + (value.tv_usec / 1000)
    }
}

actual fun toRegexpWithException(string: String, options: Set<RegexOption>): Regex {
    return try {
        string.toRegex(options)
    } catch (e: Exception) {
        throw RegexpParseException("Error parsing regexp: \"${string}\"", e)
    }
}
