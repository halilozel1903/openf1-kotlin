package io.github.halilozel1903.openf1.query

/**
 * The filters of one request, built in a block such as
 *
 * ```kotlin
 * client.laps {
 *     sessionKey eq 9839
 *     driverNumber oneOf listOf(1, 4, 81)
 *     lapNumber inRange 10..20
 *     lapDuration lt 90.0
 * }
 * ```
 *
 * Every endpoint has its own subclass with the attributes that endpoint can be filtered by.
 */
public sealed class OpenF1Query {
    internal val sink: MutableList<Filter> = mutableListOf()

    /** The filters added so far, in order. */
    public val filters: List<Filter> get() = sink.toList()

    /** Adds a filter on an attribute this library does not model yet. */
    public fun raw(field: String, operator: Operator, value: String) {
        sink += Filter(field, operator, value)
    }

    protected fun int(name: String): IntField = IntField(name, sink)
    protected fun key(name: String): KeyField = KeyField(name, sink)
    protected fun double(name: String): DoubleField = DoubleField(name, sink)
    protected fun string(name: String): StringField = StringField(name, sink)
    protected fun boolean(name: String): BooleanField = BooleanField(name, sink)
    protected fun date(name: String): DateField = DateField(name, sink)
}

/** Filters of every endpoint that belongs to a meeting. */
public sealed class MeetingScopedQuery : OpenF1Query() {
    public val meetingKey: KeyField = key("meeting_key")
}

/** Filters of every endpoint that belongs to a session. */
public sealed class SessionScopedQuery : MeetingScopedQuery() {
    public val sessionKey: KeyField = key("session_key")
}

/** Filters of every endpoint with one row per driver and moment. */
public sealed class DriverScopedQuery : SessionScopedQuery() {
    public val driverNumber: IntField = int("driver_number")
}

/** `GET /meetings`. */
public class MeetingsQuery : MeetingScopedQuery() {
    public val year: IntField = int("year")
    public val meetingName: StringField = string("meeting_name")
    public val meetingOfficialName: StringField = string("meeting_official_name")
    public val countryName: StringField = string("country_name")
    public val countryCode: StringField = string("country_code")
    public val countryKey: IntField = int("country_key")
    public val circuitKey: IntField = int("circuit_key")
    public val circuitShortName: StringField = string("circuit_short_name")
    public val circuitType: StringField = string("circuit_type")
    public val location: StringField = string("location")
    public val dateStart: DateField = date("date_start")
    public val dateEnd: DateField = date("date_end")
    public val isCancelled: BooleanField = boolean("is_cancelled")
}

/** `GET /sessions`. */
public class SessionsQuery : SessionScopedQuery() {
    public val year: IntField = int("year")
    /** Such as `Race`, `Qualifying`, `Sprint`, `Practice 1`. */
    public val sessionName: StringField = string("session_name")
    /** Such as `Race`, `Qualifying`, `Practice`. */
    public val sessionType: StringField = string("session_type")
    public val countryName: StringField = string("country_name")
    public val countryCode: StringField = string("country_code")
    public val circuitKey: IntField = int("circuit_key")
    public val circuitShortName: StringField = string("circuit_short_name")
    public val location: StringField = string("location")
    public val dateStart: DateField = date("date_start")
    public val dateEnd: DateField = date("date_end")
    public val isCancelled: BooleanField = boolean("is_cancelled")
}

/** `GET /drivers`. */
public class DriversQuery : DriverScopedQuery() {
    public val nameAcronym: StringField = string("name_acronym")
    public val firstName: StringField = string("first_name")
    public val lastName: StringField = string("last_name")
    public val fullName: StringField = string("full_name")
    public val broadcastName: StringField = string("broadcast_name")
    public val teamName: StringField = string("team_name")
    public val teamColour: StringField = string("team_colour")
    public val headshotUrl: StringField = string("headshot_url")
}

/** `GET /laps`. */
public class LapsQuery : DriverScopedQuery() {
    public val lapNumber: IntField = int("lap_number")
    /** Seconds. */
    public val lapDuration: DoubleField = double("lap_duration")
    public val durationSector1: DoubleField = double("duration_sector_1")
    public val durationSector2: DoubleField = double("duration_sector_2")
    public val durationSector3: DoubleField = double("duration_sector_3")
    /** km/h. */
    public val i1Speed: IntField = int("i1_speed")
    public val i2Speed: IntField = int("i2_speed")
    public val stSpeed: IntField = int("st_speed")
    public val isPitOutLap: BooleanField = boolean("is_pit_out_lap")
    public val dateStart: DateField = date("date_start")
}

/** `GET /position`. */
public class PositionQuery : DriverScopedQuery() {
    public val position: IntField = int("position")
    public val date: DateField = date("date")
}

/** `GET /intervals`. Race sessions only. */
public class IntervalsQuery : DriverScopedQuery() {
    /** Seconds; lapped cars (`+1 LAP`) never match a number. */
    public val gapToLeader: DoubleField = double("gap_to_leader")
    /** Seconds to the car ahead. */
    public val interval: DoubleField = double("interval")
    public val date: DateField = date("date")
}

/** `GET /pit`. */
public class PitQuery : DriverScopedQuery() {
    public val lapNumber: IntField = int("lap_number")
    /** Seconds in the pit lane. */
    public val laneDuration: DoubleField = double("lane_duration")
    /** Seconds stationary. Recorded from the 2024 United States Grand Prix on. */
    public val stopDuration: DoubleField = double("stop_duration")
    /** Deprecated by OpenF1 in favour of [laneDuration]. */
    public val pitDuration: DoubleField = double("pit_duration")
    public val date: DateField = date("date")
}

/** `GET /stints`. */
public class StintsQuery : DriverScopedQuery() {
    public val stintNumber: IntField = int("stint_number")
    /** `SOFT`, `MEDIUM`, `HARD`, `INTERMEDIATE` or `WET`. */
    public val compound: StringField = string("compound")
    public val lapStart: IntField = int("lap_start")
    public val lapEnd: IntField = int("lap_end")
    public val tyreAgeAtStart: IntField = int("tyre_age_at_start")
}

/** `GET /race_control`. */
public class RaceControlQuery : DriverScopedQuery() {
    /** Such as `Flag`, `SafetyCar`, `Drs`, `CarEvent`, `SessionStatus`. */
    public val category: StringField = string("category")
    /** Such as `GREEN`, `YELLOW`, `RED`, `CHEQUERED`, `BLUE`. */
    public val flag: StringField = string("flag")
    /** `Track`, `Driver` or `Sector`. */
    public val scope: StringField = string("scope")
    public val sector: IntField = int("sector")
    public val lapNumber: IntField = int("lap_number")
    public val qualifyingPhase: IntField = int("qualifying_phase")
    public val message: StringField = string("message")
    public val date: DateField = date("date")
}

/** `GET /weather`. About one sample a minute. */
public class WeatherQuery : SessionScopedQuery() {
    public val airTemperature: DoubleField = double("air_temperature")
    public val trackTemperature: DoubleField = double("track_temperature")
    public val humidity: DoubleField = double("humidity")
    public val pressure: DoubleField = double("pressure")
    public val rainfall: IntField = int("rainfall")
    public val windDirection: IntField = int("wind_direction")
    public val windSpeed: DoubleField = double("wind_speed")
    public val date: DateField = date("date")
}

/** `GET /car_data`. About 3.7 samples a second per car: always narrow it to a driver and a time window. */
public class CarDataQuery : DriverScopedQuery() {
    public val speed: IntField = int("speed")
    public val rpm: IntField = int("rpm")
    public val nGear: IntField = int("n_gear")
    public val throttle: IntField = int("throttle")
    public val brake: IntField = int("brake")
    public val drs: IntField = int("drs")
    public val date: DateField = date("date")
}

/** `GET /team_radio`. */
public class TeamRadioQuery : DriverScopedQuery() {
    public val date: DateField = date("date")
}

/** `GET /session_result`. */
public class SessionResultQuery : DriverScopedQuery() {
    public val position: IntField = int("position")
    public val numberOfLaps: IntField = int("number_of_laps")
    public val dnf: BooleanField = boolean("dnf")
    public val dns: BooleanField = boolean("dns")
    public val dsq: BooleanField = boolean("dsq")
}

/** `GET /starting_grid`. */
public class StartingGridQuery : DriverScopedQuery() {
    public val position: IntField = int("position")
    public val lapDuration: DoubleField = double("lap_duration")
}
