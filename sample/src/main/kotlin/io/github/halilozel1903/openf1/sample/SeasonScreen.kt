package io.github.halilozel1903.openf1.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.openf1.model.Meeting

val SeasonYears: List<Int> = listOf(2023, 2024, 2025, 2026)

/** The Grands Prix of a season from `/meetings`, with flags. Tapping one opens its race. */
@Composable
fun SeasonScreen(
    repository: RaceRepository,
    year: Int,
    onYearChange: (Int) -> Unit,
    onOpen: (Meeting) -> Unit,
    modifier: Modifier = Modifier,
) {
    var reload by remember { mutableIntStateOf(0) }
    val season by produceState<Load<Season>>(Load.Loading, repository, year, reload) {
        value = Load.Loading
        value = load { repository.loadSeason(year) }
    }
    ReportLoad(season)
    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
            Text("Race Hub", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Formula 1 sessions from the OpenF1 API",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextTabs(SeasonYears, year, { it.toString() }, onYearChange, Modifier.padding(top = 8.dp))
        when (val state = season) {
            Load.Loading -> LoadingState()
            is Load.Failed -> ErrorState(state.message, onRetry = { reload++ })
            is Load.Ready -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 320.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Caption("${state.value.meetings.size} Grands Prix in ${state.value.year}")
                }
                itemsIndexed(state.value.meetings, key = { _, meeting -> meeting.meetingKey }) { index, meeting ->
                    MeetingCard(round = index + 1, meeting = meeting, onClick = { onOpen(meeting) })
                }
            }
        }
    }
}

@Composable
private fun MeetingCard(round: Int, meeting: Meeting, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(72.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                RemoteImage(
                    url = meeting.countryFlag,
                    contentDescription = meeting.countryName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(
                    "ROUND $round",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    meeting.meetingName ?: "Meeting ${meeting.meetingKey}",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(meeting.circuitShortName, meeting.countryName).distinct().joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                val start = meeting.start
                val end = meeting.end
                Text(
                    if (start != null && end != null) "${start.shortDay()} – ${end.shortDay()}" else "",
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    meeting.circuitType?.substringAfter("- ")?.ifBlank { null } ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
