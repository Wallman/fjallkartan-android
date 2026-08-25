@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package fjallkartan.fjallkartan.product

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import fjallkartan.fjallkartan.BuildConfig
import kotlinx.coroutines.launch
import org.maplibre.android.offline.OfflineManager

private data class GuidePage(
    val icon: ImageVector,
    val title: String,
    val message: String,
    val notes: List<String> = emptyList(),
)

private val guidePages = listOf(
    GuidePage(Icons.Default.Search, "Find a place", "Search Norwegian and Swedish place names.", listOf("Save a result as a pin.")),
    GuidePage(Icons.Default.Straighten, "Measure a route", "Tap the ruler, then drag to trace your route.", listOf("One finger draws.", "Two fingers move and zoom.", "Tap the distance for the terrain profile.")),
    GuidePage(Icons.Default.LocationOn, "Mark a spot", "Press and hold the map to drop a pin. Tap it to rename or delete it."),
    GuidePage(Icons.Default.Layers, "Steepness", "Shade terrain by slope angle to spot steep ground."),
    GuidePage(Icons.Default.DownloadForOffline, "Offline mode", "Frame the area inside the dashed box, then download it."),
    GuidePage(Icons.Default.CheckCircle, "You're ready", "Open this guide again from About."),
)

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun OnboardingSheet(onDismiss: () -> Unit) {
    val pager = rememberPagerState { guidePages.size }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().height(620.dp).padding(20.dp)) {
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
                val page = guidePages[index]
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    androidx.compose.material3.Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape,
                    ) {
                        Icon(
                            page.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(24.dp).size(48.dp),
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(localText(page.title), style = MaterialTheme.typography.headlineSmall)
                    Text(localText(page.message), modifier = Modifier.padding(top = 12.dp))
                    page.notes.forEach { Text("• ${localText(it)}", modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onDismiss) { Text(localText("Skip")) }
                Text("${pager.currentPage + 1} / ${guidePages.size}")
                Button(onClick = {
                    if (pager.currentPage == guidePages.lastIndex) onDismiss()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                }) {
                    Text(localText(if (pager.currentPage == guidePages.lastIndex) "Done" else "Next"))
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
fun AboutSheet(
    onDismiss: () -> Unit,
    onShowGuide: () -> Unit,
    onShowDebug: () -> Unit,
) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            Text(localText("About"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
            LinkRow("©Kartverket", "https://www.kartverket.no/")
            LinkRow("©Lantmäteriet", "https://www.lantmateriet.se/")
            LinkRow("©NVE", "https://www.nve.no/")
            LinkRow("©MapLibre Native", "https://maplibre.org/")
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            ListItem(
                headlineContent = { Text(localText("How to use the app")) },
                modifier = Modifier.combinedClickable(onClick = onShowGuide),
            )
            LinkRow(localText("Support"), "https://wallman.github.io/fjallkartan-ios/support.html")
            LinkRow(localText("Privacy policy"), "https://wallman.github.io/fjallkartan-ios/privacy.html")
            ListItem(
                headlineContent = { Text(localText("Version")) },
                trailingContent = { Text("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})") },
                modifier = Modifier.combinedClickable(onClick = {}, onLongClick = onShowDebug),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun LinkRow(title: String, url: String) {
    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(title) },
        modifier = Modifier.combinedClickable(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }),
    )
}

@Composable
fun DebugSheet(
    showZoom: Boolean,
    onShowZoomChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localText("Debug tools")) },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text(localText("Show zoom level")) },
                    trailingContent = {
                        Switch(checked = showZoom, onCheckedChange = onShowZoomChanged)
                    },
                )
                TextButton(onClick = {
                    OfflineManager.getInstance(context).clearAmbientCache(
                        object : OfflineManager.FileSourceCallback {
                            override fun onSuccess() { message = "Map cache cleared." }
                            override fun onError(error: String) { message = error }
                        },
                    )
                }) { Text(localText("Clear map cache")) }
                TextButton(onClick = { shareDiagnostics(context) }) { Text(localText("Share diagnostics")) }
                message?.let { Text(it) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(localText("Done")) } },
    )
}

@Composable
fun GuideTipBadge(text: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = Color(0xEFFFFFFF),
        shadowElevation = 6.dp,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Text(text, modifier = Modifier.padding(start = 8.dp), color = Color.Black)
        }
    }
}

@Composable
fun ToolsSheet(
    canSaveRoute: Boolean,
    onSavedRoutes: () -> Unit,
    onSaveRoute: () -> Unit,
    onChooseOfflineArea: () -> Unit,
    onOfflineMaps: () -> Unit,
    onLegend: () -> Unit,
    onAbout: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            Text(localText("Map tools"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
            ToolRow(Icons.Default.Bookmarks, localText("Saved routes"), onSavedRoutes)
            if (canSaveRoute) ToolRow(Icons.Default.Save, localText("Save current route"), onSaveRoute)
            ToolRow(Icons.Default.DownloadForOffline, localText("Download current area"), onChooseOfflineArea)
            ToolRow(Icons.Default.Folder, localText("Offline maps"), onOfflineMaps)
            ToolRow(Icons.Default.Layers, localText("Legend"), onLegend)
            ToolRow(Icons.Default.Info, localText("About"), onAbout)
        }
    }
}

@Composable
private fun ToolRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        modifier = Modifier.combinedClickable(onClick = onClick),
    )
}

class GuideTips(context: Context) {
    private val preferences = context.getSharedPreferences("guide-tips", Context.MODE_PRIVATE)

    fun take(key: String): Boolean {
        if (preferences.getBoolean(key, false)) return false
        preferences.edit().putBoolean(key, true).apply()
        return true
    }
}

@Composable
private fun localText(key: String): String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    return remember(context) { Localizer.get(context) }.text(key, locale)
}

private fun shareDiagnostics(context: Context) {
    val text = buildString {
        appendLine("Fjällkartan Android ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Android ${android.os.Build.VERSION.RELEASE} / API ${android.os.Build.VERSION.SDK_INT}")
        appendLine("Device ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        appendLine("ABI ${android.os.Build.SUPPORTED_ABIS.joinToString()}")
    }
    val intent = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, "Fjällkartan diagnostics")
        .putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(intent, "Share diagnostics"))
}
