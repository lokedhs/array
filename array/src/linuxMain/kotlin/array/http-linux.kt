package array

import kotlinx.cinterop.*
import libcurl.*
import platform.posix.size_t

class HttpResultLinux(
    override val code: Long,
    override val content: String
) : HttpResult

class CurlTask {
    val buf = StringBuilder()

    fun httpRequest(url: String, headers: Map<String, String>?): HttpResultLinux {
        return httpPost(url, null, headers)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun httpPost(url: String, postContent: ByteArray?, headers: Map<String, String>?): HttpResultLinux {
        val stableRef = StableRef.create(this)
        val curl = curl_easy_init() ?: throw IllegalStateException("Initialisation error in libcurl")
        try {
            memScoped {
                curl_easy_setopt(curl, CURLOPT_URL, url)
                curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L)
                curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, staticCFunction(::handleWrite))
                curl_easy_setopt(curl, CURLOPT_WRITEDATA, stableRef.asCPointer())
                if (postContent != null) {
                    val buf = allocArray<ByteVarOf<Byte>>(postContent.size)
                    for (i in postContent.indices) {
                        buf[i] = postContent[i]
                    }
                    curl_easy_setopt(curl, CURLOPT_POSTFIELDS, buf)
                    curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, postContent.size.toLong())
                }
                val headerList = initHeaders(headers)
                try {
                    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headerList)
                    val result = curl_easy_perform(curl)
                    if (result == CURLE_OK) {
                        return processResult(curl)
                    } else {
                        throw Exception("Error handling not implemented")
                    }
                } finally {
                    curl_slist_free_all(headerList)
                }
            }
        } finally {
            curl_easy_cleanup(curl)
            stableRef.dispose()
        }

    }

    private fun initHeaders(headers: Map<String, String>?): CPointer<curl_slist>? {
        var list: CPointer<curl_slist>? = null
        if (headers != null) {
            headers.entries.forEach { (key, value) ->
                list = curl_slist_append(list, "${key}: ${value}")
            }
        }
        return list
    }

    private fun escapeString(curl: COpaquePointer, s: String): String {
        val result = curl_easy_escape(curl, s, 0)
        try {
            if (result == null) {
                throw IllegalStateException("Unable to escape string")
            }
            return result.toKString()
        } finally {
            curl_free(result)
        }
    }

    private fun processResult(curl: COpaquePointer): HttpResultLinux {
        memScoped {
            val codeResult = alloc<LongVarOf<Long>>()
            curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, codeResult)
            return HttpResultLinux(codeResult.value, buf.toString())
        }
    }
}

actual fun httpRequest(url: String, headers: Map<String, String>?): HttpResult {
    val task = CurlTask()
    return task.httpRequest(url, headers)
}

actual fun httpPost(url: String, content: ByteArray, headers: Map<String, String>?): HttpResult {
    val task = CurlTask()
    return task.httpPost(url, content, headers)
}

@OptIn(ExperimentalUnsignedTypes::class)
private fun handleWrite(buffer: CPointer<ByteVar>?, size: size_t, nitems: size_t, userdata: COpaquePointer?): size_t {
    if (buffer == null) return 0u
    if (userdata != null) {
        val bytesResult = buffer.readBytes((size * nitems).toInt())
        val data = bytesResult.toKString()
        val curlTask = userdata.asStableRef<CurlTask>().get()
        curlTask.buf.append(data)
    }
    return size * nitems
}
