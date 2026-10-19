package io.github.halilozel1903.openf1

/** Every failure of [OpenF1Client]. Catch the subclasses you care about, or all of them. */
public sealed class OpenF1Exception(
    /** The requested url. */
    public val url: String,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /**
     * OpenF1 answered 401 or 403. Data of a session that is running (from 30 minutes before it starts to 30 minutes
     * after it ends) needs a sponsor token: set [OpenF1Config.accessToken]. Historical data needs none.
     */
    public class Unauthorized(url: String, public val status: Int, public val body: String?) :
        OpenF1Exception(url, "HTTP $status for $url: live data needs an OpenF1 sponsor token${body?.let { " ($it)" } ?: ""}")

    /** OpenF1 answered 429: too many requests. [retryAfterSeconds] comes from the `Retry-After` header, when sent. */
    public class RateLimited(url: String, public val retryAfterSeconds: Long?) :
        OpenF1Exception(url, "Rate limited by OpenF1 for $url")

    /** OpenF1 answered with another unsuccessful [status], such as 422 for a query that matches too much data. */
    public class Http(url: String, public val status: Int, public val body: String?) :
        OpenF1Exception(url, "HTTP $status for $url${body?.let { ": ${it.take(300)}" } ?: ""}")

    /** The request failed before a response arrived (offline, timeout, DNS). */
    public class Network(url: String, cause: Throwable) :
        OpenF1Exception(url, "Request failed for $url: ${cause.message ?: cause::class.simpleName}", cause)

    /** The response was not the JSON this library expects. */
    public class Decoding(url: String, cause: Throwable) :
        OpenF1Exception(url, "Could not decode the response of $url: ${cause.message}", cause)
}
