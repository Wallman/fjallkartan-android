package fjallkartan.fjallkartan.offline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.text.format.Formatter
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        val context = LocalContext.current
        var pendingDeleteId by remember { mutableStateOf<String?>(null) }
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(bottom = 28.dp)) {
            item { Text("Offline maps", modifier = Modifier.padding(16.dp)) }
            if (regions.isEmpty()) {
                item { Text("No downloaded areas yet.", modifier = Modifier.padding(16.dp)) }
            }
            items(regions, key = OfflineRegionSummary::id) { region ->
                ListItem(
                    headlineContent = { Text(region.name) },
                    supportingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                                when (region.status) {
                                    OfflineStatus.Downloading -> CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                    OfflineStatus.Complete -> Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Available offline",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(16.dp),
                                    )
                                    OfflineStatus.Paused -> Icon(
                                        Icons.Default.Pause,
                                        contentDescription = "Paused",
                                        modifier = Modifier.size(16.dp),
                                    )
                                    OfflineStatus.Failed -> Unit
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                    .format(Date.from(region.createdAt)),
                            )
                            if (region.status == OfflineStatus.Complete && region.completedBytes > 0) {
                                Text(
                                    " • ${Formatter.formatShortFileSize(context, region.completedBytes)}",
                                )
                            }
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
                            IconButton(onClick = { pendingDeleteId = region.id }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete offline map")
                            }
                        }
                    },
                )
            }
        }
        val deleteTarget = regions.firstOrNull { it.id == pendingDeleteId }
        if (deleteTarget != null) {
            AlertDialog(
                onDismissRequest = { pendingDeleteId = null },
                title = { Text("Delete offline map?") },
                text = { Text("\"${deleteTarget.name}\" will be removed and its downloaded data deleted. This can't be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        onDelete(deleteTarget.id)
                        pendingDeleteId = null
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteId = null }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}
