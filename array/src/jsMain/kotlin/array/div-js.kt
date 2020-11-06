package array

import kotlin.js.Date

actual fun sleepMillis(time: Long) {
    TODO("not implemented")
}

class JsAtomicRefArray<T>(size: Int) : MPAtomicRefArray<T> {
    private val content: MutableList<T?>

    init {
        content = ArrayList<T?>()
        repeat(size) {
            content.add(null)
        }
    }

    override fun get(index: Int) = content[index]

    override fun compareAndExchange(index: Int, expected: T?, newValue: T?): T? {
        val v = content[index]
        if (v == expected) {
            content[index] = newValue
        }
        return v
    }
}

actual fun <T> makeAtomicRefArray(size: Int): MPAtomicRefArray<T> {
    return JsAtomicRefArray(size)
}

actual fun Double.formatDouble(): String {
    return if (this.rem(1.0) == 0.0) {
        "${this}.0"
    } else {
        this.toString()
    }
}

actual fun currentTime(): Long {
    return Date.now().toLong()
}
