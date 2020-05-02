package array

enum class HttpMethod(val methodName: String) {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    HEAD("HEAD"),
}

interface HttpResult {
    val code: Long
    val content: String
}

expect fun httpRequest(url: String, method: HttpMethod = HttpMethod.GET, headers: Map<String, String>? = null): HttpResult
