package io.github.halilozel1903.openf1.model

import io.github.halilozel1903.openf1.OpenF1Dates
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

// Every attribute that OpenF1 does not always send is nullable. Unknown attributes are ignored, so new ones do not
// break decoding. Timestamps are kept as the text OpenF1 sends (ISO 8601, UTC); each model with one has an
// `Instant` property that parses it.

/** A Grand Prix or testing event: `GET /meetings`. */
@Serializable
public data class Meeting(
    @SerialName("meeting_key") public val meetingKey: Int,
    @SerialName("meeting_name") public val meetingName: String? = null,
    @SerialName("meeting_official_name") public val meetingOfficialName: String? = null,
    public val year: Int? = null,
    public val location: String? = null,
    @SerialName("country_key") public val countryKey: Int? = null,
    @SerialName("country_code") public val countryCode: String? = null,
    @SerialName("country_name") public val countryName: String? = null,
    /** Image url of the country's flag (16:9). */
    @SerialName("country_flag") public val countryFlag: String? = null,
    @SerialName("circuit_key") public val circuitKey: Int? = null,
    @SerialName("circuit_short_name") public val circuitShortName: String? = null,
    /** `Permanent`, `Temporary - Street` or `Temporary - Road`. */
    @SerialName("circuit_type") public val circuitType: String? = null,
    /** Image url of the circuit outline. */
    @SerialName("circuit_image") public val circuitImage: String? = null,
    @SerialName("circuit_info_url") public val circuitInfoUrl: String? = null,
    @SerialName("date_start") public val dateStart: String? = null,
    @SerialName("date_end") public val dateEnd: String? = null,
    /** Local time offset from UTC, such as `04:00:00` or `-05:00:00`. */
    @SerialName("gmt_offset") public val gmtOffset: String? = null,
    @SerialName("is_cancelled") public val isCancelled: Boolean = false,
) {
    public val start: Instant? get() = OpenF1Dates.parse(dateStart)
    public val end: Instant? get() = OpenF1Dates.parse(dateEnd)
}

/** One session of a meeting, such as practice, qualifying or the race: `GET /sessions`. */
@Serializable
public data class Session(
    @SerialName("session_key") public val sessionKey: Int,
    @SerialName("meeting_key") public val meetingKey: Int,
    @SerialName("session_name") public val sessionName: String? = null,
    @SerialName("session_type") public val sessionType: String? = null,
    public val year: Int? = null,
    public val location: String? = null,
    @SerialName("country_key") public val countryKey: Int? = null,
    @SerialName("country_code") public val countryCode: String? = null,
    @SerialName("country_name") public val countryName: String? = null,
    @SerialName("circuit_key") public val circuitKey: Int? = null,
    @SerialName("circuit_short_name") public val circuitShortName: String? = null,
    @SerialName("date_start") public val dateStart: String? = null,
    @SerialName("date_end") public val dateEnd: String? = null,
    @SerialName("gmt_offset") public val gmtOffset: String? = null,
    @SerialName("is_cancelled") public val isCancelled: Boolean = false,
) {
    public val start: Instant? get() = OpenF1Dates.parse(dateStart)
    public val end: Instant? get() = OpenF1Dates.parse(dateEnd)
}

/** A driver entered in a session: `GET /drivers`. */
@Serializable
public data class Driver(
    @SerialName("driver_number") public val driverNumber: Int,
    @SerialName("session_key") public val sessionKey: Int? = null,
    @SerialName("meeting_key") public val meetingKey: Int? = null,
    @SerialName("broadcast_name") public val broadcastName: String? = null,
    @SerialName("full_name") public val fullName: String? = null,
    @SerialName("first_name") public val firstName: String? = null,
    @SerialName("last_name") public val lastName: String? = null,
    /** Three letters, such as `VER`. */
    @SerialName("name_acronym") public val nameAcronym: String? = null,
    @SerialName("team_name") public val teamName: String? = null,
    /** Hex `RRGGBB` without `#`, such as `3671C6`. See [teamColorArgb]. */
    @SerialName("team_colour") public val teamColour: String? = null,
    /** Photo url from formula1.com. Missing for some drivers in some sessions; see `OpenF1Client.completeHeadshots`. */
    @SerialName("headshot_url") public val headshotUrl: String? = null,
    /** Deprecated by OpenF1 and removed after the 2026 season. */
    @SerialName("country_code") public val countryCode: String? = null,
) {
    /** "Lando Norris" from the first and last name, falling back to [fullName] or [broadcastName]. */
    public val displayName: String
        get() = listOfNotNull(firstName, lastName).joinToString(" ").ifBlank { fullName ?: broadcastName ?: "#$driverNumber" }

    /** [teamColour] as an opaque ARGB color (`0xFF3671C6`), or `null` when it is missing or malformed. */
    public val teamColorArgb: Long?
        get() = teamColour?.removePrefix("#")?.takeIf { it.length == 6 }?.toLongOrNull(16)?.let { 0xFF000000L or it }
}

/** One lap of one driver: `GET /laps`. Times are in seconds, speeds in km/h. */
@Serializable
public data class Lap(
    @SerialName("driver_number") public val driverNumber: Int,
    @SerialName("lap_number") public val lapNumber: Int,
    @SerialName("session_key") public val sessionKey: Int? = null,
    @SerialName("meeting_key") public val meetingKey: Int? = null,
    @SerialName("date_start") public val dateStart: String? = null,
    @SerialName("lap_duration") public val lapDuration: Double? = null,
    @SerialName("duration_sector_1") public val durationSector1: Double? = null,
    @SerialName("duration_sector_2") public val durationSector2: Double? = null,
    @SerialName("duration_sector_3") public val durationSector3: Double? = null,
    @SerialName("i1_speed") public val i1Speed: Int? = null,
    @SerialName("i2_speed") public val i2Speed: Int? = null,
    @SerialName("st_speed") public val stSpeed: Int? = null,
    @SerialName("is_pit_out_lap") public val isPitOutLap: Boolean = false,
    /** Mini-sector codes: 2048 yellow, 2049 green, 2051 purple, 2064 pit lane, 0 or `null` unknown. */
    @SerialName("segments_sector_1") public val segmentsSector1: List<Int?> = emptyList(),
    @SerialName("segments_sector_2") public val segmentsSector2: List<Int?> = emptyList(),
    @SerialName("segments_sector_3") public val segmentsSector3: List<Int?> = emptyList(),
) {
    public val start: Instant? get() = OpenF1Dates.parse(dateStart)
}

/** A driver's position changing: `GET /position`. Only changes are recorded. */
@Serializable
public data class Position(
    @SerialName("driver_number") public val driverNumber: Int,
    public val position: Int,
    public val date: String,
    @SerialName("session_key") public val sessionKey: Int? = null,
    @SerialName("meeting_key") public val meetingKey: Int? = null,
) {
    public val instant: Instant? get() = OpenF1Dates.parse(date)
}

/** Gaps during a race, about every 4 seconds: `GET /intervals`. */
@Serializable
public data class Interval(
    @SerialName("driver_number") public val driverNumber: Int,
    public val date: String,
    /** `null` for the leader. */
    @SerialName("gap_to_leader") public val gapToLeader: Gap? = null,
    /** Gap to the car ahead; `null` for the leader. */
    public val interval: Gap? = null,
    @SerialName("session_key") public val sessionKey: Int? = null,
    @SerialName("meeting_key") public val meetingKey: Int? = null,
) {
    public val instant: Instant? get() = OpenF1Dates.parse(date)
}

/** A trip through the pit lane: `GET /pit`. */
@Serializable
public data class PitStop(
    @SerialName("driver_number") public val driverNumber: Int,
    @SerialName("lap_number") public val lapNumber: Int? = null,
    public val date: String? = null,
    /** Seconds from pit entry to pit exit. */
    @SerialName("lane_duration") public val laneDuration: Double? = null,
    /** Seconds stationary; recorded from the 2024 United States Grand Prix on. */
    @SerialName("stop_duration") public val stopDuration: Double? = null,
    /** Deprecated by OpenF1 (same as [laneDuration]); older seasons only have this one. */
    @SerialName("pit_duration") public val pitDuration: Double? = null,
    @SerialName("session_key") public val sessionKey: Int? = null,
    @SerialName("meeting_key") public val meetingKey: Int? = null,
) {
    /** [laneDuration], or [pitDuration] for data that predates it. */
    public val pitLaneTime: Double? get() = laneDuration ?: pitDuration
    public val instant: Instant? get() = OpenF1Dates.parse(date)
}

/** A run on one set of tyres: `GET /stints`. */
@Serializable
public data class Stint(
    @SerialName("driver_number") public val driverNumber: Int,
    @SerialName("stint_number") public val stintNumber: Int,
    public val compound: String? = null,
    @SerialName("lap_start") public val lapStart: Int? = null,
    @SerialName("lap_end") public val lapEnd: Int? = null,
    @SerialName("tyre_age_at_start") public val tyreAgeAtStart: Int? = null,
    @SerialName("session_key") public val sessionKey: Int? = null,
    @SerialName("meeting_key") public val meetingKey: Int? = null,
) {
    public val tyre: TyreCompound get() = TyreCompound.from(compound)

    /** Laps in this stint, or `null` when either end is unknown. */
    public val laps: Int? get() = if (lapStart != null && lapEnd != null) lapEnd - lapStart + 1 else null
}

/** A tyre compound. */
public enum class TyreCompound(
    /** The value OpenF1 sends. */
    public val apiName: String,
    /** One letter as on TV graphics. */
    public val letter: String,
    /** The sidewall color Pirelli uses, ARGB. */
    public val colorArgb: Long,
) {
    Soft("SOFT", "S", 0xFFE8002D),
    Medium("MEDIUM", "M", 0xFFFFD12E),
    Hard("HARD", "H", 0xFFF0F0EC),
    Intermediate("INTERMEDIATE", "I", 0xFF43B02A),
    Wet("WET", "W", 0xFF0067AD),
    Unknown("UNKNOWN", "?", 0xFF9AA0A6),
    ;

    public companion object {
        /** The compound for OpenF1's [name], [Unknown] for anything else (such as `TEST_UNKNOWN`). */
        public fun from(name: String?): TyreCompound =
            entries.firstOrNull { it.apiName.equals(name?.trim(), ignoreCase = true) } ?: Unknown
    }
}

/** A message from race control: flags, safety car, penalties, track limits: `GET /race_control`. */
@Serializable
public data class RaceControlMessage(
    public val date: String,
    public val message: String? = null,
    public val category: String? = null,
    public val flag: String? = null,
    public val scope: String? = null,
    public val sector: Int? = null,
    @SerialName("driver_number") public val driverNumber: Int? = null,
    @SerialName("lap_number") public val lapNumber: Int? = null,
    @SerialName("qualifying_phase") public val qualifyingPhase: Int? = null,
    @SerialName("session_key") public val sessionKey: Int? = null,
    @SerialName("meeting_key") public val meetingKey: Int? = null,
) {
    public val instant: Instant? get() = OpenF1Dates.parse(date)
}

/** A weather sample at the track, about once a minute: `GET /weather`. */
@Serializable
public data class Weather(
    public val date: String,
    /** °C. */
    @SerialName("air_temperature") public val airTemperature: Double? = null,
    /** °C. */
    @SerialName("track_temperature") public val trackTemperature: Double? = null,
    /** Percent. */
    public val humidity: Double? = null,
    /** mbar. */
    public val pressure: Double? = null,
    /** Non-zero when it rains. */
    public val rainfall: Double? = null,
    /** Degrees, 0 to 359. */
    @SerialName("wind_direction") public val windDirection: Double? = null,
    /** m/s. */
    @SerialName("wind_speed") public val windSpeed: Double? = null,
    @SerialName("session_key") public val sessionKey: Int? = null,
    @SerialName("meeting_key") public val meetingKey: Int? = null,
) {
    public val isRaining: Boolean get() = (rainfall ?: 0.0) > 0.0
    public val instant: Instant? get() = OpenF1Dates.parse(date)
}

/** Car telemetry, about 3.7 samples a second: `GET /car_data`. */
@Serializable
public data class CarData(
    @SerialName("driver_number") public val driverNumber: Int,
    public val date: String,
    /** km/h. */
    public val speed: Int? = null,
    public val rpm: Int? = null,
    /** 1 to 8, 0 is neutral. */
    @SerialName("n_gear") public val nGear: Int? = null,
    /** Percent. */
    public val throttle: Int? = null,
    /** 0 or 100. */
    public val brake: Int? = null,
    /** OpenF1's DRS code; 10, 12 and 14 mean open. */
    public val drs: Int? = null,
    @SerialName("session_key") public val sessionKey: Int? = null,
    @SerialName("meeting_key") public val meetingKey: Int? = null,
) {
    public val isDrsOpen: Boolean get() = drs == 10 || drs == 12 || drs == 14
    public val instant: Instant? get() = OpenF1Dates.parse(date)
}

/** A team radio clip: `GET /team_radio`. */
@Serializable
public data class TeamRadio(
    @SerialName("driver_number") public val driverNumber: Int,
    public val date: String,
    /** MP3 url on livetiming.formula1.com. */
    @SerialName("recording_url") public val recordingUrl: String,
    @SerialName("session_key") public val sessionKey: Int? = null,
    @SerialName("meeting_key") public val meetingKey: Int? = null,
) {
    public val instant: Instant? get() = OpenF1Dates.parse(date)
}

/** The official result of a driver in a session: `GET /session_result`. */
@Serializable
public data class SessionResult(
    @SerialName("driver_number") public val driverNumber: Int,
    public val position: Int? = null,
    @SerialName("number_of_laps") public val numberOfLaps: Int? = null,
    /** Race: total time. Practice: best lap. Qualifying: best lap of Q1, Q2 and Q3. Seconds. */
    @kotlinx.serialization.Serializable(with = OneOrManyDurations::class)
    public val duration: List<Double?> = emptyList(),
    /** Like [duration]: one value, or one per qualifying part. */
    @kotlinx.serialization.Serializable(with = OneOrManyGaps::class)
    @SerialName("gap_to_leader") public val gapToLeader: List<Gap?> = emptyList(),
    public val dnf: Boolean = false,
    public val dns: Boolean = false,
    public val dsq: Boolean = false,
    @SerialName("session_key") public val sessionKey: Int? = null,
    @SerialName("meeting_key") public val meetingKey: Int? = null,
)

/** A starting grid slot: `GET /starting_grid`. */
@Serializable
public data class StartingGridSlot(
    @SerialName("driver_number") public val driverNumber: Int,
    public val position: Int,
    /** The qualifying lap that set it, seconds. */
    @SerialName("lap_duration") public val lapDuration: Double? = null,
    @SerialName("session_key") public val sessionKey: Int? = null,
    @SerialName("meeting_key") public val meetingKey: Int? = null,
)
