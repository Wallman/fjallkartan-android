package fjallkartan.fjallkartan.elevation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Elevation profile", style = MaterialTheme.typography.titleLarge)
            ElevationChart(state, Modifier.fillMaxWidth().height(220.dp))
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Metric("Ascent", state.ascent, Icons.Default.ArrowUpward)
                Metric("Descent", state.descent, Icons.Default.ArrowDownward)
                Metric("Lowest", state.minimum, Icons.Default.Terrain)
                Metric("Highest", state.maximum, Icons.Default.Terrain)
            }
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
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
        Text(
            value?.let { "${it.roundToInt()} m" } ?: "—",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun ElevationChart(state: ElevationProfileState, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    Canvas(modifier) {
        drawLine(gridColor, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
        val minimum = state.minimum ?: return@Canvas
        val maximum = state.maximum ?: return@Canvas
        val distance = state.points.lastOrNull()?.distance?.takeIf { it > 0 } ?: return@Canvas
        val elevationRange = (maximum - minimum).coerceAtLeast(1.0)

        var path = Path()
        var hasCurrentRun = false
        state.points.forEach { point ->
            val elevation = point.elevation
            if (elevation == null) {
                if (hasCurrentRun) drawPath(path, lineColor, style = Stroke(2.5.dp.toPx()))
                path = Path()
                hasCurrentRun = false
            } else {
                val x = (point.distance / distance * size.width).toFloat()
                val y = (size.height - (elevation - minimum) / elevationRange * size.height).toFloat()
                if (!hasCurrentRun) {
                    path.moveTo(x, y)
                    hasCurrentRun = true
                } else {
                    path.lineTo(x, y)
                }
            }
        }
        if (hasCurrentRun) drawPath(path, lineColor, style = Stroke(2.5.dp.toPx()))
    }
}
