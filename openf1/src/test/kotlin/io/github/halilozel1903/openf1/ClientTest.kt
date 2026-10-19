package io.github.halilozel1903.openf1

import io.github.halilozel1903.openf1.model.Gap
import io.github.halilozel1903.openf1.model.TyreCompound
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClientTest {

    @Test
    fun meetingsAndSessions() = runTest {
        val (client, transport) = Fixtures.client()
        val meeting = client.meetings { year eq 2025 }.single()
        assertEquals("Abu Dhabi Grand Prix", meeting.meetingName)
        assertEquals("Permanent", meeting.circuitType)
        assertEquals(1764927000L, meeting.start?.epochSeconds)
        assertEquals("https://api.openf1.org/v1/meetings?year=2025", transport.requests.single())

        val sessions = client.sessions { sessionName eq "Race" }
        assertEquals(listOf(9839, 9850), sessions.map { it.sessionKey })
    }

    @Test
    fun driversWithTeamColours() = runTest {
        val (client, _) = Fixtures.client()
        val drivers = client.drivers { sessionKey eq 9839 }
        val norris = drivers.first()
        assertEquals("Lando Norris", norris.displayName)
        assertEquals(0xFFF47600, norris.teamColorArgb)
        assertNull(drivers[1].headshotUrl)
        assertNull(norris.countryCode)
    }

    @Test
    fun completeHeadshotsUsesOtherSessions() = runTest {
        val (client, transport) = Fixtures.client { endpoint, query ->
            if ("headshot_url" in query) fixture("drivers-headshots") else fixture(endpoint)
        }
        val drivers = client.completeHeadshots(client.drivers { sessionKey eq 9839 })
        assertTrue(drivers.all { it.headshotUrl != null })
        assertTrue(drivers[1].headshotUrl!!.contains("fracol01"))
        assertEquals(
            "https://api.openf1.org/v1/drivers?driver_number=43&headshot_url%3E=http",
            transport.requests.last(),
        )
    }

    @Test
    fun completeHeadshotsSkipsTheRequestWhenNothingIsMissing() = runTest {
        val (client, transport) = Fixtures.client()
        val complete = client.drivers().take(1)
        assertEquals(complete, client.completeHeadshots(complete))
        assertEquals(1, transport.requests.size)
    }

    @Test
    fun lapsWithNulls() = runTest {
        val (client, _) = Fixtures.client()
        val laps = client.laps { sessionKey eq 9839; driverNumber eq 4 }
        assertEquals(5, laps.size)
        assertNull(laps[1].i1Speed)
        assertEquals(listOf(null, 2049, 2049, 2049, 2049), laps[1].segmentsSector1)
        assertTrue(laps[2].isPitOutLap)
        assertNull(laps[4].lapDuration)
        assertEquals(1765112607L, laps[0].start?.epochSeconds)
    }

    @Test
    fun intervalsReadNumbersAndLappedText() = runTest {
        val (client, _) = Fixtures.client()
        val intervals = client.intervals { sessionKey eq 9165 }
        assertNull(intervals[0].gapToLeader)
        assertEquals(Gap.Seconds(41.019), intervals[1].gapToLeader)
        assertEquals(Gap.Seconds(0.003), intervals[1].interval)
        assertEquals(Gap.Laps(1), intervals[2].gapToLeader)
        assertEquals(Gap.Seconds(2.104), intervals[2].interval)
        assertEquals("+1 LAP", intervals[2].gapToLeader.toString())
    }

    @Test
    fun pitStopsStintsWeather() = runTest {
        val (client, _) = Fixtures.client()
        val stops = client.pitStops { sessionKey eq 9839 }
        assertNull(stops[0].stopDuration)
        assertEquals(2.1, stops[1].stopDuration)
        assertEquals(24.1, stops[3].pitLaneTime)

        val stints = client.stints()
        assertEquals(TyreCompound.Hard, stints[0].tyre)
        assertEquals(24, stints[0].laps)
        assertEquals(TyreCompound.Unknown, stints[3].tyre)
        assertNull(stints[3].laps)

        val weather = client.weather()
        assertEquals(58.0, weather[0].humidity)
        assertFalse(weather[0].isRaining)
    }

    @Test
    fun sessionResultsInRaceAndQualifying() = runTest {
        val (client, _) = Fixtures.client { _, query ->
            if ("session_key=9838" in query) fixture("session_result-qualifying") else fixture("session_result-race")
        }
        val race = client.sessionResults { sessionKey eq 9839 }
        assertEquals(listOf<Double?>(5197.123), race[0].duration)
        assertEquals(listOf<Gap?>(Gap.Seconds(0.0)), race[0].gapToLeader)
        assertEquals(emptyList(), race[1].duration)
        assertEquals(listOf<Gap?>(Gap.Laps(1)), race[1].gapToLeader)

        val qualifying = client.sessionResults { sessionKey eq 9838 }
        assertEquals(listOf<Double?>(83.122, 82.701, 82.207), qualifying[0].duration)
        assertEquals(listOf<Gap?>(Gap.Seconds(0.778), null, null), qualifying[1].gapToLeader)
    }

    @Test
    fun otherEndpoints() = runTest {
        val (client, _) = Fixtures.client()
        assertEquals("GREEN", client.raceControl().single().flag)
        assertTrue(client.teamRadio().single().recordingUrl.endsWith(".mp3"))
        val sample = client.carData { driverNumber eq 4; speed gte 300 }.single()
        assertEquals(315, sample.speed)
        assertTrue(sample.isDrsOpen)
    }

    @Test
    fun noResultsIsAnEmptyList() = runTest {
        val (client, _) = Fixtures.client { _, _ -> null }
        assertEquals(emptyList(), client.pitStops { driverNumber eq 999 })
        assertEquals("", client.csv(OpenF1Endpoint.Pit))
    }

    @Test
    fun malformedJsonIsADecodingError() = runTest {
        val (client, _) = Fixtures.client { _, _ -> """{"detail":"oops"}""" }
        val error = runCatching { client.laps() }.exceptionOrNull()
        assertTrue(error is OpenF1Exception.Decoding, "was $error")
    }

    @Test
    fun latestCompletedSession() = runTest {
        val (client, transport) = Fixtures.client()
        val session = client.latestCompletedSession(year = 2025, now = kotlin.time.Instant.parse("2026-01-01T00:00:00Z"))
        assertEquals(9839, session?.sessionKey)
        assertEquals(
            "https://api.openf1.org/v1/sessions?session_name=Race&year=2025&date_end%3C2026-01-01T00:00:00Z",
            transport.requests.single(),
        )
    }

    @Test
    fun cachesButNotLatest() = runTest {
        val transport = io.github.halilozel1903.openf1.transport.FixtureTransport { endpoint, _ -> fixture(endpoint) }
        val client = OpenF1Client {
            this.transport = transport
            rateLimit = null
        }
        client.meetings { year eq 2025 }
        client.meetings { year eq 2025 }
        assertEquals(1, transport.requests.size)
        client.meetings { meetingKey.latest() }
        client.meetings { meetingKey.latest() }
        assertEquals(3, transport.requests.size)
        client.clearCache()
        client.meetings { year eq 2025 }
        assertEquals(4, transport.requests.size)
    }

    @Test
    fun datesParse() {
        assertEquals(1686579661L, OpenF1Dates.parse("2023-06-12T14:21:01+00:00")?.epochSeconds)
        assertEquals(584_000_000, OpenF1Dates.parse("2025-12-07T13:03:27.584000+00:00")?.nanosecondsOfSecond)
        assertEquals(OpenF1Dates.parse("2025-12-07T13:03:27Z"), OpenF1Dates.parse("2025-12-07T13:03:27"))
        assertNull(OpenF1Dates.parse("not a date"))
        assertNull(OpenF1Dates.parse(null))
    }
}
