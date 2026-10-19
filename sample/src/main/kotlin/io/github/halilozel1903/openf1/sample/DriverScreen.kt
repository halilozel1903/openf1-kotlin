package io.github.halilozel1903.openf1.sample

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.halilozel1903.openf1.analysis.LapTimes

/** One driver's race: result, lap time chart, tyre stints and pit stops. */
@Composable
fun DriverDetail(race: RaceData, row: DriverRace, onBack: (() -> Unit)?, modifier: Modifier = Modifier) {
    val pitLaps = row.laps.filter { it.isPitOutLap }.map { it.lapNumber }.toSet()
    val fastest = race.fastestLap?.driverNumber == row.number
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { DriverHero(race, row, onBack) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Finished", "P${row.result.position}", Modifier.weight(1f))
                StatTile("Started", "P${row.result.startPosition}", Modifier.weight(1f))
                StatTile(
                    "Best lap",
                    row.bestLap?.lapDuration?.let { LapTimes.format(it) } ?: "–",
                    Modifier.weight(1.25f),
                    accent = if (fastest) FastestPurple else null,
                )
                StatTile("Stops", "${row.pitStops.size}", Modifier.weight(0.85f))
            }
        }
        item {
            SectionCard(
                "Lap times",
                subtitle = "${row.laps.size} laps · dashed lines are pit exits · purple dot is the best lap",
            ) {
                LapChart(row.laps, row.driver.teamColor, pitLaps, race.totalLaps)
            }
        }
        item {
            SectionCard("Tyre stints") {
                TyreStrip(row.stints, race.totalLaps, height = 22.dp)
                row.stints.forEach { stint ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(stint.tyre.color),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stint.tyre.name,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.width(110.dp),
                        )
                        Text(
                            "Laps ${stint.lapStart ?: "?"}–${stint.lapEnd ?: "?"}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            when (val age = stint.tyreAgeAtStart) {
                                null -> ""
                                0 -> "new"
                                else -> "$age laps old"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            SectionCard("Pit stops") {
                if (row.pitStops.isEmpty()) Caption("No pit stops recorded.")
                row.pitStops.forEach { stop ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Lap ${stop.lapNumber ?: "?"}",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.width(72.dp),
                        )
                        Text(
                            stop.stopDuration?.let { "${it.format(1)} s stationary" } ?: "Stationary time not recorded",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            stop.pitLaneTime?.let { "${it.format(1)} s in lane" } ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** The team colour banner with the headshot, number, name and result. */
@Composable
private fun DriverHero(race: RaceData, row: DriverRace, onBack: (() -> Unit)?) {
    val team = row.driver.teamColor
    val onTeam = contentColorOn(team)
    Box(
        Modifier
            .fillMaxWidth()
            .height(196.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(team, team.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.55f)))),
    ) {
        Text(
            "${row.number}",
            fontSize = 120.sp,
            fontWeight = FontWeight.Black,
            color = onTeam.copy(alpha = 0.16f),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 120.dp),
        )
        RemoteImage(
            url = row.driver?.headshotUrl,
            contentDescription = row.name,
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomEnd,
            modifier = Modifier.align(Alignment.BottomEnd).size(188.dp),
            placeholder = { Text(row.acronym, fontSize = 40.sp, fontWeight = FontWeight.Black, color = onTeam.copy(alpha = 0.6f)) },
        )
        Column(Modifier.align(Alignment.TopStart).padding(start = if (onBack != null) 4.dp else 18.dp, top = 8.dp, end = 160.dp)) {
            if (onBack != null) {
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))) { BackButton(onBack) }
            }
            Spacer(Modifier.height(if (onBack != null) 6.dp else 12.dp))
            Column(Modifier.padding(start = if (onBack != null) 14.dp else 0.dp)) {
                Text(
                    "P${row.result.position} · ${race.title}",
                    style = MaterialTheme.typography.labelLarge,
                    color = onTeam.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(row.driver?.firstName ?: "", style = MaterialTheme.typography.titleMedium, color = onTeam)
                Text(
                    (row.driver?.lastName ?: row.name).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = onTeam,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "#${row.number} · ${row.driver?.teamName ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onTeam.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Shown on wide screens before a driver is picked. */
@Composable
fun EmptyDriver(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Caption("Pick a driver to see lap times, tyre stints and pit stops.")
    }
}
