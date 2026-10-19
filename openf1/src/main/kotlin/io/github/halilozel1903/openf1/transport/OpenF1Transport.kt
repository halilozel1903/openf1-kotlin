package io.github.halilozel1903.openf1.transport

/**
 * Fetches the body of an OpenF1 url. The client uses a Ktor based transport by default; supply your own through
 * `OpenF1Config.transport` to serve responses from files ([FixtureTransport]), a database or another HTTP stack.
 *
 * Throw [io.github.halilozel1903.openf1.OpenF1Exception] subclasses for HTTP errors. A transport may return `null`
 * for "no results" (OpenF1 answers 404 then), which the client turns into an empty list. Other exceptions are
 * reported as [io.github.halilozel1903.openf1.OpenF1Exception.Network].
 */
public fun interface OpenF1Transport {
    public suspend fun get(url: String): String?
}
