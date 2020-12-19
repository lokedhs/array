package array

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class HttpResultJvm(
    override val code: Long,
    override val content: String
) : HttpResult

private fun makeHttpClient(): HttpClient {
    return HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
}

actual fun httpRequest(url: String, headers: Map<String, String>?): HttpResult {
    val httpRequest = HttpRequest.newBuilder()
        .uri(URI(url))
        .timeout(Duration.ofMinutes(1))
        .GET()
        .also { m ->
            headers?.forEach { (k, v) ->
                m.header(k, v)
            }
        }.build()
    val result = makeHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString())
    return HttpResultJvm(result.statusCode().toLong(), result.body())
}

actual fun httpPost(url: String, content: ByteArray, headers: Map<String, String>?): HttpResult {
    val httpRequest = HttpRequest.newBuilder()
        .uri(URI(url))
        .timeout(Duration.ofMinutes(1))
        .POST(HttpRequest.BodyPublishers.ofByteArray(content))
        .also { m ->
            headers?.forEach { (k, v) ->
                m.header(k, v)
            }
        }.build()
    val result = makeHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString())
    return HttpResultJvm(result.statusCode().toLong(), result.body())
}
