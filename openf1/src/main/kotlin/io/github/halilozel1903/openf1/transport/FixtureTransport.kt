package io.github.halilozel1903.openf1.transport

import io.github.halilozel1903.openf1.OpenF1Client

/**
 * Serves OpenF1 responses from local JSON files, for tests, previews and offline demos. [read] gets the endpoint
 * path and the query string of each request and returns the body, or `null` for "no results":
 *
 * ```kotlin
 * val client = OpenF1Client {
 *     transport = FixtureTransport { endpoint, query -> javaClass.getResource("/fixtures/$endpoint.json")?.readText() }
 * }
 * ```
 */
public class FixtureTransport(
    private val baseUrl: String = OpenF1Client.DEFAULT_BASE_URL,
    private val read: (endpoint: String, query: String) -> String?,
) : OpenF1Transport {

    private val lock = Any()
    private val recorded = mutableListOf<String>()

    /** Every url requested so far, in order. */
    public val requests: List<String> get() = synchronized(lock) { recorded.toList() }

    override suspend fun get(url: String): String? {
        synchronized(lock) { recorded += url }
        val relative = url.removePrefix(if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/")
        return read(relative.substringBefore('?').trim('/'), relative.substringAfter('?', ""))
    }
}
