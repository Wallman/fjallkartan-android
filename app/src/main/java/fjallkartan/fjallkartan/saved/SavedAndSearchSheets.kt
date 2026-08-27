package fjallkartan.fjallkartan.saved

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import fjallkartan.fjallkartan.measurement.DistanceMeasurement
import fjallkartan.fjallkartan.product.localText
import fjallkartan.fjallkartan.search.PlaceResult

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PlaceSearchSheet(
    query: String,
    results: List<PlaceResult>,
    onQueryChanged: (String) -> Unit,
    onSelect: (PlaceResult) -> Unit,
    onSave: (PlaceResult) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.72f).padding(bottom = 24.dp)) {
            TextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .focusRequester(focusRequester),
                label = { Text(localText("Search places")) },
                singleLine = true,
            )
            LazyColumn(Modifier.weight(1f)) {
                items(results, key = PlaceResult::id) { place ->
                    ListItem(
                        headlineContent = { Text(place.name) },
                        supportingContent = {
                            Column {
                                Row {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.extraSmall,
                                    ) {
                                        Text(
                                            place.countryCode,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                        )
                                    }
                                    if (place.subtitle.isNotBlank()) {
                                        Text(
                                            place.subtitle,
                                            modifier = Modifier.padding(start = 6.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                                if (place.matchedAlias != null) {
                                    Text(
                                        localText("also %@").replace("%@", place.matchedAlias),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { onSave(place) }) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = localText("Save place"))
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(bottom = 28.dp)) {
            item {
                Text(localText("Saved routes"), modifier = Modifier.padding(16.dp))
            }
            items(saved, key = SavedRoute::id) { route ->
                ListItem(
                    headlineContent = { Text(route.displayName) },
                    supportingContent = { Text(DistanceMeasurement.formatDistance(route.meters)) },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { renameTarget = route }) {
                                Icon(Icons.Default.Edit, contentDescription = localText("Rename route"))
                            }
                            IconButton(onClick = { onDelete(route) }) {
                                Icon(Icons.Default.Delete, contentDescription = localText("Delete route"))
                            }
                        }
                    },
                    modifier = Modifier.clickable { onLoad(route) },
                )
            }
            item {
                HorizontalDivider()
                Text(localText("Suggested routes"), modifier = Modifier.padding(16.dp))
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
        }
    }

    renameTarget?.let { route ->
        NameDialog(
            title = localText("Rename route"),
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
                label = { Text(localText("Name")) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text(localText("Save")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(localText("Cancel")) }
        },
    )
}

@Composable
fun PinDetailDialog(
    pin: SavedPin,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(pin.id) { mutableStateOf(pin.name.orEmpty()) }
    var notes by remember(pin.id) { mutableStateOf(pin.notes.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(pin.displayName.ifBlank { localText("Saved pin") }) },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(localText("Name")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(localText("Notes")) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                Text(
                    "${pin.coordinate.latitude}, ${pin.coordinate.longitude}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(name, notes)
                onDismiss()
            }) { Text(localText("Save")) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    onDelete()
                    onDismiss()
                }) {
                    Text(localText("Delete"), color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text(localText("Cancel")) }
            }
        },
    )
}
