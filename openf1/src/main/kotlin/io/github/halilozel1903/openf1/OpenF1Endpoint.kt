package io.github.halilozel1903.openf1

/** The OpenF1 endpoints, by path. */
public enum class OpenF1Endpoint(public val path: String) {
    Meetings("meetings"),
    Sessions("sessions"),
    Drivers("drivers"),
    Laps("laps"),
    Position("position"),
    Intervals("intervals"),
    Pit("pit"),
    Stints("stints"),
    RaceControl("race_control"),
    Weather("weather"),
    CarData("car_data"),
    TeamRadio("team_radio"),
    SessionResult("session_result"),
    StartingGrid("starting_grid"),
}
