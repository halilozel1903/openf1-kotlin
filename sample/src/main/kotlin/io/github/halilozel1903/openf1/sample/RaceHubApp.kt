package io.github.halilozel1903.openf1.sample

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The whole app: a season of meetings that opens a race hub, with tabs for results, drivers, strategy and weather,
 * and a driver screen. On wide screens the race hub shows the tabs and the driver side by side. Screenshot
 * [scene]s open a fixed race, tab and driver, and report to a [SceneMonitor] when their data and images have loaded.
 */
@Composable
fun RaceHubApp(scene: Scene?, repository: RaceRepository) {
    val monitor = remember(scene) { scene?.let { SceneMonitor(it) } }
    monitor?.Report()

    var year by rememberSaveable { mutableStateOf(2025) }
    // null: the season list. RaceRequest.SCENE_RACE: the race of the screenshot scenes. Otherwise a meeting key.
    var raceRequest by rememberSaveable {
        mutableStateOf(if (scene != null && scene != Scene.Meetings) RaceRequest.SCENE_RACE else null)
    }
    var meetingName by rememberSaveable { mutableStateOf<String?>(null) }
    var tab by rememberSaveable { mutableStateOf(scene?.tab ?: RaceTab.Results) }
    var driver by rememberSaveable { mutableStateOf(scene?.driverNumber) }

    CompositionLocalProvider(LocalSceneMonitor provides monitor) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            val wide = maxWidth >= 840.dp
            val request = raceRequest
            if (request == null) {
                SeasonScreen(
                    repository = repository,
                    year = year,
                    onYearChange = { year = it },
                    onOpen = { meeting ->
                        meetingName = meeting.meetingName
                        raceRequest = meeting.meetingKey
                        tab = RaceTab.Results
                        driver = null
                    },
                )
            } else {
                BackHandler {
                    if (!wide && driver != null) driver = null else raceRequest = null
                }
                key(request) {
                    RaceHub(
                        repository = repository,
                        request = request,
                        meetingName = meetingName,
                        wide = wide,
                        tab = tab,
                        onTabChange = { tab = it },
                        driver = driver,
                        onDriverChange = { driver = it },
                        onBack = { raceRequest = null },
                    )
                }
            }
        }
    }
}

object RaceRequest {
    /** Stands for the race the screenshot scenes look up, [RaceLookup.Scenes]. Meeting keys are positive. */
    const val SCENE_RACE = -1
}

@Composable
private fun RaceHub(
    repository: RaceRepository,
    request: Int,
    meetingName: String?,
    wide: Boolean,
    tab: RaceTab,
    onTabChange: (RaceTab) -> Unit,
    driver: Int?,
    onDriverChange: (Int?) -> Unit,
    onBack: () -> Unit,
) {
    var reload by remember { mutableIntStateOf(0) }
    val race by produceState<Load<RaceData>>(Load.Loading, repository, request, reload) {
        value = Load.Loading
        value = load {
            val session = if (request == RaceRequest.SCENE_RACE) {
                repository.findRace(RaceLookup.Scenes)
            } else {
                repository.raceOf(request, meetingName)
            }
            repository.loadRace(session)
        }
    }
    ReportLoad(race)
    when (val state = race) {
        Load.Loading -> LoadingState()
        is Load.Failed -> ErrorState(state.message, onRetry = { reload++ })
        is Load.Ready -> {
            val data = state.value
            val selected = driver?.let { data.row(it) }
            if (wide) {
                Column(Modifier.fillMaxSize()) {
                    RaceHeader(data, onBack)
                    Row(Modifier.fillMaxSize()) {
                        Column(Modifier.weight(0.44f)) {
                            TextTabs(RaceTab.entries, tab, { it.label }, onTabChange)
                            RaceTabContent(data, tab, selected?.number, onSelect = { onDriverChange(it) })
                        }
                        Box(
                            Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                        Box(Modifier.weight(0.56f)) {
                            if (selected == null) {
                                EmptyDriver()
                            } else {
                                key(selected.number) { DriverDetail(data, selected, onBack = null) }
                            }
                        }
                    }
                }
            } else if (selected != null) {
                DriverDetail(data, selected, onBack = { onDriverChange(null) })
            } else {
                Column(Modifier.fillMaxSize()) {
                    RaceHeader(data, onBack)
                    TextTabs(RaceTab.entries, tab, { it.label }, onTabChange)
                    RaceTabContent(data, tab, null, onSelect = { onDriverChange(it) })
                }
            }
        }
    }
}
