package io.github.halilozel1903.openf1

import io.github.halilozel1903.openf1.query.LapsQuery
import io.github.halilozel1903.openf1.query.Operator
import io.github.halilozel1903.openf1.query.PositionQuery
import io.github.halilozel1903.openf1.query.SessionsQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class QueryTest {
    private val client = OpenF1Client { rateLimit = null }

    @Test
    fun equalityAndComparisonOperators() {
        val query = LapsQuery().apply {
            sessionKey eq 9839
            driverNumber eq 4
            lapNumber lte 3
            lapDuration gt 86.5
        }
        assertEquals(
            "https://api.openf1.org/v1/laps?session_key=9839&driver_number=4&lap_number%3C=3&lap_duration%3E86.5",
            client.urlFor(OpenF1Endpoint.Laps, query),
        )
        assertEquals("lap_number<=3", query.filters[2].toString())
    }

    @Test
    fun rangesAndMultipleValues() {
        val query = LapsQuery().apply {
            driverNumber.oneOf(1, 4, 81)
            lapNumber inRange 10..20
            lapDuration.between(86.0, 90.0)
        }
        assertEquals(
            "driver_number=1&driver_number=4&driver_number=81&lap_number%3E=10&lap_number%3C=20" +
                "&lap_duration%3E=86&lap_duration%3C=90",
            client.urlFor(OpenF1Endpoint.Laps, query).substringAfter('?'),
        )
    }

    @Test
    fun datesAreEncoded() {
        val query = PositionQuery().apply {
            date gte "2025-12-07T14:20:00+00:00"
            date lt Instant.parse("2025-12-07T15:00:00Z")
        }
        assertEquals(
            "date%3E=2025-12-07T14:20:00%2B00:00&date%3C2025-12-07T15:00:00Z",
            client.urlFor(OpenF1Endpoint.Position, query).substringAfter('?'),
        )
    }

    @Test
    fun textLatestAndRawFilters() {
        val query = SessionsQuery().apply {
            meetingKey.latest()
            sessionName eq "Practice 1"
            countryName eq "São Paulo & Co"
            raw("circuit_key", Operator.GreaterThanOrEqual, "7")
        }
        assertEquals(
            "meeting_key=latest&session_name=Practice%201&country_name=S%C3%A3o%20Paulo%20%26%20Co&circuit_key%3E=7",
            client.urlFor(OpenF1Endpoint.Sessions, query).substringAfter('?'),
        )
    }

    @Test
    fun noFiltersAndCsv() {
        assertEquals("https://api.openf1.org/v1/meetings", client.urlFor(OpenF1Endpoint.Meetings, emptyList()))
        val filters = SessionsQuery().apply { year eq 2025 }.filters
        assertEquals(
            "https://api.openf1.org/v1/sessions?year=2025&csv=true",
            client.urlFor(OpenF1Endpoint.Sessions, filters, csv = true),
        )
    }

    @Test
    fun customBaseUrlGetsASlash() {
        val custom = OpenF1Client { baseUrl = "http://localhost:8000/v1"; rateLimit = null }
        assertEquals("http://localhost:8000/v1/pit", custom.urlFor(OpenF1Endpoint.Pit, emptyList()))
    }
}
