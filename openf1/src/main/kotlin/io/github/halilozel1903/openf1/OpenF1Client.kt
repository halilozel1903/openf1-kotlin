package io.github.halilozel1903.openf1

import io.github.halilozel1903.openf1.model.CarData
import io.github.halilozel1903.openf1.model.Driver
import io.github.halilozel1903.openf1.model.Interval
import io.github.halilozel1903.openf1.model.Lap
import io.github.halilozel1903.openf1.model.Meeting
import io.github.halilozel1903.openf1.model.PitStop
import io.github.halilozel1903.openf1.model.Position
import io.github.halilozel1903.openf1.model.RaceControlMessage
import io.github.halilozel1903.openf1.model.Session
import io.github.halilozel1903.openf1.model.SessionResult
import io.github.halilozel1903.openf1.model.StartingGridSlot
import io.github.halilozel1903.openf1.model.Stint
import io.github.halilozel1903.openf1.model.TeamRadio
import io.github.halilozel1903.openf1.model.Weather
import io.github.halilozel1903.openf1.query.CarDataQuery
import io.github.halilozel1903.openf1.query.DriversQuery
import io.github.halilozel1903.openf1.query.Filter
import io.github.halilozel1903.openf1.query.IntervalsQuery
import io.github.halilozel1903.openf1.query.LapsQuery
import io.github.halilozel1903.openf1.query.MeetingsQuery
import io.github.halilozel1903.openf1.query.OpenF1Query
import io.github.halilozel1903.openf1.query.PitQuery
import io.github.halilozel1903.openf1.query.PositionQuery
import io.github.halilozel1903.openf1.query.RaceControlQuery
import io.github.halilozel1903.openf1.query.SessionResultQuery
import io.github.halilozel1903.openf1.query.SessionsQuery
import io.github.halilozel1903.openf1.query.StartingGridQuery
import io.github.halilozel1903.openf1.query.StintsQuery
import io.github.halilozel1903.openf1.query.TeamRadioQuery
import io.github.halilozel1903.openf1.query.WeatherQuery
import io.github.halilozel1903.openf1.transport.KtorTransport
import io.github.halilozel1903.openf1.transport.OpenF1Transport
import kotlinx.coroutines.delay
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * A coroutine client for the [OpenF1 API](https://openf1.org): Formula 1 timing, telemetry and session data from
 * 2023 on, free without a key for finished sessions.
 *
 * ```kotlin
 * val client = OpenF1Client()
 * val race = client.sessions { year eq 2025; sessionName eq "Race"; location eq "Yas Island" }.single()
 * val quickLaps = client.laps {
 *     sessionKey eq race.sessionKey
 *     driverNumber eq 4
 *     lapDuration lt 87.0
 * }
 * ```
 *
 * Every endpoint takes a block of typed filters: only the attributes of that endpoint are offered, with the right
 * value type, and the comparison operators OpenF1 supports (`eq`, `gt`, `gte`, `lt`, `lte`, `between`, `oneOf`).
 * A query that matches nothing returns an empty list. Requests are spaced to OpenF1's rate limit
 * ([OpenF1Config.rateLimit]), retried after HTTP 429, and cached by url. Every function is main-safe and throws an
 * [OpenF1Exception] on failure. Close the client when you no longer need it.
 */
public class OpenF1Client(configure: OpenF1Config.() -> Unit = {}) : AutoCloseable {

    private val config = OpenF1Config().apply(configure)

    /** The API root, always ending with a slash. */
    public val baseUrl: String = config.baseUrl.let { if (it.endsWith('/')) it else "$it/" }

    private val transport: OpenF1Transport = config.transport ?: KtorTransport(
        httpClient = config.httpClient,
        engine = config.engine,
        userAgent = config.userAgent,
        token = config.tokenProvider ?: config.accessToken?.let { token -> { token } },
        requestTimeoutMillis = config.requestTimeoutMillis,
        retries = config.retries,
    )
    private val cache = config.cache
    private val limiter = config.rateLimit?.let { RateLimiter(it, config.clock) }

    // region Endpoints

    /** `GET /meetings`: Grands Prix and tests, such as `meetings { year eq 2025 }`. */
    public suspend fun meetings(query: MeetingsQuery.() -> Unit = {}): List<Meeting> =
        list(OpenF1Endpoint.Meetings, MeetingsQuery().apply(query), Meeting.serializer())

    /** `GET /sessions`: practice, qualifying, sprint and race sessions. */
    public suspend fun sessions(query: SessionsQuery.() -> Unit = {}): List<Session> =
        list(OpenF1Endpoint.Sessions, SessionsQuery().apply(query), Session.serializer())

    /** `GET /drivers`: the drivers of a session, with team colours and headshots. */
    public suspend fun drivers(query: DriversQuery.() -> Unit = {}): List<Driver> =
        list(OpenF1Endpoint.Drivers, DriversQuery().apply(query), Driver.serializer())

    /** `GET /laps`: lap and sector times, speed traps and mini-sectors. */
    public suspend fun laps(query: LapsQuery.() -> Unit = {}): List<Lap> =
        list(OpenF1Endpoint.Laps, LapsQuery().apply(query), Lap.serializer())

    /** `GET /position`: every position change. */
    public suspend fun positions(query: PositionQuery.() -> Unit = {}): List<Position> =
        list(OpenF1Endpoint.Position, PositionQuery().apply(query), Position.serializer())

    /** `GET /intervals`: gaps to the leader and to the car ahead during races, about every 4 seconds. */
    public suspend fun intervals(query: IntervalsQuery.() -> Unit = {}): List<Interval> =
        list(OpenF1Endpoint.Intervals, IntervalsQuery().apply(query), Interval.serializer())

    /** `GET /pit`: pit lane visits with lane and stationary times. */
    public suspend fun pitStops(query: PitQuery.() -> Unit = {}): List<PitStop> =
        list(OpenF1Endpoint.Pit, PitQuery().apply(query), PitStop.serializer())

    /** `GET /stints`: tyre stints with compound and tyre age. */
    public suspend fun stints(query: StintsQuery.() -> Unit = {}): List<Stint> =
        list(OpenF1Endpoint.Stints, StintsQuery().apply(query), Stint.serializer())

    /** `GET /race_control`: flags, safety cars, penalties and other race control messages. */
    public suspend fun raceControl(query: RaceControlQuery.() -> Unit = {}): List<RaceControlMessage> =
        list(OpenF1Endpoint.RaceControl, RaceControlQuery().apply(query), RaceControlMessage.serializer())

    /** `GET /weather`: air and track temperature, humidity, wind and rain, about once a minute. */
    public suspend fun weather(query: WeatherQuery.() -> Unit = {}): List<Weather> =
        list(OpenF1Endpoint.Weather, WeatherQuery().apply(query), Weather.serializer())

    /**
     * `GET /car_data`: speed, RPM, gear, throttle, brake and DRS, about 3.7 samples a second. Narrow it to one
     * driver and a time window (`date.between(from, to)`) or one lap's start and end; a whole race is far too much.
     */
    public suspend fun carData(query: CarDataQuery.() -> Unit = {}): List<CarData> =
        list(OpenF1Endpoint.CarData, CarDataQuery().apply(query), CarData.serializer())

    /** `GET /team_radio`: links to radio clips. */
    public suspend fun teamRadio(query: TeamRadioQuery.() -> Unit = {}): List<TeamRadio> =
        list(OpenF1Endpoint.TeamRadio, TeamRadioQuery().apply(query), TeamRadio.serializer())

    /** `GET /session_result`: the official classification of a session. */
    public suspend fun sessionResults(query: SessionResultQuery.() -> Unit = {}): List<SessionResult> =
        list(OpenF1Endpoint.SessionResult, SessionResultQuery().apply(query), SessionResult.serializer())

    /** `GET /starting_grid`: the grid of a race or sprint. */
    public suspend fun startingGrid(query: StartingGridQuery.() -> Unit = {}): List<StartingGridSlot> =
        list(OpenF1Endpoint.StartingGrid, StartingGridQuery().apply(query), StartingGridSlot.serializer())

    /** The same data as CSV text (`csv=true`), for spreadsheets and notebooks. Empty when nothing matches. */
    public suspend fun csv(endpoint: OpenF1Endpoint, filters: List<Filter> = emptyList()): String =
        load(urlFor(endpoint, filters, csv = true)).orEmpty()

    // endregion

    // region Helpers

    /**
     * The most recent session called [sessionName] (`Race` by default) that ended before [now] and was not
     * cancelled, or `null` when there is none. Optionally limited to one [year].
     */
    public suspend fun latestCompletedSession(
        sessionName: String = "Race",
        year: Int? = null,
        now: Instant = Clock.System.now(),
    ): Session? = sessions {
        this.sessionName eq sessionName
        if (year != null) this.year eq year
        dateEnd lt now
    }.filter { !it.isCancelled }.maxByOrNull { it.dateStart.orEmpty() }

    /**
     * [drivers] with missing `headshot_url`s filled in from the same driver's entries in other sessions (OpenF1
     * leaves them out for some drivers in some sessions, often rookies and stand-ins). Drivers are matched by number
     * and last name; one request covers every driver that needs it. Drivers without any headshot stay as they are.
     */
    public suspend fun completeHeadshots(drivers: List<Driver>): List<Driver> {
        val missing = drivers.filter { it.headshotUrl.isNullOrBlank() }
        if (missing.isEmpty()) return drivers
        val known = this.drivers {
            driverNumber oneOf missing.map { it.driverNumber }.distinct()
            // Text comparison leaves out rows without a headshot.
            headshotUrl gte "http"
        }.filter { !it.headshotUrl.isNullOrBlank() }
        return drivers.map { driver ->
            if (!driver.headshotUrl.isNullOrBlank()) return@map driver
            val match = known
                .filter { it.driverNumber == driver.driverNumber && sameName(it, driver) }
                .maxByOrNull { it.sessionKey ?: 0 }
            if (match != null) driver.copy(headshotUrl = match.headshotUrl) else driver
        }
    }

    private fun sameName(a: Driver, b: Driver): Boolean =
        a.lastName == null || b.lastName == null || a.lastName.equals(b.lastName, ignoreCase = true)

    /** The url this client requests for [endpoint] with [filters]. */
    public fun urlFor(endpoint: OpenF1Endpoint, filters: List<Filter>, csv: Boolean = false): String {
        val parameters = filters.map { it.toQueryParameter() } + (if (csv) listOf("csv=true") else emptyList())
        return if (parameters.isEmpty()) "$baseUrl${endpoint.path}" else "$baseUrl${endpoint.path}?${parameters.joinToString("&")}"
    }

    /** The url this client requests for [endpoint] with the filters of [query]. */
    public fun urlFor(endpoint: OpenF1Endpoint, query: OpenF1Query): String = urlFor(endpoint, query.filters)

    /** Loads [endpoint] with [filters] and decodes each row with [serializer]. */
    public suspend fun <T> list(endpoint: OpenF1Endpoint, filters: List<Filter>, serializer: KSerializer<T>): List<T> =
        fetch(urlFor(endpoint, filters), ListSerializer(serializer))

    /** Loads [endpoint] with the filters of [query] and decodes each row with [serializer]. */
    public suspend fun <T> list(endpoint: OpenF1Endpoint, query: OpenF1Query, serializer: KSerializer<T>): List<T> =
        list(endpoint, query.filters, serializer)

    /** Loads any OpenF1 url that answers with a JSON array and decodes it. "No results" decodes as `[]`. */
    public suspend fun <T> fetch(url: String, deserializer: DeserializationStrategy<List<T>>): List<T> {
        val body = load(url) ?: return emptyList()
        return try {
            json.decodeFromString(deserializer, body)
        } catch (e: SerializationException) {
            throw OpenF1Exception.Decoding(url, e)
        } catch (e: IllegalArgumentException) {
            throw OpenF1Exception.Decoding(url, e)
        }
    }

    /** Removes every cached response. */
    public suspend fun clearCache() {
        cache?.clear()
    }

    /** Releases the HTTP client this instance created. */
    override fun close() {
        (transport as? AutoCloseable)?.close()
    }

    // endregion

    private suspend fun load(url: String): String? {
        val cache = cache?.takeIf { "=latest" !in url }
        cache?.get(url)?.let { return it.ifEmpty { null } }
        var attempt = 0
        while (true) {
            limiter?.acquire()
            val body = try {
                transport.get(url)
            } catch (e: CancellationException) {
                throw e
            } catch (e: OpenF1Exception.RateLimited) {
                if (attempt >= config.rateLimitRetries) throw e
                val waitMillis = e.retryAfterSeconds?.times(1_000) ?: (2_000L shl attempt)
                attempt++
                delay(waitMillis.coerceIn(500, 60_000))
                continue
            } catch (e: OpenF1Exception) {
                throw e
            } catch (e: Exception) {
                throw OpenF1Exception.Network(url, e)
            }
            // An empty string stands for "no results" in the cache.
            cache?.put(url, body.orEmpty())
            return body?.takeUnless { it.isBlank() }
        }
    }

    public companion object {
        /** `https://api.openf1.org/v1/`. */
        public const val DEFAULT_BASE_URL: String = "https://api.openf1.org/v1/"

        internal val json: Json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
            isLenient = true
        }
    }
}
