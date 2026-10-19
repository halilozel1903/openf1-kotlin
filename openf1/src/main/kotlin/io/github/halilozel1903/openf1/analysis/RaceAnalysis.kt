package io.github.halilozel1903.openf1.analysis

import io.github.halilozel1903.openf1.model.Lap
import io.github.halilozel1903.openf1.model.PitStop
import io.github.halilozel1903.openf1.model.Position
import io.github.halilozel1903.openf1.model.Stint
import io.github.halilozel1903.openf1.model.Weather
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** A driver's place in a classification built from `/position` data. */
public data class ClassifiedPosition(
    public val driverNumber: Int,
    /** The last recorded position. */
    public val position: Int,
    /** The first recorded position: the grid slot in a race. */
    public val startPosition: Int,
) {
    /** Places gained (positive) or lost (negative) from [startPosition] to [position]. */
    public val positionsGained: Int get() = startPosition - position
}

/**
 * The classification at the end of the data: each driver's last recorded position, ordered by position. OpenF1
 * records only position changes, so the last row of each driver is where they ended up. A car that stopped keeps
 * its last position while others move past it, so ties go to the driver whose position changed most recently, and
 * [ClassifiedPosition.position] is the rank after that. For the official result, with penalties applied afterwards,
 * use `/session_result`.
 */
public fun List<Position>.classification(): List<ClassifiedPosition> {
    val ordered = sortedBy { it.date }
    val first = LinkedHashMap<Int, Int>()
    val last = LinkedHashMap<Int, Position>()
    for (row in ordered) {
        first.getOrPut(row.driverNumber) { row.position }
        last[row.driverNumber] = row
    }
    return last.values
        .sortedWith(compareBy<Position> { it.position }.thenByDescending { it.date }.thenBy { it.driverNumber })
        .mapIndexed { index, row -> ClassifiedPosition(row.driverNumber, index + 1, first.getValue(row.driverNumber)) }
}

/** Each driver's position after every change, as `(date, position)` pairs, for position charts. */
public fun List<Position>.positionHistory(): Map<Int, List<Pair<String, Int>>> =
    sortedBy { it.date }.groupBy({ it.driverNumber }, { it.date to it.position })

/** The quickest lap with a time, or `null`. Pit out laps count only when they are the quickest. */
public fun List<Lap>.fastestLap(): Lap? = filter { it.lapDuration != null }.minByOrNull { it.lapDuration!! }

/** The quickest lap of each driver. */
public fun List<Lap>.fastestLapByDriver(): Map<Int, Lap> =
    filter { it.lapDuration != null }.groupBy { it.driverNumber }.mapValues { (_, laps) -> laps.minBy { it.lapDuration!! } }

/**
 * Laps that represent race pace: with a time, not a pit out lap, not lap 1, and not slower than [cutoff] times
 * the driver's median (which leaves out pit in laps, safety car laps and incidents).
 */
public fun List<Lap>.representativeLaps(cutoff: Double = 1.07): List<Lap> {
    val timed = filter { it.lapDuration != null && !it.isPitOutLap && it.lapNumber > 1 }
    val medians = timed.groupBy { it.driverNumber }.mapValues { (_, laps) -> laps.map { it.lapDuration!! }.median() }
    return timed.filter { it.lapDuration!! <= medians.getValue(it.driverNumber) * cutoff }
}

private fun List<Double>.median(): Double {
    val sorted = sorted()
    return if (sorted.size % 2 == 1) sorted[sorted.size / 2] else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
}

/** Each driver's stints in order. */
@JvmName("stintsByDriver")
public fun List<Stint>.byDriver(): Map<Int, List<Stint>> =
    groupBy { it.driverNumber }.mapValues { (_, stints) -> stints.sortedBy { it.stintNumber } }

/** Each driver's pit stops in order. */
@JvmName("pitStopsByDriver")
public fun List<PitStop>.byDriver(): Map<Int, List<PitStop>> =
    groupBy { it.driverNumber }.mapValues { (_, stops) -> stops.sortedBy { it.lapNumber ?: Int.MAX_VALUE } }

/** The quickest stationary time ([PitStop.stopDuration]), or `null` when none was recorded. */
public fun List<PitStop>.fastestStop(): PitStop? = filter { it.stopDuration != null }.minByOrNull { it.stopDuration!! }

/** Ranges and averages over a session's weather samples. */
public data class WeatherSummary(
    public val samples: Int,
    public val airTemperature: ClosedFloatingPointRange<Double>?,
    public val trackTemperature: ClosedFloatingPointRange<Double>?,
    public val averageHumidity: Double?,
    public val averageWindSpeed: Double?,
    public val averagePressure: Double?,
    /** Whether any sample reported rain. */
    public val rained: Boolean,
)

/** Summarizes a session's weather samples. */
public fun List<Weather>.summary(): WeatherSummary {
    fun range(values: List<Double>) = if (values.isEmpty()) null else values.min()..values.max()
    fun average(values: List<Double>) = if (values.isEmpty()) null else values.average()
    return WeatherSummary(
        samples = size,
        airTemperature = range(mapNotNull { it.airTemperature }),
        trackTemperature = range(mapNotNull { it.trackTemperature }),
        averageHumidity = average(mapNotNull { it.humidity }),
        averageWindSpeed = average(mapNotNull { it.windSpeed }),
        averagePressure = average(mapNotNull { it.pressure }),
        rained = any { it.isRaining },
    )
}

/** Formats times the way timing screens do. */
public object LapTimes {
    /** `88.987` → `1:28.987`, `59.1` → `59.100`. */
    public fun format(seconds: Double): String {
        val millis = (seconds * 1000).roundToLong()
        val minutes = millis / 60_000
        val rest = millis % 60_000
        val text = String.format(Locale.US, "%d.%03d", rest / 1000, rest % 1000)
        return if (minutes > 0) "$minutes:${text.padStart(6, '0')}" else text
    }

    /** `5025.6` → `1:23:45.600`, for race times. */
    public fun formatLong(seconds: Double): String {
        val millis = (seconds * 1000).roundToLong()
        val hours = millis / 3_600_000
        if (hours == 0L) return format(seconds)
        val minutes = (millis % 3_600_000) / 60_000
        val rest = millis % 60_000
        return String.format(Locale.US, "%d:%02d:%02d.%03d", hours, minutes, rest / 1000, rest % 1000)
    }

    /** `0.734` → `+0.734`, `-1.2` → `-1.200`. */
    public fun delta(seconds: Double): String =
        (if (seconds < 0) "-" else "+") + String.format(Locale.US, "%.3f", abs(seconds))
}
