package io.github.halilozel1903.openf1.sample

/**
 * Screenshot scenes, picked with the `scene` intent extra. They load live data from OpenF1 like a normal launch.
 * Every scene but [Meetings] opens the race found with [RaceLookup.Scenes] at runtime: the 2025 Abu Dhabi Grand
 * Prix, a finished session, so the screenshots stay the same from run to run.
 */
enum class Scene(val key: String, val tab: RaceTab, val driverNumber: Int?) {
    Meetings("meetings", RaceTab.Results, null),
    Race("race", RaceTab.Results, null),
    Drivers("drivers", RaceTab.Drivers, null),
    Driver("driver", RaceTab.Results, 4),
    Tablet("tablet", RaceTab.Results, 4),
    ;

    companion object {
        fun from(key: String?): Scene? = entries.firstOrNull { it.key == key }
    }
}

/** How a scene finds its race: by season, session name and location, through the sessions endpoint. */
data class RaceLookup(val year: Int, val location: String) {
    companion object {
        val Scenes = RaceLookup(year = 2025, location = "Yas Island")
    }
}

enum class RaceTab(val label: String) {
    Results("Results"),
    Drivers("Drivers"),
    Strategy("Strategy"),
    Weather("Weather"),
}
