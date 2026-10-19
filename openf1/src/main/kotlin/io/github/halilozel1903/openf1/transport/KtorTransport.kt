package io.github.halilozel1903.openf1.transport

import io.github.halilozel1903.openf1.OpenF1Exception
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException

/** The default transport: GET requests with Ktor. Closes the client only when it created it. */
internal class KtorTransport(
    httpClient: HttpClient?,
    engine: HttpClientEngine?,
    private val userAgent: String,
    private val token: (suspend () -> String?)?,
    requestTimeoutMillis: Long,
    retries: Int,
) : OpenF1Transport, AutoCloseable {

    private val ownsClient = httpClient == null
    private val client: HttpClient = httpClient ?: run {
        val setup: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
            expectSuccess = false
            install(HttpTimeout) {
                this.requestTimeoutMillis = requestTimeoutMillis
            }
            if (retries > 0) {
                install(HttpRequestRetry) {
                    retryOnServerErrors(maxRetries = retries)
                    retryOnException(maxRetries = retries, retryOnTimeout = true)
                    exponentialDelay()
                }
            }
        }
        if (engine != null) HttpClient(engine, setup) else HttpClient(CIO, setup)
    }

    override suspend fun get(url: String): String? {
        try {
            val bearer = token?.invoke()
            val response = client.get(url) {
                header(HttpHeaders.UserAgent, userAgent)
                accept(ContentType.Application.Json)
                if (!bearer.isNullOrBlank()) header(HttpHeaders.Authorization, "Bearer $bearer")
            }
            val status = response.status
            return when {
                status.isSuccess() -> response.bodyAsText()
                // OpenF1 answers 404 with {"detail": "No results found."} when nothing matches the filters.
                status == HttpStatusCode.NotFound -> null
                status == HttpStatusCode.Unauthorized || status == HttpStatusCode.Forbidden ->
                    throw OpenF1Exception.Unauthorized(url, status.value, response.safeBody())
                status == HttpStatusCode.TooManyRequests ->
                    throw OpenF1Exception.RateLimited(url, response.headers[HttpHeaders.RetryAfter]?.trim()?.toLongOrNull())
                else -> throw OpenF1Exception.Http(url, status.value, response.safeBody())
            }
        } catch (e: OpenF1Exception) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw OpenF1Exception.Network(url, e)
        }
    }

    private suspend fun io.ktor.client.statement.HttpResponse.safeBody(): String? =
        runCatching { bodyAsText().take(500) }.getOrNull()?.takeIf { it.isNotBlank() }

    override fun close() {
        if (ownsClient) client.close()
    }
}
