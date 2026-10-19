package io.github.halilozel1903.openf1.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.halilozel1903.openf1.analysis.LapTimes
import io.github.halilozel1903.openf1.analysis.representativeLaps
import io.github.halilozel1903.openf1.model.Lap
import io.github.halilozel1903.openf1.model.Weather
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Lap times of one driver from `/laps`, in the team colour. The scale fits the representative laps (see
 * `representativeLaps`), so pit laps and lap 1 sit at the top edge instead of flattening the chart. Pit laps are
 * marked with dashed lines and the driver's best lap with a purple dot.
 */
@Composable
fun LapChart(
    laps: List<Lap>,
    color: Color,
    pitLaps: Set<Int>,
    totalLaps: Int,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
) {
    val timed = remember(laps) { laps.filter { it.lapDuration != null }.sortedBy { it.lapNumber } }
    if (timed.size < 2) {
        Caption("Not enough timed laps to draw a chart.", modifier)
        return
    }
    val scale = remember(laps) {
        val pace = laps.representativeLaps().mapNotNull { it.lapDuration }.ifEmpty { timed.mapNotNull { it.lapDuration } }
        floor(pace.min() - 0.4)..ceil(pace.max() + 0.6)
    }
    val best = timed.minBy { it.lapDuration!! }
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val pit = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)

    Canvas(modifier.fillMaxWidth().height(height)) {
        val left = 52.dp.toPx()
        val bottom = 20.dp.toPx()
        val top = 6.dp.toPx()
        val plotWidth = size.width - left - 4.dp.toPx()
        val plotHeight = size.height - bottom - top
        val lapsWide = (totalLaps.coerceAtLeast(timed.last().lapNumber) - 1).coerceAtLeast(1).toFloat()
        fun x(lap: Int) = left + plotWidth * (lap - 1) / lapsWide
        fun y(seconds: Double): Float {
            val clamped = seconds.coerceIn(scale.start, scale.endInclusive)
            return top + plotHeight * ((scale.endInclusive - clamped) / (scale.endInclusive - scale.start)).toFloat()
        }

        // Grid lines with lap times every second or two.
        val span = scale.endInclusive - scale.start
        val step = if (span > 6) 2.0 else 1.0
        var tick = ceil(scale.start / step) * step
        while (tick <= scale.endInclusive) {
            val ty = y(tick)
            drawLine(grid, Offset(left, ty), Offset(left + plotWidth, ty), 1.dp.toPx())
            val label = measurer.measure(LapTimes.format(tick).substringBefore('.'), labelStyle)
            drawText(label, topLeft = Offset(left - label.size.width - 6.dp.toPx(), ty - label.size.height / 2))
            tick += step
        }
        // Lap labels.
        val lapTicks = listOf(1) + (10..totalLaps step 10).toList() + listOf(totalLaps).filter { it % 10 > 3 }
        lapTicks.distinct().forEach { lap ->
            val label = measurer.measure("$lap", labelStyle)
            drawText(label, topLeft = Offset((x(lap) - label.size.width / 2).coerceAtLeast(left), size.height - label.size.height))
        }
        // Pit laps.
        pitLaps.forEach { lap ->
            drawLine(
                pit,
                Offset(x(lap), top),
                Offset(x(lap), top + plotHeight),
                1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
            )
        }
        // The lap time line.
        val path = Path()
        timed.forEachIndexed { index, lap ->
            val point = Offset(x(lap.lapNumber), y(lap.lapDuration!!))
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(FastestPurple, radius = 5.dp.toPx(), center = Offset(x(best.lapNumber), y(best.lapDuration!!)))
    }
}

/** Air and track temperature over a session, from `/weather`. */
@Composable
fun TemperatureChart(samples: List<Weather>, modifier: Modifier = Modifier, height: Dp = 160.dp) {
    val air = samples.mapNotNull { it.airTemperature }
    val track = samples.mapNotNull { it.trackTemperature }
    if (air.size < 2 && track.size < 2) {
        Caption("Not enough weather samples to draw a chart.", modifier)
        return
    }
    val all = air + track
    val low = floor(all.min() - 1)
    val high = ceil(all.max() + 1)
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val airColor = MaterialTheme.colorScheme.secondary
    val trackColor = MaterialTheme.colorScheme.primary
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val left = 34.dp.toPx()
            val top = 6.dp.toPx()
            val plotWidth = size.width - left
            val plotHeight = size.height - top * 2
            fun y(value: Double) = top + plotHeight * ((high - value) / (high - low)).toFloat()
            var tick = ceil(low / 5) * 5
            while (tick <= high) {
                drawLine(grid, Offset(left, y(tick)), Offset(size.width, y(tick)), 1.dp.toPx())
                val label = measurer.measure("${tick.toInt()}°", labelStyle)
                drawText(label, topLeft = Offset(left - label.size.width - 6.dp.toPx(), y(tick) - label.size.height / 2))
                tick += 5
            }
            listOf(air to airColor, track to trackColor).forEach { (values, lineColor) ->
                if (values.size < 2) return@forEach
                val path = Path()
                values.forEachIndexed { index, value ->
                    val px = left + plotWidth * index / (values.size - 1)
                    if (index == 0) path.moveTo(px, y(value)) else path.lineTo(px, y(value))
                }
                drawPath(path, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Legend("Track", trackColor)
            Legend("Air", airColor)
            Caption("Start to finish of the session")
        }
    }
}

@Composable
private fun Legend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(10.dp)) { drawCircle(color) }
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
