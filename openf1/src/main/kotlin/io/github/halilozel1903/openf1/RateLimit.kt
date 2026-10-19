package io.github.halilozel1903.openf1

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * How many requests the client sends at most, per second and per minute. OpenF1's free tier allows 3 a second and
 * 30 a minute; requests beyond that wait for their turn instead of failing with HTTP 429.
 */
public data class RateLimit(public val perSecond: Int, public val perMinute: Int) {
    init {
        require(perSecond > 0) { "perSecond must be positive, was $perSecond" }
        require(perMinute > 0) { "perMinute must be positive, was $perMinute" }
    }

    public companion object {
        /** The free tier: 3 requests a second, 30 a minute. */
        public val Free: RateLimit = RateLimit(perSecond = 3, perMinute = 30)

        /** The sponsor tier: 6 requests a second, 60 a minute. */
        public val Sponsor: RateLimit = RateLimit(perSecond = 6, perMinute = 60)
    }
}

/** A sliding window limiter. [clock] returns milliseconds; tests pass virtual time. */
internal class RateLimiter(private val limit: RateLimit, private val clock: () -> Long) {
    private val mutex = Mutex()
    private val sent = ArrayDeque<Long>()

    /** Suspends until one more request fits both windows, then counts it. */
    suspend fun acquire() {
        mutex.withLock {
            while (true) {
                val now = clock()
                while (sent.isNotEmpty() && now - sent.first() >= MINUTE) sent.removeFirst()
                val wait = maxOf(waitFor(now, SECOND, limit.perSecond), waitFor(now, MINUTE, limit.perMinute))
                if (wait <= 0) {
                    sent.addLast(now)
                    return
                }
                delay(wait)
            }
        }
    }

    /** How long until fewer than [max] requests were sent in the last [window] ms. */
    private fun waitFor(now: Long, window: Long, max: Int): Long {
        val inWindow = sent.count { now - it < window }
        if (inWindow < max) return 0
        // The oldest request that has to leave the window first.
        val oldest = sent.filter { now - it < window }[inWindow - max]
        return oldest + window - now
    }

    private companion object {
        const val SECOND = 1_000L
        const val MINUTE = 60_000L
    }
}
