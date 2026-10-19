@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package io.github.halilozel1903.openf1

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RateLimiterTest {
    @Test
    fun perMinuteLimitWaitsForTheOldestRequest() = runTest {
        val limiter = RateLimiter(RateLimit(perSecond = 10, perMinute = 4)) { testScheduler.currentTime }
        val times = mutableListOf<Long>()
        repeat(6) {
            limiter.acquire()
            times += testScheduler.currentTime
        }
        assertEquals(listOf(0L, 0L, 0L, 0L, 60_000L, 60_000L), times)
    }

    @Test
    fun freeTierAllowsThirtyAMinute() = runTest {
        val limiter = RateLimiter(RateLimit.Free) { testScheduler.currentTime }
        repeat(30) { limiter.acquire() }
        assertEquals(9_000L, testScheduler.currentTime)
        limiter.acquire()
        assertEquals(60_000L, testScheduler.currentTime)
    }

    @Test
    fun rejectsNonPositiveLimits() {
        assertFailsWith<IllegalArgumentException> { RateLimit(0, 10) }
    }
}
