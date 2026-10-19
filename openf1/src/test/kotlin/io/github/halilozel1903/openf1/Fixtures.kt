package io.github.halilozel1903.openf1

import io.github.halilozel1903.openf1.transport.FixtureTransport

/** Reads `src/test/resources/fixtures/<name>.json`. */
internal fun fixture(name: String): String? =
    Fixtures::class.java.getResource("/fixtures/$name.json")?.readText()

internal object Fixtures {
    /** A client that answers each endpoint with its fixture file, without rate limiting or caching. */
    fun client(read: (endpoint: String, query: String) -> String? = { endpoint, _ -> fixture(endpoint) }): Pair<OpenF1Client, FixtureTransport> {
        val transport = FixtureTransport(read = read)
        val client = OpenF1Client {
            this.transport = transport
            rateLimit = null
            cache = null
        }
        return client to transport
    }
}
