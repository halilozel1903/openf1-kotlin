package io.github.halilozel1903.openf1

import io.github.halilozel1903.openf1.analysis.LapTimes
import io.github.halilozel1903.openf1.analysis.byDriver
import io.github.halilozel1903.openf1.analysis.classification
import io.github.halilozel1903.openf1.analysis.fastestLap
import io.github.halilozel1903.openf1.analysis.fastestLapByDriver
import io.github.halilozel1903.openf1.analysis.fastestStop
import io.github.halilozel1903.openf1.analysis.positionHistory
import io.github.halilozel1903.openf1.analysis.representativeLaps
import io.github.halilozel1903.openf1.analysis.summary
import io.github.halilozel1903.openf1.model.Gap
import io.github.halilozel1903.openf1.model.Position
import io.github.halilozel1903.openf1.model.TyreCompound
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AnalysisTest {

    @Test
    fun classificationFromPositionChanges() = runTest {
        val (client, _) = Fixtures.client()
        val result = client.positions { sessionKey eq 9839 }.classification()
        assertEquals(listOf(1, 81, 4), result.map { it.driverNumber })
        assertEquals(listOf(1, 2, 3), result.map { it.position })
        assertEquals(1, result[1].positionsGained)
        assertEquals(-1, result[2].positionsGained)
        assertEquals(2, client.positions().positionHistory().getValue(4).size)
    }

    @Test
    fun aStoppedCarDropsBehindTheCarThatTookItsPlace() {
        val rows = listOf(
            Position(driverNumber = 1, position = 1, date = "2025-01-01T13:00:00+00:00"),
            Position(driverNumber = 2, position = 2, date = "2025-01-01T13:00:00+00:00"),
            Position(driverNumber = 3, position = 3, date = "2025-01-01T13:00:00+00:00"),
            // Car 2 stops; car 3 is now second, car 2 is never updated.
            Position(driverNumber = 3, position = 2, date = "2025-01-01T13:30:00+00:00"),
        )
        val result = rows.classification()
        assertEquals(listOf(1, 3, 2), result.map { it.driverNumber })
        assertEquals(listOf(1, 2, 3), result.map { it.position })
    }

    @Test
    fun lapHelpers() = runTest {
        val (client, _) = Fixtures.client()
        val laps = client.laps()
        assertEquals(48, laps.fastestLap()?.lapNumber)
        assertEquals(48, laps.fastestLapByDriver().getValue(4).lapNumber)
        // Lap 1, the pit out lap and the lap without a time are left out.
        assertEquals(listOf(16, 48), laps.representativeLaps().map { it.lapNumber })
    }

    @Test
    fun stintsAndStopsByDriver() = runTest {
        val (client, _) = Fixtures.client()
        val stints = client.stints().byDriver().getValue(4)
        assertEquals(listOf(TyreCompound.Medium, TyreCompound.Hard, TyreCompound.Hard), stints.map { it.tyre })
        val stops = client.pitStops()
        assertEquals(listOf(16, 40), stops.byDriver().getValue(4).map { it.lapNumber })
        assertEquals(2.1, stops.fastestStop()?.stopDuration)
    }

    @Test
    fun weatherSummary() = runTest {
        val (client, _) = Fixtures.client()
        val summary = client.weather().summary()
        assertEquals(2, summary.samples)
        assertEquals(25.7..26.1, summary.airTemperature)
        assertEquals(30.2..31.4, summary.trackTemperature)
        assertEquals(60.0, summary.averageHumidity)
        assertFalse(summary.rained)
    }

    @Test
    fun lapTimeFormatting() {
        assertEquals("1:28.987", LapTimes.format(88.987))
        assertEquals("1:26.818", LapTimes.format(86.818))
        assertEquals("59.100", LapTimes.format(59.1))
        assertEquals("1:05.004", LapTimes.format(65.004))
        assertEquals("1:26:37.123", LapTimes.formatLong(5197.123))
        assertEquals("+0.734", LapTimes.delta(0.734))
        assertEquals("-1.200", LapTimes.delta(-1.2))
    }

    @Test
    fun gapParsing() {
        assertEquals(Gap.Laps(2), Gap.parse("+2 LAPS"))
        assertEquals(Gap.Seconds(1.5), Gap.parse("+1.5"))
        assertEquals(null, Gap.parse("DNF"))
        assertEquals("+2.104", Gap.Seconds(2.104).toString())
    }

    @Test
    fun tyreCompounds() {
        assertEquals(TyreCompound.Intermediate, TyreCompound.from("intermediate"))
        assertEquals(TyreCompound.Unknown, TyreCompound.from(null))
    }
}
