package array

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.nanosleep
import platform.posix.timespec

actual fun sleepMillis(time: Long) {
    memScoped {
        val tms = alloc<timespec>()
        tms.tv_sec = time / 1000
        tms.tv_nsec = time.rem(1000) * 1000L * 1000L
        nanosleep(tms.ptr, null)
    }
}
