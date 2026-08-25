package fjallkartan.fjallkartan.saved

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fjallkartan.fjallkartan.measurement.DistanceMeasurement
import fjallkartan.fjallkartan.search.PlaceResult

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PlaceSearchSheet(
    results: List<PlaceResult>,
    onQueryChanged: (String) -> Unit,
    onSelect: (PlaceResult) -> Unit,
    onSave: (PlaceResult) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    LaunchedEffect(query) { onQueryChanged(query) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                label = { Text("Search places") },
                singleLine = true,
            )
            LazyColumn {
                items(results, key = PlaceResult::id) { place ->
                    ListItem(
                        headlineContent = { Text(place.name) },
                        supportingContent = {
                            Column {
                                if (place.matchedAlias != null) Text(place.matchedAlias)
                                if (place.subtitle.isNotBlank()) Text(place.subtitle)
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { onSave(place) }) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = "Save place")
                            }
                        },
                        modifier = Modifier.clickable { onSelect(place) },
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SavedRoutesSheet(
    featured: List<FeaturedRoute>,
    saved: List<SavedRoute>,
    onLoad: (SavedRoute) -> Unit,
    onRename: (SavedRoute, String) -> Unit,
    onDelete: (SavedRoute) -> Unit,
    onDismiss: () -> Unit,
) {
    var renameTarget by remember { mutableStateOf<SavedRoute?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            item {
                Text("Suggested routes", modifier = Modifier.padding(16.dp))
            }
            items(featured, key = FeaturedRoute::id) { featuredRoute ->
                ListItem(
                    headlineContent = { Text(featuredRoute.name) },
                    supportingContent = {
                        Text("${featuredRoute.subtitle} • ${DistanceMeasurement.formatDistance(featuredRoute.route.meters)}")
                    },
                    modifier = Modifier.clickable { onLoad(featuredRoute.route) },
                )
            }
            item {
                HorizontalDivider()
                Text("Saved routes", modifier = Modifier.padding(16.dp))
            }
            items(saved, key = SavedRoute::id) { route ->
                ListItem(
                    headlineContent = { Text(route.displayName) },
                    supportingContent = { Text(DistanceMeasurement.formatDistance(route.meters)) },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { renameTarget = route }) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename route")
                            }
                            IconButton(onClick = { onDelete(route) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete route")
                            }
                        }
                    },
                    modifier = Modifier.clickable { onLoad(route) },
                )
            }
        }
    }

    renameTarget?.let { route ->
        NameDialog(
            title = "Rename route",
            initialValue = route.name.orEmpty(),
            onConfirm = {
                onRename(route, it)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }
}

@Composable
fun NameDialog(
    title: String,
    initialValue: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun PinDetailDialog(
    pin: SavedPin,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var renaming by remember { mutableStateOf(false) }
    if (renaming) {
        NameDialog(
            title = "Rename pin",
            initialValue = pin.name.orEmpty(),
            onConfirm = {
                onRename(it)
                renaming = false
                onDismiss()
            },
            onDismiss = { renaming = false },
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(pin.displayName.ifBlank { "Saved pin" }) },
            text = { Text("${pin.coordinate.latitude}, ${pin.coordinate.longitude}") },
            confirmButton = {
                TextButton(onClick = { renaming = true }) { Text("Rename") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        onDelete()
                        onDismiss()
                    }) { Text("Delete") }
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            },
        )
    }
}
