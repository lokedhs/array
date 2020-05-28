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

actual fun httpRequest(url: String, method: HttpMethod, headers: Map<String, String>?): HttpResult {
    val httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
    val httpRequest = HttpRequest.newBuilder()
        .uri(URI(url))
        .timeout(Duration.ofMinutes(1))
        .GET()
        .build()
    val result = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
    return HttpResultJvm(result.statusCode().toLong(), result.body())
}
