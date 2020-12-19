package array

interface HttpResult {
    val code: Long
    val content: String
}

expect fun httpRequest(url: String, headers: Map<String, String>? = null): HttpResult
