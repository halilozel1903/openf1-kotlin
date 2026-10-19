@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package io.github.halilozel1903.openf1

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HttpTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun TestScope.client(
        token: String? = null,
        limit: RateLimit? = null,
        handler: suspend (HttpRequestData) -> Pair<HttpStatusCode, String>,
    ): Pair<OpenF1Client, MutableList<HttpRequestData>> {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            val (status, body) = handler(request)
            val headers = if (status == HttpStatusCode.TooManyRequests) headersOf(HttpHeaders.RetryAfter, "2") else json
            respond(body, status, headers)
        }
        val client = OpenF1Client {
            this.engine = engine
            retries = 0
            rateLimit = limit
            cache = null
            accessToken = token
            clock = { testScheduler.currentTime }
        }
        return client to requests
    }

    @Test
    fun operatorsSurviveKtor() = runTest {
        val (client, requests) = client { HttpStatusCode.OK to "[]" }
        client.laps { sessionKey eq 9839; lapNumber lte 3; lapDuration gt 86.5 }
        client.positions { date gte "2025-12-07T14:20:00+00:00" }
        val first = requests[0].url.toString()
        assertEquals("https://api.openf1.org/v1/laps?session_key=9839&lap_number%3C=3&lap_duration%3E86.5", first)
        assertEquals("https://api.openf1.org/v1/position?date%3E=2025-12-07T14:20:00%2B00:00", requests[1].url.toString())
    }

    @Test
    fun notFoundMeansNoResults() = runTest {
        val (client, _) = client { HttpStatusCode.NotFound to """{"detail":"No results found."}""" }
        assertEquals(emptyList(), client.pitStops { driverNumber eq 999 })
    }

    @Test
    fun unauthorizedForLiveData() = runTest {
        val (client, requests) = client(token = "abc") { HttpStatusCode.Unauthorized to """{"detail":"Live data requires a token"}""" }
        val error = runCatching { client.sessions { sessionKey.latest() } }.exceptionOrNull()
        assertTrue(error is OpenF1Exception.Unauthorized, "was $error")
        assertEquals("Bearer abc", requests.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun serverErrorsKeepTheBody() = runTest {
        val (client, _) = client { HttpStatusCode.UnprocessableEntity to """{"detail":"Too much data"}""" }
        val error = runCatching { client.carData() }.exceptionOrNull()
        assertTrue(error is OpenF1Exception.Http && error.status == 422 && error.body!!.contains("Too much"), "was $error")
    }

    @Test
    fun retriesAfterRateLimiting() = runTest {
        var calls = 0
        val (client, requests) = client {
            calls++
            if (calls < 3) HttpStatusCode.TooManyRequests to "" else HttpStatusCode.OK to "[]"
        }
        val start = testScheduler.currentTime
        assertEquals(emptyList(), client.weather { sessionKey eq 9839 })
        assertEquals(3, requests.size)
        // Retry-After: 2 seconds, twice.
        assertEquals(4_000, testScheduler.currentTime - start)
    }

    @Test
    fun givesUpAfterRateLimitRetries() = runTest {
        val (client, requests) = client { HttpStatusCode.TooManyRequests to "" }
        val error = runCatching { client.weather() }.exceptionOrNull()
        assertTrue(error is OpenF1Exception.RateLimited && error.retryAfterSeconds == 2L, "was $error")
        assertEquals(4, requests.size)
    }

    @Test
    fun requestsAreSpacedToTheRateLimit() = runTest {
        val (client, requests) = client(limit = RateLimit(perSecond = 3, perMinute = 30)) { HttpStatusCode.OK to "[]" }
        val times = mutableListOf<Long>()
        repeat(7) {
            client.meetings { year eq 2023 + it }
            times += testScheduler.currentTime
        }
        assertEquals(7, requests.size)
        assertEquals(listOf(0L, 0L, 0L, 1_000L, 1_000L, 1_000L, 2_000L), times)
    }
}
