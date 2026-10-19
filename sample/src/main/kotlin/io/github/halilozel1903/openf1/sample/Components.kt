package io.github.halilozel1903.openf1.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.halilozel1903.openf1.model.Driver
import io.github.halilozel1903.openf1.model.Stint
import io.github.halilozel1903.openf1.model.TyreCompound
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Instant

/** The driver's team colour from `team_colour`, or a neutral grey. */
val Driver?.teamColor: Color
    get() = this?.teamColorArgb?.let { Color(it) } ?: Color(0xFF8A8C9C)

val TyreCompound.color: Color get() = Color(colorArgb)

/** Black or white, whichever reads better on [background]. */
fun contentColorOn(background: Color): Color = if (background.luminance() > 0.45f) Color(0xFF111111) else Color.White

fun Double.format(decimals: Int): String = String.format(Locale.US, "%.${decimals}f", this)

private val dayFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)
private val shortDayFormat = DateTimeFormatter.ofPattern("d MMM", Locale.US)

fun Instant.day(): String = dayFormat.format(java.time.Instant.ofEpochSecond(epochSeconds).atOffset(ZoneOffset.UTC))
fun Instant.shortDay(): String = shortDayFormat.format(java.time.Instant.ofEpochSecond(epochSeconds).atOffset(ZoneOffset.UTC))

/** A round headshot from `headshot_url` on the team colour, with the acronym while it loads or when there is none. */
@Composable
fun Headshot(driver: Driver?, size: Dp, modifier: Modifier = Modifier) {
    val team = driver.teamColor
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(team.copy(alpha = 0.28f)),
        contentAlignment = Alignment.Center,
    ) {
        RemoteImage(
            url = driver?.headshotUrl,
            contentDescription = driver?.displayName,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.size(size),
            placeholder = {
                Text(
                    driver?.nameAcronym ?: "",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
        )
    }
}

/** A row of text tabs with an underline under the selected one. */
@Composable
fun <T> TextTabs(tabs: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        tabs.forEach { tab ->
            val active = tab == selected
            Column(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    label(tab),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .width(28.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent),
                )
            }
        }
    }
}

/** A card with a title, used by the detail sections. */
@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, subtitle: String? = null, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

/** A small labelled number. */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, accent: Color? = null) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                label.uppercase(Locale.US),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = accent ?: MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

/**
 * A driver's tyre stints across the race, one bar per stint in the compound's colour with its letter, from
 * `/stints`. [totalLaps] is the width of the strip.
 */
@Composable
fun TyreStrip(stints: List<Stint>, totalLaps: Int, modifier: Modifier = Modifier, height: Dp = 16.dp) {
    val measurer = rememberTextMeasurer()
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier.fillMaxWidth().height(height)) {
        val laps = totalLaps.coerceAtLeast(1).toFloat()
        val radius = CornerRadius(size.height / 2.5f)
        drawRoundRect(track, size = size, cornerRadius = radius)
        val gap = 2.dp.toPx()
        stints.forEach { stint ->
            val start = ((stint.lapStart ?: 1) - 1).coerceAtLeast(0)
            val end = (stint.lapEnd ?: totalLaps).coerceAtMost(totalLaps)
            if (end <= start) return@forEach
            val left = size.width * start / laps
            val width = (size.width * (end - start) / laps - gap).coerceAtLeast(1f)
            val fill = stint.tyre.color
            drawRoundRect(fill, topLeft = Offset(left, 0f), size = Size(width, size.height), cornerRadius = radius)
            val letter = measurer.measure(
                stint.tyre.letter,
                TextStyle(fontSize = (size.height * 0.62f).toSp(), fontWeight = FontWeight.Bold, color = contentColorOn(fill)),
            )
            if (letter.size.width + 4.dp.toPx() < width) {
                drawText(
                    letter,
                    topLeft = Offset(left + (width - letter.size.width) / 2, (size.height - letter.size.height) / 2),
                )
            }
        }
    }
}

/** The compounds and their colours. */
@Composable
fun TyreLegend(compounds: List<TyreCompound>, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        compounds.forEach { compound ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Canvas(Modifier.size(10.dp)) { drawCircle(compound.color) }
                Text(
                    compound.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A thin vertical bar in the team colour. */
@Composable
fun TeamBar(color: Color, modifier: Modifier = Modifier, height: Dp = 34.dp) {
    Canvas(modifier.width(4.dp).height(height)) {
        drawLine(color, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), size.width, StrokeCap.Round)
    }
}

/** A back chevron button. */
@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    Box(
        modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(18.dp)) {
            val stroke = 2.4.dp.toPx()
            drawLine(color, Offset(size.width * 0.65f, size.height * 0.1f), Offset(size.width * 0.3f, size.height * 0.5f), stroke, StrokeCap.Round)
            drawLine(color, Offset(size.width * 0.3f, size.height * 0.5f), Offset(size.width * 0.65f, size.height * 0.9f), stroke, StrokeCap.Round)
        }
    }
}

/** Text for small print under charts. */
@Composable
fun Caption(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
