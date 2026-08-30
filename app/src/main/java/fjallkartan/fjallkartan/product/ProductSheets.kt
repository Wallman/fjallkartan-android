@file:OptIn(ExperimentalMaterial3Api::class)

package fjallkartan.fjallkartan.product

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.core.net.toUri
import fjallkartan.fjallkartan.BuildConfig
import fjallkartan.fjallkartan.R
import kotlinx.coroutines.launch
import org.maplibre.android.offline.OfflineManager

private data class GuidePage(
    val icon: ImageVector,
    @param:StringRes val title: Int,
    @param:StringRes val message: Int,
    val notes: List<Int> = emptyList(),
)

private val guidePages = listOf(
    GuidePage(Icons.Default.Search, R.string.find_a_place, R.string.search_norwegian_and_swedish_place_names_including_local, listOf(R.string.save_the_result_as_a_pin)),
    GuidePage(Icons.Default.Straighten, R.string.measure_a_route, R.string.tap_the_ruler_then_drag_to_trace_your_route, listOf(R.string.one_finger_draws, R.string.two_fingers_move_and_zoom, R.string.tap_the_distance_for_the_terrain_profile)),
    GuidePage(Icons.Default.LocationOn, R.string.mark_a_spot, R.string.press_and_hold_the_map_to_drop_a_pin_tap_a_pin_to_rename_or),

    GuidePage(Icons.Default.Layers, R.string.steepness, R.string.shade_the_terrain_by_slope_angle_to_spot_steep_ground),
    GuidePage(Icons.Default.DownloadForOffline, R.string.offline_mode, R.string.frame_the_area_inside_the_dashed_box_then_download_it),
    GuidePage(Icons.Default.CheckCircle, R.string.youre_ready, R.string.open_this_guide_again_from_about),
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
                    Text(stringResource(page.title), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(page.message), modifier = Modifier.padding(top = 12.dp))
                    page.notes.forEach { Text("• ${stringResource(it)}", modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.skip)) }
                Text("${pager.currentPage + 1} / ${guidePages.size}")
                Button(onClick = {
                    if (pager.currentPage == guidePages.lastIndex) onDismiss()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                }) {
                    Text(stringResource(if (pager.currentPage == guidePages.lastIndex) R.string.done else R.string.next))
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            Text(stringResource(R.string.about), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
            LinkRow("©Kartverket", "https://www.kartverket.no/")
            LinkRow("©Lantmäteriet", "https://www.lantmateriet.se/")
            LinkRow("©NVE", "https://www.nve.no/")
            LinkRow("©MapLibre Native", "https://maplibre.org/")
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            ListItem(
                headlineContent = { Text(stringResource(R.string.how_to_use_the_app)) },
                modifier = Modifier.combinedClickable(onClick = onShowGuide),
            )
            LinkRow(stringResource(R.string.support), "https://wallman.github.io/fjallkartan-ios/support.html")
            LinkRow(stringResource(R.string.privacy_policy), "https://wallman.github.io/fjallkartan-ios/privacy.html")
            ListItem(
                headlineContent = { Text(stringResource(R.string.version)) },
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
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
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
    var statusMessage by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.debug_tools)) },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.show_zoom_level)) },
                    trailingContent = {
                        Switch(checked = showZoom, onCheckedChange = onShowZoomChanged)
                    },
                )
                TextButton(onClick = {
                    OfflineManager.getInstance(context).clearAmbientCache(
                        object : OfflineManager.FileSourceCallback {
                            override fun onSuccess() { statusMessage = "Map cache cleared." }
                            override fun onError(message: String) { statusMessage = message }
                        },
                    )
                }) { Text(stringResource(R.string.clear_map_cache)) }
                TextButton(onClick = { shareDiagnostics(context) }) { Text(stringResource(R.string.share_diagnostics)) }
                statusMessage?.let { Text(it) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
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

class GuideTips(context: Context) {
    private val preferences = context.getSharedPreferences("guide-tips", Context.MODE_PRIVATE)

    fun take(key: String): Boolean {
        if (preferences.getBoolean(key, false)) return false
        preferences.edit { putBoolean(key, true) }
        return true
    }
}

class DebugSettings(context: Context) {
    private val preferences = context.getSharedPreferences("debug-settings", Context.MODE_PRIVATE)

    var showZoomBadge: Boolean
        get() = preferences.getBoolean("show_zoom_badge", false)
        set(value) = preferences.edit { putBoolean("show_zoom_badge", value) }
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
