package array

import kotlinx.cinterop.*
import platform.posix.size_t

class HttpResultLinux(
    override val code: Long,
    override val content: String
) : HttpResult

class CurlTask {
    val buf = StringBuilder()

    @OptIn(ExperimentalUnsignedTypes::class)
    fun httpRequest(url: String): HttpResultLinux {
        val stableRef = StableRef.create(this)
        val curl = libcurl.curl_easy_init()
        try {
            libcurl.curl_easy_setopt(curl, libcurl.CURLOPT_URL, url)
            libcurl.curl_easy_setopt(curl, libcurl.CURLOPT_FOLLOWLOCATION, 1L)
            libcurl.curl_easy_setopt(curl, libcurl.CURLOPT_WRITEFUNCTION, staticCFunction(::handleWrite))
            libcurl.curl_easy_setopt(curl, libcurl.CURLOPT_WRITEDATA, stableRef.asCPointer())
            val result = libcurl.curl_easy_perform(curl)
            if (result == libcurl.CURLE_OK) {
                return processResult(curl)
            } else {
                throw Exception("Error handling not implemented")
            }
        } finally {
            libcurl.curl_easy_cleanup(curl)
            stableRef.dispose()
        }
    }

    private fun processResult(curl: CPointer<out CPointed>?): HttpResultLinux {
        memScoped {
            val codeResult = alloc(sizeOf<LongVar>(), 16)
            libcurl.curl_easy_getinfo(curl, libcurl.CURLINFO_RESPONSE_CODE, codeResult)
            return HttpResultLinux(codeResult.reinterpret<LongVar>().value, buf.toString())
        }
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
actual fun httpRequest(url: String, method: HttpMethod, headers: Map<String, String>?): HttpResult {
    val task = CurlTask()
    return task.httpRequest(url)
}

@OptIn(ExperimentalUnsignedTypes::class)
private fun handleWrite(buffer: CPointer<ByteVar>?, size: size_t, nitems: size_t, userdata: COpaquePointer?): size_t {
    if (buffer == null) return 0u
    if (userdata != null) {
        val bytesResult = buffer.readBytes((size * nitems).toInt())
        val data = bytesResult.decodeToString()
        val curlTask = userdata.asStableRef<CurlTask>().get()
        curlTask.buf.append(data)
    }
    return size * nitems
}
