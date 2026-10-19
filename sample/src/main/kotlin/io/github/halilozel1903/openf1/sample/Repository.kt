package io.github.halilozel1903.openf1.sample

import io.github.halilozel1903.openf1.OpenF1Cache
import io.github.halilozel1903.openf1.OpenF1Client
import io.github.halilozel1903.openf1.RateLimit
import io.github.halilozel1903.openf1.analysis.ClassifiedPosition
import io.github.halilozel1903.openf1.analysis.WeatherSummary
import io.github.halilozel1903.openf1.analysis.classification
import io.github.halilozel1903.openf1.analysis.fastestLap
import io.github.halilozel1903.openf1.analysis.fastestLapByDriver
import io.github.halilozel1903.openf1.analysis.byDriver
import io.github.halilozel1903.openf1.analysis.summary
import io.github.halilozel1903.openf1.model.Driver
import io.github.halilozel1903.openf1.model.Lap
import io.github.halilozel1903.openf1.model.Meeting
import io.github.halilozel1903.openf1.model.PitStop
import io.github.halilozel1903.openf1.model.Session
import io.github.halilozel1903.openf1.model.Stint
import io.github.halilozel1903.openf1.model.Weather
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.time.Clock

/** A season's Grands Prix, without testing. */
data class Season(val year: Int, val meetings: List<Meeting>)

/** One row of the classification with everything the race screens show for that driver. */
data class DriverRace(
    val result: ClassifiedPosition,
    val driver: Driver?,
    val laps: List<Lap>,
    val stints: List<Stint>,
    val pitStops: List<PitStop>,
    val bestLap: Lap?,
) {
    val number: Int get() = result.driverNumber
    val acronym: String get() = driver?.nameAcronym ?: number.toString()
    val name: String get() = driver?.displayName ?: "Car $number"
    val lapsCompleted: Int get() = laps.maxOfOrNull { it.lapNumber } ?: 0
}

/** Everything the race hub shows for one race. */
data class RaceData(
    val session: Session,
    val meeting: Meeting?,
    val rows: List<DriverRace>,
    val totalLaps: Int,
    val fastestLap: Lap?,
    val weather: List<Weather>,
    val weatherSummary: WeatherSummary,
) {
    val title: String get() = meeting?.meetingName ?: session.location ?: "Race"
    fun row(driverNumber: Int): DriverRace? = rows.firstOrNull { it.number == driverNumber }
}

/**
 * The sample's only data source: the live OpenF1 API through [OpenF1Client], with its rate limiting, retries and
 * cache. Nothing is bundled: every screen is built from API responses.
 */
class RaceRepository(private val client: OpenF1Client) {

    /** `GET /meetings?year=…`, without pre-season testing, in calendar order. */
    suspend fun loadSeason(year: Int): Season {
        val meetings = client.meetings { this.year eq year }
            .filter { !it.isCancelled && it.meetingName?.contains("Testing", ignoreCase = true) != true }
            .sortedBy { it.dateStart.orEmpty() }
        if (meetings.isEmpty()) throw MissingDataException("OpenF1 has no meetings for $year.")
        return Season(year, meetings)
    }

    /** Finds the race of a scene with `sessions?year=…&session_name=Race&location=…`. */
    suspend fun findRace(lookup: RaceLookup): Session =
        client.sessions {
            year eq lookup.year
            sessionName eq "Race"
            location eq lookup.location
        }.firstOrNull { !it.isCancelled }
            ?: throw MissingDataException("No ${lookup.year} race at ${lookup.location} in OpenF1.")

    /** The race of the meeting with [meetingKey], when it has been run. [name] is only used in messages. */
    suspend fun raceOf(meetingKey: Int, name: String?): Session {
        val race = client.sessions {
            this.meetingKey eq meetingKey
            sessionName eq "Race"
        }.firstOrNull { !it.isCancelled }
            ?: throw MissingDataException("${name ?: "This meeting"} has no race session.")
        val end = race.end
        if (end != null && end > Clock.System.now()) {
            throw MissingDataException("The ${name ?: "race"} has not finished yet. Live sessions need an OpenF1 sponsor token.")
        }
        return race
    }

    /**
     * Loads one race: its meeting, drivers (with missing headshots filled in from other sessions), position changes,
     * laps, tyre stints, pit stops and weather. Nine requests, spaced to OpenF1's free rate limit by the client.
     */
    suspend fun loadRace(session: Session): RaceData = coroutineScope {
        val key = session.sessionKey
        val meeting = async { client.meetings { meetingKey eq session.meetingKey }.firstOrNull() }
        val drivers = async { client.completeHeadshots(client.drivers { sessionKey eq key }) }
        val positions = async { client.positions { sessionKey eq key } }
        val laps = async { client.laps { sessionKey eq key } }
        val stints = async { client.stints { sessionKey eq key } }
        val pitStops = async { client.pitStops { sessionKey eq key } }
        val weather = async { client.weather { sessionKey eq key } }

        val classification = positions.await().classification()
        if (classification.isEmpty()) throw MissingDataException("OpenF1 has no position data for this race yet.")
        val allLaps = laps.await()
        val lapsByDriver = allLaps.groupBy { it.driverNumber }
        val best = allLaps.fastestLapByDriver()
        val stintsByDriver = stints.await().byDriver()
        val stopsByDriver = pitStops.await().groupBy { it.driverNumber }
        val driversByNumber = drivers.await().associateBy { it.driverNumber }
        val samples = weather.await().sortedBy { it.date }

        RaceData(
            session = session,
            meeting = meeting.await(),
            rows = classification.map { result ->
                val number = result.driverNumber
                DriverRace(
                    result = result,
                    driver = driversByNumber[number],
                    laps = lapsByDriver[number].orEmpty().sortedBy { it.lapNumber },
                    stints = stintsByDriver[number].orEmpty(),
                    pitStops = stopsByDriver[number].orEmpty().sortedBy { it.lapNumber ?: 0 },
                    bestLap = best[number],
                )
            },
            totalLaps = allLaps.maxOfOrNull { it.lapNumber } ?: 0,
            fastestLap = allLaps.fastestLap(),
            weather = samples,
            weatherSummary = samples.summary(),
        )
    }
}

/** The app's repository, created on first use. */
object Repositories {
    private var live: RaceRepository? = null
    private var scenes: RaceRepository? = null

    private fun client(retries: Int) = OpenF1Client {
        // Shares OkHttp with Coil.
        engine = OkHttp.create()
        rateLimit = RateLimit.Free
        cache = OpenF1Cache.memory(maxEntries = 48)
        userAgent = "openf1-kotlin-sample/1.0.0 (+https://github.com/halilozel1903/openf1-kotlin)"
        this.retries = retries
        rateLimitRetries = 5
    }

    /** Normal use, or screenshot scenes with more retries for a busy CI network. */
    @Synchronized
    fun get(forScenes: Boolean): RaceRepository = if (forScenes) {
        scenes ?: RaceRepository(client(retries = 4)).also { scenes = it }
    } else {
        live ?: RaceRepository(client(retries = 2)).also { live = it }
    }
}
