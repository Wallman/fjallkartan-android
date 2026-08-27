package fjallkartan.fjallkartan.elevation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

private val ElevationOrange = Color(0xFFF28C28)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ElevationProfileSheet(
    state: ElevationProfileState,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(Modifier.fillMaxWidth()) {
                Text(
                    "Elevation",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.Center),
                )
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Text("Done", fontWeight = FontWeight.SemiBold)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Metric("Ascent", state.ascent, Icons.Default.ArrowUpward)
                Metric("Descent", state.descent, Icons.Default.ArrowDownward)
                Metric("Highest", state.maximum, Icons.Default.Terrain)
            }
            ElevationChart(state, Modifier.fillMaxWidth().height(240.dp))
            if (state.isPartial) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "Part of this route is outside the covered area, so the totals are a minimum.",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: Double?, icon: ImageVector) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(14.dp),
            )
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        }
        Text(
            value?.let { "${it.roundToInt()} m" } ?: "—",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Picks a "nice" round grid step so a range shows roughly 3-6 gridlines. */
private fun niceStep(range: Double, targetLines: Int = 4): Double {
    if (range <= 0) return 1.0
    val roughStep = range / targetLines
    val magnitude = 10.0.pow(floor(kotlin.math.log10(roughStep)))
    val normalized = roughStep / magnitude
    val niceNormalized = when {
        normalized <= 1.0 -> 1.0
        normalized <= 2.0 -> 2.0
        normalized <= 5.0 -> 5.0
        else -> 10.0
    }
    return niceNormalized * magnitude
}

@Composable
private fun ElevationChart(state: ElevationProfileState, modifier: Modifier = Modifier) {
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    Canvas(modifier) {
        val minimum = state.minimum ?: return@Canvas
        val maximum = state.maximum ?: return@Canvas
        val totalDistance = state.points.lastOrNull()?.distance?.takeIf { it > 0 } ?: return@Canvas

        val labelPaint = android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = 11.sp.toPx()
            isAntiAlias = true
        }
        val axisLabelWidth = 40.dp.toPx()
        val topInset = 26.dp.toPx()
        val bottomInset = 34.dp.toPx()
        val plotWidth = size.width - axisLabelWidth
        val plotHeight = size.height - topInset - bottomInset
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))

        // Horizontal gridlines with elevation labels on the right.
        val elevationStep = niceStep((maximum - minimum).coerceAtLeast(1.0))
        val topElevation = ceil(maximum / elevationStep) * elevationStep
        val bottomElevation = floor((minimum - 0.0).coerceAtLeast(0.0) / elevationStep) * elevationStep
        val elevationRange = (topElevation - bottomElevation).coerceAtLeast(1.0)
        var gridElevation = bottomElevation
        while (gridElevation <= topElevation + 0.001) {
            val y = topInset + plotHeight - ((gridElevation - bottomElevation) / elevationRange * plotHeight).toFloat()
            drawLine(
                gridColor,
                Offset(0f, y),
                Offset(plotWidth, y),
                1.dp.toPx(),
                pathEffect = dashEffect,
            )
            drawContext.canvas.nativeCanvas.drawText(
                gridElevation.roundToInt().toString(),
                plotWidth + 6.dp.toPx(),
                y + 4.dp.toPx(),
                labelPaint,
            )
            gridElevation += elevationStep
        }
        drawContext.canvas.nativeCanvas.drawText(
            "m",
            plotWidth + 6.dp.toPx(),
            topInset - 14.dp.toPx(),
            labelPaint,
        )

        // Vertical gridlines with distance labels along the bottom.
        val totalKm = totalDistance / 1000
        val kmStep = niceStep(totalKm.coerceAtLeast(0.1))
        var gridKm = 0.0
        while (gridKm <= totalKm + 0.001) {
            val x = (gridKm * 1000 / totalDistance * plotWidth).toFloat()
            drawLine(
                gridColor,
                Offset(x, topInset),
                Offset(x, topInset + plotHeight),
                1.dp.toPx(),
                pathEffect = dashEffect,
            )
            drawContext.canvas.nativeCanvas.drawText(
                gridKm.roundToInt().toString(),
                x,
                topInset + plotHeight + 16.dp.toPx(),
                labelPaint,
            )
            gridKm += kmStep
        }
        drawContext.canvas.nativeCanvas.drawText(
            "km",
            0f,
            topInset + plotHeight + 32.dp.toPx(),
            labelPaint,
        )
        drawLine(gridColor, Offset(0f, topInset + plotHeight), Offset(plotWidth, topInset + plotHeight), 1.dp.toPx())

        // Elevation area + line, broken across no-data gaps.
        var linePath = Path()
        var areaPath = Path()
        var hasCurrentRun = false
        fun toPoint(distance: Double, elevation: Double): Offset {
            val x = (distance / totalDistance * plotWidth).toFloat()
            val y = topInset + plotHeight - ((elevation - bottomElevation) / elevationRange * plotHeight).toFloat()
            return Offset(x, y)
        }
        fun closeRun() {
            if (hasCurrentRun) {
                drawPath(linePath, ElevationOrange, style = Stroke(2.5.dp.toPx()))
                drawPath(
                    areaPath,
                    Brush.verticalGradient(
                        listOf(ElevationOrange.copy(alpha = 0.45f), ElevationOrange.copy(alpha = 0.05f)),
                        startY = topInset,
                        endY = topInset + plotHeight,
                    ),
                )
            }
        }
        state.points.forEach { point ->
            val elevation = point.elevation
            if (elevation == null) {
                closeRun()
                linePath = Path()
                areaPath = Path()
                hasCurrentRun = false
            } else {
                val p = toPoint(point.distance, elevation)
                if (!hasCurrentRun) {
                    linePath.moveTo(p.x, p.y)
                    areaPath.moveTo(p.x, topInset + plotHeight)
                    areaPath.lineTo(p.x, p.y)
                    hasCurrentRun = true
                } else {
                    linePath.lineTo(p.x, p.y)
                    areaPath.lineTo(p.x, p.y)
                }
            }
        }
        if (hasCurrentRun) {
            areaPath.lineTo(linePath.getBounds().right, topInset + plotHeight)
        }
        closeRun()
    }
}

