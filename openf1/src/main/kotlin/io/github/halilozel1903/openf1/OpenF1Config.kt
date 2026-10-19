package io.github.halilozel1903.openf1

import io.github.halilozel1903.openf1.transport.OpenF1Transport
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine

/** Settings for [OpenF1Client], set in its builder block. */
public class OpenF1Config internal constructor() {
    /** The API root. */
    public var baseUrl: String = OpenF1Client.DEFAULT_BASE_URL

    /**
     * The Ktor engine. `null` uses CIO. Pass `OkHttp.create()` (add `io.ktor:ktor-client-okhttp`) to share your
     * app's OkHttp stack, or a `MockEngine` in tests. The client does not close an engine you pass.
     */
    public var engine: HttpClientEngine? = null

    /** A ready Ktor client to use as is, instead of [engine]. [requestTimeoutMillis] and [retries] then do not apply. */
    public var httpClient: HttpClient? = null

    /** Replaces HTTP entirely, for example with a [io.github.halilozel1903.openf1.transport.FixtureTransport]. */
    public var transport: OpenF1Transport? = null

    /**
     * An OpenF1 sponsor access token, sent as `Authorization: Bearer <token>`. Only needed for sessions that are
     * running; historical data (2023 onwards) is free without one. See [tokenProvider] for tokens that expire.
     */
    public var accessToken: String? = null

    /** Returns the token for each request, for example after refreshing it. Overrides [accessToken]. */
    public var tokenProvider: (suspend () -> String?)? = null

    /** Requests are spaced to stay within this limit. [RateLimit.Free] by default; `null` turns it off. */
    public var rateLimit: RateLimit? = RateLimit.Free

    /** Where responses are cached. An in-memory cache by default; `null` turns caching off. */
    public var cache: OpenF1Cache? = OpenF1Cache.memory()

    /** Sent with every request. */
    public var userAgent: String = "openf1-kotlin/1.0.0 (+https://github.com/halilozel1903/openf1-kotlin)"

    /** Timeout of one request, in milliseconds. Large queries (all laps of a race) can take a few seconds. */
    public var requestTimeoutMillis: Long = 60_000

    /** Retries on network errors and 5xx responses, with exponential backoff. */
    public var retries: Int = 2

    /** Retries after HTTP 429, waiting for `Retry-After` or an increasing delay. */
    public var rateLimitRetries: Int = 3

    /** Milliseconds since some fixed point; tests replace it with virtual time. */
    internal var clock: () -> Long = { System.nanoTime() / 1_000_000 }
}
