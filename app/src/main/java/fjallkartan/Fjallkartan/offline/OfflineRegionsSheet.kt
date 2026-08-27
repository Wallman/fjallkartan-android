package fjallkartan.fjallkartan.offline

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun OfflineRegionsSheet(
    regions: List<OfflineRegionSummary>,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            item { Text("Offline maps", modifier = Modifier.padding(16.dp)) }
            if (regions.isEmpty()) {
                item { Text("No downloaded areas yet.", modifier = Modifier.padding(16.dp)) }
            }
            items(regions, key = OfflineRegionSummary::id) { region ->
                ListItem(
                    headlineContent = { Text(region.name) },
                    supportingContent = {
                        Column {
                            Text(
                                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                    .format(Date.from(region.createdAt)),
                            )
                            if (region.status == OfflineStatus.Downloading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            }
                            Text(statusText(region))
                        }
                    },
                    trailingContent = {
                        Row {
                            when (region.status) {
                                OfflineStatus.Downloading -> IconButton(onClick = { onPause(region.id) }) {
                                    Icon(Icons.Default.Pause, contentDescription = "Pause download")
                                }
                                OfflineStatus.Paused, OfflineStatus.Failed -> IconButton(
                                    onClick = { onResume(region.id) },
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume download")
                                }
                                OfflineStatus.Complete -> Unit
                            }
                            IconButton(onClick = { onDelete(region.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete offline map")
                            }
                        }
                    },
                )
            }
        }
    }
}

private fun statusText(region: OfflineRegionSummary): String {
    region.error?.let { return it }
    val megabytes = region.completedBytes / (1024.0 * 1024.0)
    return when (region.status) {
        OfflineStatus.Downloading -> "Downloading • %.1f MB".format(megabytes)
        OfflineStatus.Paused -> "Paused • %.1f MB".format(megabytes)
        OfflineStatus.Complete -> "Available offline • %.1f MB".format(megabytes)
        OfflineStatus.Failed -> "Download failed"
    }
}
