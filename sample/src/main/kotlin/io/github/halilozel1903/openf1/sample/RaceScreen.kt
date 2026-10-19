package io.github.halilozel1903.openf1.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.halilozel1903.openf1.analysis.LapTimes
import io.github.halilozel1903.openf1.model.TyreCompound

/** The race title, circuit, date and lap count, with the country's flag. */
@Composable
fun RaceHeader(race: RaceData, onBack: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(start = if (onBack != null) 4.dp else 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) BackButton(onBack)
        Column(Modifier.weight(1f)) {
            Text(race.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    race.session.sessionName,
                    race.session.circuitShortName,
                    race.session.start?.day(),
                    "${race.totalLaps} laps".takeIf { race.totalLaps > 0 },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val flag = race.meeting?.countryFlag
        if (flag != null) {
            Box(
                Modifier
                    .width(48.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                RemoteImage(flag, race.meeting?.countryName, Modifier.fillMaxSize(), ContentScale.Crop)
            }
        }
    }
}

/** The tab content of the race hub. */
@Composable
fun RaceTabContent(
    race: RaceData,
    tab: RaceTab,
    selected: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (tab) {
        RaceTab.Results -> ResultsList(race, selected, onSelect, modifier)
        RaceTab.Drivers -> DriverGrid(race, onSelect, modifier)
        RaceTab.Strategy -> StrategyList(race, onSelect, modifier)
        RaceTab.Weather -> WeatherPanel(race, modifier)
    }
}

/** The classification built from `/position` with `classification()`, one row per driver. */
@Composable
fun ResultsList(race: RaceData, selected: Int?, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            Caption(
                "Final order from OpenF1 position data · best lap · stops · tyres",
                Modifier.padding(start = 8.dp, top = 4.dp, bottom = 6.dp),
            )
        }
        items(race.rows, key = { it.number }) { row ->
            ResultRow(row, race, selected == row.number, onClick = { onSelect(row.number) })
        }
    }
}

@Composable
private fun ResultRow(row: DriverRace, race: RaceData, selected: Boolean, onClick: () -> Unit) {
    val fastest = race.fastestLap?.let { it.driverNumber == row.number } == true
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${row.result.position}",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp),
        )
        TeamBar(row.driver.teamColor, Modifier.padding(horizontal = 6.dp))
        Headshot(row.driver, 42.dp)
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(row.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    row.driver?.teamName ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                TyreDots(row.stints.map { it.tyre })
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                row.bestLap?.lapDuration?.let { LapTimes.format(it) } ?: "–",
                style = MaterialTheme.typography.labelLarge,
                color = if (fastest) FastestPurple else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                movement(row.result.positionsGained) + " · " + stopsLabel(row.pitStops.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun movement(gained: Int): String = when {
    gained > 0 -> "▲$gained"
    gained < 0 -> "▼${-gained}"
    else -> "="
}

fun stopsLabel(stops: Int): String = if (stops == 1) "1 stop" else "$stops stops"

@Composable
private fun TyreDots(tyres: List<TyreCompound>) {
    // A hairline edge keeps the white hard compound visible on light surfaces.
    val edge = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        tyres.forEach { tyre ->
            Box(
                Modifier
                    .size(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(tyre.color)
                    .border(0.75.dp, edge, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(tyre.letter, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = contentColorOn(tyre.color))
            }
        }
    }
}

/** Every driver as a card in the team colour with the headshot from `/drivers`. */
@Composable
fun DriverGrid(race: RaceData, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        items(race.rows, key = { it.number }) { row -> DriverCard(row, onClick = { onSelect(row.number) }) }
    }
}

@Composable
private fun DriverCard(row: DriverRace, onClick: () -> Unit) {
    val team = row.driver.teamColor
    val onTeam = contentColorOn(team)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = team,
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Black.copy(alpha = 0.28f))))) {
            Text(
                "${row.number}",
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                color = onTeam.copy(alpha = 0.22f),
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 10.dp),
            )
            RemoteImage(
                url = row.driver?.headshotUrl,
                contentDescription = row.name,
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(150.dp).padding(bottom = 44.dp),
                placeholder = {
                    Text(row.acronym, style = MaterialTheme.typography.headlineSmall, color = onTeam.copy(alpha = 0.7f))
                },
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(
                    "P${row.result.position}  ${row.acronym}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
                Text(
                    row.driver?.teamName ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                row.driver?.firstName ?: "",
                style = MaterialTheme.typography.labelMedium,
                color = onTeam.copy(alpha = 0.9f),
                modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 12.dp),
            )
        }
    }
}

/** Tyre stints of the whole field from `/stints`, then the quickest pit stops from `/pit`. */
@Composable
fun StrategyList(race: RaceData, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val stops = race.rows.flatMap { row -> row.pitStops.map { row to it } }
        .filter { it.second.stopDuration != null }
        .sortedBy { it.second.stopDuration }
    val used = race.rows.flatMap { row -> row.stints.map { it.tyre } }.distinct().sortedBy { it.ordinal }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Column(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Tyre strategy", style = MaterialTheme.typography.titleMedium)
                TyreLegend(used)
            }
        }
        items(race.rows, key = { "stints-${it.number}" }) { row ->
            Row(
                Modifier.fillMaxWidth().clickable { onSelect(row.number) }.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(row.acronym, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(44.dp))
                TyreStrip(row.stints, race.totalLaps, Modifier.weight(1f), height = 16.dp)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(start = 44.dp)) {
                Caption("Lap 1")
                Spacer(Modifier.weight(1f))
                Caption("Lap ${race.totalLaps}")
            }
        }
        item {
            Column(Modifier.padding(top = 16.dp)) {
                Text("Quickest pit stops", style = MaterialTheme.typography.titleMedium)
                Caption("Stationary time from OpenF1 pit data")
            }
        }
        items(stops.take(10), key = { (row, stop) -> "stop-${row.number}-${stop.lapNumber}" }) { (row, stop) ->
            Row(
                Modifier.fillMaxWidth().clickable { onSelect(row.number) }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TeamBar(row.driver.teamColor, height = 22.dp)
                Spacer(Modifier.width(10.dp))
                Text(row.acronym, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(48.dp))
                Text("Lap ${stop.lapNumber ?: "?"}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text("${stop.stopDuration!!.format(1)} s", style = MaterialTheme.typography.labelLarge)
                Text(
                    stop.pitLaneTime?.let { "  ·  lane ${it.format(1)} s" } ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The session's weather from `/weather`: ranges, averages and a temperature chart. */
@Composable
fun WeatherPanel(race: RaceData, modifier: Modifier = Modifier) {
    val summary = race.weatherSummary
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            WeatherTiles(race)
        }
        item {
            SectionCard("Temperature", subtitle = "${summary.samples} samples from OpenF1 weather data") {
                TemperatureChart(race.weather)
            }
        }
    }
}

/** Air, track, humidity, wind and rain in tiles. */
@Composable
fun WeatherTiles(race: RaceData, modifier: Modifier = Modifier) {
    val summary = race.weatherSummary
    fun range(values: ClosedFloatingPointRange<Double>?) =
        values?.let { "${it.start.format(1)}–${it.endInclusive.format(1)} °C" } ?: "–"
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Track", range(summary.trackTemperature), Modifier.weight(1f))
            StatTile("Air", range(summary.airTemperature), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Humidity", summary.averageHumidity?.let { "${it.format(0)} %" } ?: "–", Modifier.weight(1f))
            StatTile("Wind", summary.averageWindSpeed?.let { "${it.format(1)} m/s" } ?: "–", Modifier.weight(1f))
            StatTile("Rain", if (summary.rained) "Yes" else "Dry", Modifier.weight(1f))
        }
    }
}
