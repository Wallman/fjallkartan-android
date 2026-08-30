package fjallkartan.fjallkartan.map

import android.Manifest
import android.app.Activity
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.StatFs
import android.text.format.Formatter
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.createBitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.play.core.review.ReviewManagerFactory
import fjallkartan.fjallkartan.R
import fjallkartan.fjallkartan.elevation.ElevationProfileSheet
import fjallkartan.fjallkartan.measurement.DistanceMeasurement
import fjallkartan.fjallkartan.measurement.GeoCoordinate
import fjallkartan.fjallkartan.measurement.MeasurementState
import fjallkartan.fjallkartan.offline.OfflineRegionsSheet
import fjallkartan.fjallkartan.offline.OfflineStatus
import fjallkartan.fjallkartan.product.AboutSheet
import fjallkartan.fjallkartan.product.DebugSettings
import fjallkartan.fjallkartan.product.DebugSheet
import fjallkartan.fjallkartan.product.GuideTipBadge
import fjallkartan.fjallkartan.product.GuideTips
import fjallkartan.fjallkartan.product.LegendSheet
import fjallkartan.fjallkartan.product.OnboardingSheet
import fjallkartan.fjallkartan.product.ReviewPrompter
import fjallkartan.fjallkartan.saved.NameDialog
import fjallkartan.fjallkartan.saved.PinDetailDialog
import fjallkartan.fjallkartan.saved.PlaceSearchSheet
import fjallkartan.fjallkartan.saved.SavedPin
import fjallkartan.fjallkartan.saved.SavedRoutesSheet
import fjallkartan.fjallkartan.search.PlaceResult
import kotlinx.coroutines.delay
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.OnLocationCameraTransitionListener
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MapScreen(viewModel: MapViewModel = viewModel()) {
    val slopeVisible by viewModel.slopeVisible.collectAsStateWithLifecycle()
    val trackingEnabled by viewModel.trackingEnabled.collectAsStateWithLifecycle()
    val measurement by viewModel.measurement.collectAsStateWithLifecycle()
    val elevation by viewModel.elevation.collectAsStateWithLifecycle()
    val savedRoutes by viewModel.savedRoutes.collectAsStateWithLifecycle()
    val savedPins by viewModel.savedPins.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val searchSelection by viewModel.selectedPlace.collectAsStateWithLifecycle()
    val routeFitVersion by viewModel.routeFitVersion.collectAsStateWithLifecycle()
    val offlineRegions by viewModel.offlineRegions.collectAsStateWithLifecycle()
    val offlineError by viewModel.offlineError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var zoom by remember { mutableDoubleStateOf(3.5) }
    var metersPerPixel by remember { mutableDoubleStateOf(0.0) }
    var tileZ by remember { mutableIntStateOf(0) }
    var tileColumn by remember { mutableIntStateOf(0) }
    var tileRow by remember { mutableIntStateOf(0) }
    val debugSettings = remember { DebugSettings(context) }
    var showElevation by rememberSaveable { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showSavedRoutes by rememberSaveable { mutableStateOf(false) }
    var showSaveRoute by rememberSaveable { mutableStateOf(false) }
    var selectedPinId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedPin = selectedPinId?.let { id -> savedPins.firstOrNull { it.id.toString() == id } }
    var isPickingOffline by rememberSaveable { mutableStateOf(false) }
    var showOfflineRegions by rememberSaveable { mutableStateOf(false) }
    var showTools by rememberSaveable { mutableStateOf(false) }
    var showLegend by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showDebug by rememberSaveable { mutableStateOf(false) }
    var showZoom by rememberSaveable { mutableStateOf(debugSettings.showZoomBadge) }
    var showGuide by rememberSaveable { mutableStateOf(false) }
    var guideTip by rememberSaveable { mutableStateOf<String?>(null) }
    val guideTips = remember { GuideTips(context) }
    val reviewPrompter = remember { ReviewPrompter(context) }
    var reviewPending by rememberSaveable { mutableStateOf(false) }
    var measurementStartVersion by remember { mutableStateOf<Int?>(null) }
    val knownCompletedRegions = remember { mutableSetOf<String>() }
    var offlineRegionsInitialized by rememberSaveable { mutableStateOf(false) }
    var pendingOfflineBounds by remember { mutableStateOf<LatLngBounds?>(null) }
    var offlinePreviewBounds by remember { mutableStateOf<LatLngBounds?>(null) }
    val mapState = remember { MapHolder() }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions.values.any { it }) {
            mapState.setTracking(true)
        }
    }
    LaunchedEffect(Unit) {
        reviewPrompter.noteAppOpen()
    }
    LaunchedEffect(measurement.isMeasuring) {
        if (measurement.isMeasuring) {
            measurementStartVersion = measurement.version
            if (guideTips.take("measurement")) {
                guideTip = "Draw with one finger. Use two fingers to move or zoom."
            }
        } else {
            val startedAt = measurementStartVersion
            if (startedAt != null && measurement.version != startedAt) {
                reviewPending = reviewPrompter.recordMeasurement(measurement.totalMeters) || reviewPending
            }
            measurementStartVersion = null
        }
    }
    LaunchedEffect(offlineRegions) {
        val completed = offlineRegions.filter { it.status == OfflineStatus.Complete }.mapTo(mutableSetOf()) { it.id }
        if (offlineRegionsInitialized) {
            if (completed.any { it !in knownCompletedRegions }) {
                reviewPending = reviewPrompter.recordOfflineCompletion() || reviewPending
            }
        } else {
            offlineRegionsInitialized = true
        }
        knownCompletedRegions.clear()
        knownCompletedRegions.addAll(completed)
    }
    LaunchedEffect(guideTip) {
        if (guideTip != null) {
            delay(7_000.milliseconds)
            guideTip = null
        }
    }
    LaunchedEffect(
        reviewPending,
        measurement.isMeasuring,
        isPickingOffline,
        showSearch,
        showSavedRoutes,
        showOfflineRegions,
        showElevation,
        showTools,
        showLegend,
        showAbout,
        showGuide,
    ) {
        if (
            reviewPending && !measurement.isMeasuring && !isPickingOffline &&
            !showSearch && !showSavedRoutes && !showOfflineRegions && !showElevation &&
            !showTools && !showLegend && !showAbout && !showGuide
        ) {
            delay(3_000.milliseconds)
            if (reviewPrompter.consume()) {
                val activity = context as? Activity
                if (activity != null) {
                    val manager = ReviewManagerFactory.create(context)
                    manager.requestReviewFlow().addOnCompleteListener { task ->
                        if (task.isSuccessful) manager.launchReviewFlow(activity, task.result)
                    }
                }
            }
            reviewPending = false
        }
    }

    LaunchedEffect(isPickingOffline) {
        offlinePreviewBounds = if (isPickingOffline) mapState.currentOfflineBounds() else null
    }
    LaunchedEffect(slopeVisible) {
        mapState.setSlopeVisible(slopeVisible)
    }
    LaunchedEffect(trackingEnabled) {
        if (!trackingEnabled) {
            mapState.setTracking(false)
        } else if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            mapState.setTracking(true)
        } else {
            locationPermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        MapLibreView(
            holder = mapState,
            measurement = measurement,
            onStrokeFinished = viewModel::appendStroke,
            onPreviewChanged = viewModel::updatePreview,
            pins = savedPins,
            selectedPlace = searchSelection.place,
            selectedPlaceToken = searchSelection.token,
            routeFitVersion = routeFitVersion,
            isPickingOffline = isPickingOffline,
            onDropPin = viewModel::dropPin,
            onPinSelected = { pinId -> selectedPinId = pinId.toString() },
            onSaveSearchPlace = viewModel::savePlace,
            onDismissSearchPlace = { viewModel.selectPlace(null) },
            onCameraChanged = { camera, updatedMetersPerPixel ->
                zoom = camera.zoom
                metersPerPixel = updatedMetersPerPixel
                camera.target?.let { target ->
                    val (z, column, row) = tileCoordinate(target.latitude, target.longitude, camera.zoom)
                    tileZ = z
                    tileColumn = column
                    tileRow = row
                }
                if (isPickingOffline) offlinePreviewBounds = mapState.currentOfflineBounds()
            },
        )

        if (isPickingOffline) {
            OfflinePreviewOverlay(Modifier.fillMaxSize())
        }

        val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val controlsModifier = if (isLandscape) {
            Modifier
                .align(Alignment.TopEnd)
                // Unlabeled buttons (e.g. the collapsed "more" button) are 48dp circles with
                // no centering inset, while labeled buttons shown when expanded are centered
                // in a wider 64dp column. The extra 8dp here equalizes the gap to the compass
                // between the collapsed and expanded states.
                .padding(top = statusBarInset + 6.dp, end = 56.dp)
        } else {
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 110.dp, end = 4.dp)
                .width(64.dp)
        }
        val controlsEnter: (Int) -> androidx.compose.animation.EnterTransition = { delayMs ->
            if (isLandscape) {
                fadeIn(tween(durationMillis = 180, delayMillis = delayMs)) +
                    expandHorizontally(tween(durationMillis = 180, delayMillis = delayMs), expandFrom = Alignment.Start)
            } else {
                fadeIn(tween(durationMillis = 180, delayMillis = delayMs)) +
                    expandVertically(tween(durationMillis = 180, delayMillis = delayMs), expandFrom = Alignment.Top)
            }
        }
        val controlsExit: androidx.compose.animation.ExitTransition = if (isLandscape) {
            fadeOut(tween(durationMillis = 120)) + shrinkHorizontally(tween(durationMillis = 120), shrinkTowards = Alignment.Start)
        } else {
            fadeOut(tween(durationMillis = 120)) + shrinkVertically(tween(durationMillis = 120), shrinkTowards = Alignment.Top)
        }
        MapControlsBar(isLandscape = isLandscape, modifier = controlsModifier) {
            MapControlButton(onClick = { showSearch = true }) {
                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_places))
            }
            MapControlButton(onClick = viewModel::toggleTracking) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = stringResource(R.string.track_my_location),
                    tint = if (trackingEnabled) Color(0xFFFF9500) else Color.Black,
                )
            }
            MapControlButton(onClick = viewModel::toggleMeasuring) {
                Icon(
                    Icons.Default.Straighten,
                    contentDescription = stringResource(R.string.measure_distance),
                    tint = if (measurement.isMeasuring) Color(0xFFFF9500) else Color.Black,
                )
            }
            val measurementButtons = buildList<@Composable () -> Unit> {
                add {
                    MapControlButton(
                        onClick = viewModel::undoMeasurement,
                        enabled = measurement.canUndo,
                        label = stringResource(R.string.undo),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = stringResource(R.string.undo_last_stroke),
                            tint = if (measurement.canUndo) Color(0xFF007AFF) else Color.Gray,
                        )
                    }
                }
                add {
                    MapControlButton(
                        onClick = viewModel::clearMeasurement,
                        enabled = !measurement.isEmpty,
                        label = stringResource(R.string.clear),
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.clear_measurement),
                            tint = if (!measurement.isEmpty) Color(0xFF007AFF) else Color.Gray,
                        )
                    }
                }
                add {
                    MapControlButton(
                        onClick = { showSaveRoute = true },
                        enabled = !measurement.isEmpty,
                        label = stringResource(R.string.save),
                    ) {
                        Icon(
                            Icons.Default.BookmarkBorder,
                            contentDescription = stringResource(R.string.save_current_route),
                            tint = if (!measurement.isEmpty) Color(0xFF007AFF) else Color.Gray,
                        )
                    }
                }
            }
            measurementButtons.forEachIndexed { index, button ->
                AnimatedVisibility(
                    visible = measurement.isMeasuring,
                    enter = controlsEnter(index * 30),
                    exit = controlsExit,
                ) {
                    button()
                }
            }
            MapControlButton(onClick = { showSavedRoutes = true }) {
                Icon(Icons.Default.Bookmark, contentDescription = stringResource(R.string.saved_routes))
            }
            MapControlButton(onClick = { showTools = !showTools }) {
                AnimatedContent(
                    targetState = showTools,
                    transitionSpec = {
                        (fadeIn(tween(150)) + scaleIn(initialScale = 0.6f, animationSpec = tween(150)))
                            .togetherWith(fadeOut(tween(100)) + scaleOut(targetScale = 0.6f, animationSpec = tween(100)))
                    },
                    label = "moreToolsIcon",
                ) { expanded ->
                    Icon(
                        imageVector = if (expanded) {
                            if (isLandscape) Icons.Default.ChevronLeft else Icons.Default.ExpandLess
                        } else {
                            Icons.Default.MoreHoriz
                        },
                        contentDescription = stringResource(R.string.more_map_tools),
                    )
                }
            }
            data class ToolButtonSpec(val visible: Boolean, val content: @Composable () -> Unit)
            val extraToolButtons = buildList {
                add(
                    ToolButtonSpec(true) {
                        MapControlButton(
                            onClick = {
                                if (isPickingOffline) {
                                    isPickingOffline = false
                                } else {
                                    isPickingOffline = true
                                    if (guideTips.take("offline")) {
                                        guideTip = "Move and zoom until the dashed box covers your area."
                                    }
                                }
                            },
                            label = stringResource(R.string.download),
                        ) {
                            Icon(
                                Icons.Outlined.Download,
                                contentDescription = stringResource(R.string.download_current_area),
                                tint = if (isPickingOffline) Color(0xFFFF9500) else Color.Black,
                            )
                        }
                    },
                )
                add(
                    ToolButtonSpec(isPickingOffline) {
                        MapControlButton(onClick = { showOfflineRegions = true }, label = stringResource(R.string.regions)) {
                            Icon(
                                Icons.Default.Inbox,
                                contentDescription = stringResource(R.string.offline_regions),
                                tint = Color(0xFF007AFF),
                            )
                        }
                    },
                )
                add(
                    ToolButtonSpec(true) {
                        MapControlButton(onClick = { showLegend = true }, label = stringResource(R.string.symbols)) {
                            Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = stringResource(R.string.legend))
                        }
                    },
                )
                add(
                    ToolButtonSpec(true) {
                        MapControlButton(onClick = viewModel::toggleSlope, label = stringResource(R.string.slope)) {
                            Icon(
                                painterResource(R.drawable.ic_slope),
                                contentDescription = stringResource(R.string.slope),
                                tint = if (slopeVisible) Color(0xFFFF9500) else Color.Black,
                            )
                        }
                    },
                )
            }
            extraToolButtons.forEachIndexed { index, spec ->
                AnimatedVisibility(
                    visible = showTools && spec.visible,
                    enter = controlsEnter(index * 30),
                    exit = controlsExit,
                ) {
                    spec.content()
                }
            }
        }

        if (measurement.totalMeters > 0) {
            val badgeModifier = if (isLandscape) {
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 76.dp)
            } else {
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = statusBarInset + 12.dp)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = badgeModifier
                    .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            ) {
                Column(
                    modifier = Modifier.clickable(enabled = elevation.hasData) { showElevation = true },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    measurement.name?.let {
                        Text(
                            text = it,
                            color = Color.Black,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Text(
                        text = DistanceMeasurement.formatDistance(measurement.totalMeters),
                        color = Color.Black,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    AnimatedVisibility(
                        visible = elevation.hasData,
                        enter = fadeIn(tween(220)) + expandVertically(tween(220), expandFrom = Alignment.Top),
                        exit = fadeOut(tween(160)) + shrinkVertically(tween(160), shrinkTowards = Alignment.Top),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                "↑ ${elevation.ascent.toInt()} m  ↓ ${elevation.descent.toInt()} m",
                                color = Color.DarkGray,
                                style = MaterialTheme.typography.labelSmall,
                            )
                            if (elevation.isPartial) {
                                Icon(
                                    Icons.Default.WarningAmber,
                                    contentDescription = stringResource(R.string.part_of_this_route_is_outside_the_covered_area_so_the),
                                    tint = Color(0xFFFF9500),
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                    }
                }
                IconButton(
                    onClick = { viewModel.clearMeasurement() },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.clear_measurement),
                        tint = Color.DarkGray,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        ScaleBar(
            metersPerPixel = metersPerPixel,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 28.dp),
        )
        if (showZoom) {
            Text(
                "$tileZ/$tileColumn/$tileRow",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 4.dp)
                    .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                color = Color.Black,
            )
        }
        guideTip?.let {
            GuideTipBadge(
                it,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 98.dp, start = 28.dp, end = 28.dp),
            )
        }

        IconButton(
            onClick = { showAbout = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 6.dp, bottom = 15.dp)
                .size(44.dp),
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = stringResource(R.string.about),
                tint = Color.Gray,
            )
        }

        if (isPickingOffline) {
            val estimatedBytes = offlinePreviewBounds?.let { bounds ->
                TilePyramid.estimateBytes(
                    bounds.latitudeNorth,
                    bounds.longitudeEast,
                    bounds.latitudeSouth,
                    bounds.longitudeWest,
                )
            } ?: 0L
            val exceedsGuard = estimatedBytes > TilePyramid.MAX_DOWNLOAD_BYTES
            val insufficientStorage = !exceedsGuard &&
                estimatedBytes > StatFs(context.filesDir.path).availableBytes
            val downloadDisabled = exceedsGuard || insufficientStorage
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 74.dp, start = 16.dp, end = 16.dp)
                    .widthIn(max = 300.dp)
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "\u2248 ${Formatter.formatShortFileSize(context, estimatedBytes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                )
                if (exceedsGuard) {
                    Text(
                        stringResource(R.string.this_area_is_too_large_to_download_zoom_in_and_try_a),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red,
                        textAlign = TextAlign.Center,
                    )
                } else if (insufficientStorage) {
                    Text(
                        stringResource(R.string.not_enough_free_space_on_this_device_to_download_this_area),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red,
                        textAlign = TextAlign.Center,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (downloadDisabled) Color.Transparent else Color(0xFFFF9500).copy(alpha = 0.15f),
                        )
                        .clickable(enabled = !downloadDisabled) {
                            pendingOfflineBounds = mapState.currentOfflineBounds()
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.ArrowCircleDown,
                        contentDescription = null,
                        tint = if (downloadDisabled) Color.Gray else Color(0xFFFF9500),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.download_this_area),
                        color = if (downloadDisabled) Color.Gray else Color(0xFFFF9500),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }

    if (showElevation && elevation.hasData) {
        ElevationProfileSheet(state = elevation, onDismiss = { showElevation = false })
    }
    if (showLegend) LegendSheet { showLegend = false }
    if (showGuide) {
        OnboardingSheet { showGuide = false }
    }
    if (showAbout) {
        AboutSheet(
            onDismiss = { showAbout = false },
            onShowGuide = { showAbout = false; showGuide = true },
            onShowDebug = { showAbout = false; showDebug = true },
        )
    }
    if (showDebug) {
        DebugSheet(
            showZoom = showZoom,
            onShowZoomChanged = { showZoom = it; debugSettings.showZoomBadge = it },
            onDismiss = { showDebug = false },
        )
    }
    if (showSearch) {
        PlaceSearchSheet(
            query = searchQuery,
            results = searchResults,
            onQueryChanged = viewModel::setSearchQuery,
            onSelect = {
                viewModel.selectPlace(it)
                showSearch = false
            },
            onSave = viewModel::savePlace,
            onDismiss = { showSearch = false },
        )
    }
    if (showSavedRoutes) {
        SavedRoutesSheet(
            featured = viewModel.featuredRoutes,
            saved = savedRoutes,
            onLoad = {
                viewModel.loadRoute(it)
                showSavedRoutes = false
            },
            onRename = viewModel::renameRoute,
            onDelete = viewModel::deleteRoute,
            onDismiss = { showSavedRoutes = false },
        )
    }
    if (showSaveRoute) {
        NameDialog(
            title = stringResource(R.string.name_route),
            onConfirm = {
                viewModel.saveRoute(it)
                showSaveRoute = false
            },
            onDismiss = { showSaveRoute = false },
        )
    }
    selectedPin?.let { pin ->
        PinDetailDialog(
            pin = pin,
            onSave = { name, notes -> viewModel.updatePin(pin, name, notes) },
            onDelete = { viewModel.deletePin(pin) },
            onDismiss = { selectedPinId = null },
        )
    }
    if (showOfflineRegions) {
        OfflineRegionsSheet(
            regions = offlineRegions,
            onPause = viewModel::pauseOfflineRegion,
            onResume = viewModel::resumeOfflineRegion,
            onDelete = viewModel::deleteOfflineRegion,
            onDismiss = { showOfflineRegions = false },
        )
    }
    pendingOfflineBounds?.let { bounds ->
        NameDialog(
            title = stringResource(R.string.name_offline_map),
            onConfirm = {
                viewModel.startOfflineDownload(it, bounds)
                pendingOfflineBounds = null
                isPickingOffline = false
                showOfflineRegions = true
            },
            onDismiss = { pendingOfflineBounds = null },
        )
    }
    offlineError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearOfflineError,
            title = { Text(stringResource(R.string.offline_download)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::clearOfflineError) { Text(stringResource(R.string.ok)) }
            },
        )
    }
}

@Composable
private fun OfflinePreviewOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val insetX = size.width * 0.1f
        val insetY = size.height * 0.1f
        val topLeft = androidx.compose.ui.geometry.Offset(insetX, insetY)
        val rectSize = androidx.compose.ui.geometry.Size(size.width - insetX * 2, size.height - insetY * 2)
        drawRect(
            color = Color(0xFF007AFF).copy(alpha = 0.1f),
            topLeft = topLeft,
            size = rectSize,
        )
        drawRect(
            color = Color(0xFF007AFF),
            topLeft = topLeft,
            size = rectSize,
            style = Stroke(
                width = 3.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 8.dp.toPx())),
            ),
        )
    }
}

@Composable
private fun MapControlsBar(
    isLandscape: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (isLandscape) {
        Row(modifier = modifier, verticalAlignment = Alignment.Top) { content() }
    } else {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}

@Composable
private fun MapControlButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    label: String? = null,
    content: @Composable () -> Unit,
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Column(
        modifier = if (isLandscape) {
            // Labeled buttons already have a wider 64dp column, so a smaller end padding
            // keeps their spacing visually consistent with the unlabeled buttons.
            Modifier.padding(end = if (label != null) 6.dp else 10.dp)
        } else {
            Modifier.padding(bottom = if (label != null) 6.dp else 10.dp)
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(shape = CircleShape, shadowElevation = 4.dp, color = Color.White) {
            IconButton(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.size(48.dp),
                content = content,
            )
        }
        if (label != null) {
            Text(
                text = label,
                color = Color.Black,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                modifier = Modifier
                    .padding(top = 1.dp)
                    .width(64.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ScaleBar(metersPerPixel: Double, modifier: Modifier = Modifier) {
    val metersPerDp = metersPerPixel * LocalDensity.current.density
    val scale = calculateScaleBar(metersPerDp)
    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = scale.label,
            color = Color.Black,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Canvas(Modifier.width(scale.widthDp.dp).height(8.dp)) {
            val strokeWidth = 2.dp.toPx()
            val y = size.height / 2
            drawLine(Color.Black, start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = strokeWidth)
            drawLine(Color.Black, start = androidx.compose.ui.geometry.Offset(strokeWidth / 2, 0f),
                end = androidx.compose.ui.geometry.Offset(strokeWidth / 2, size.height),
                strokeWidth = strokeWidth)
            drawLine(Color.Black, start = androidx.compose.ui.geometry.Offset(size.width - strokeWidth / 2, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width - strokeWidth / 2, size.height),
                strokeWidth = strokeWidth)
        }
    }
}

/**
 * Slippy-map tile z/x/y containing [latitude]/[longitude] at the given (fractional) MapLibre camera
 * [zoom]. MapLibre's zoom is calibrated for 512px tiles; our raster sources use 256px tiles, so the
 * tile z actually requested is one level deeper than the floored camera zoom.
 */
internal fun tileCoordinate(latitude: Double, longitude: Double, zoom: Double): Triple<Int, Int, Int> {
    val z = floor(zoom).toInt() + 1
    val n = 2.0.pow(z)
    val column = floor((longitude + 180.0) / 360.0 * n).toInt()
    val latRad = Math.toRadians(latitude)
    val row = floor((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n).toInt()
    return Triple(z, column, row)
}

internal data class ScaleBarScale(val widthDp: Float, val label: String)

internal fun calculateScaleBar(metersPerDp: Double): ScaleBarScale {
    val niceDistances = doubleArrayOf(
        20.0, 50.0, 100.0, 200.0, 500.0, 1_000.0, 2_000.0, 5_000.0,
        10_000.0, 20_000.0, 50_000.0, 100_000.0, 200_000.0, 500_000.0,
    )
    if (metersPerDp <= 0.0) return ScaleBarScale(60f, "")
    val maxMeters = metersPerDp * 120.0
    val distance = niceDistances.lastOrNull { it <= maxMeters } ?: niceDistances.first()
    val label = if (distance >= 1_000) {
        "${(distance / 1_000).toInt()} km"
    } else {
        "${distance.toInt()} m"
    }
    return ScaleBarScale((distance / metersPerDp).toFloat(), label)
}

@Composable
private fun MapLibreView(
    holder: MapHolder,
    measurement: MeasurementState,
    pins: List<SavedPin>,
    selectedPlace: PlaceResult?,
    selectedPlaceToken: Int,
    routeFitVersion: Int,
    isPickingOffline: Boolean,
    onStrokeFinished: (List<GeoCoordinate>) -> Unit,
    onPreviewChanged: (Double?) -> Unit,
    onDropPin: (GeoCoordinate) -> Unit,
    onPinSelected: (java.util.UUID) -> Unit,
    onSaveSearchPlace: (PlaceResult) -> Unit,
    onDismissSearchPlace: () -> Unit,
    onCameraChanged: (CameraPosition, Double) -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    val container = remember {
        holder.createContainer(
            context = context,
            onCameraChanged = onCameraChanged,
            onStrokeFinished = onStrokeFinished,
            onPreviewChanged = onPreviewChanged,
            onDropPin = onDropPin,
            onPinSelected = onPinSelected,
            onSaveSearchPlace = onSaveSearchPlace,
            onDismissSearchPlace = onDismissSearchPlace,
        )
    }
    val mapView = container.mapView

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    KartverketTileProxy.restartIfNeeded()
                    mapView.onStart()
                }
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            holder.detach()
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = { container },
        update = {
            holder.setMeasuring(measurement.isMeasuring)
            holder.updateRoute(measurement.coordinates, measurement.committedMeters)
            holder.updatePins(pins)
            holder.showSearchPlace(selectedPlace, selectedPlaceToken)
            holder.fitRoute(measurement.coordinates, routeFitVersion)
            holder.setOfflinePreview(isPickingOffline)
        },
        modifier = Modifier.fillMaxSize(),
    )
}

private class MapHolder {
    private var mapView: MapView? = null
    private var container: MapContainerView? = null
    private var map: MapLibreMap? = null
    private var slopeVisible = false
    private var tracking = false
    private var measuring = false
    private var routeCoordinates: List<GeoCoordinate> = emptyList()
    private var routeMeters: Double = 0.0
    private var pins: List<SavedPin> = emptyList()
    private var pinMarkers: Map<java.util.UUID, Marker> = emptyMap()
    private var searchMarker: Marker? = null
    private var selectedSearchPlace: PlaceResult? = null
    private var shownSearchPlaceToken = -1
    private var lastFitVersion = 0
    private var offlinePreview = false
    private var distanceMarkerIconCache: Set<String> = emptySet()

    fun createContainer(
        context: android.content.Context,
        onCameraChanged: (CameraPosition, Double) -> Unit,
        onStrokeFinished: (List<GeoCoordinate>) -> Unit,
        onPreviewChanged: (Double?) -> Unit,
        onDropPin: (GeoCoordinate) -> Unit,
        onPinSelected: (java.util.UUID) -> Unit,
        onSaveSearchPlace: (PlaceResult) -> Unit,
        onDismissSearchPlace: () -> Unit,
    ): MapContainerView {
        val mapView = MapView(context).also { it.onCreate(Bundle()) }
        val captureView = MeasureCaptureView(context).apply {
            this.onStrokeFinished = onStrokeFinished
            this.onPreviewChanged = onPreviewChanged
        }
        val container = MapContainerView(context, mapView, captureView)
        this.mapView = mapView
        this.container = container
        mapView.getMapAsync { readyMap ->
            map = readyMap
            container.bindMap(readyMap)
            val density = context.resources.displayMetrics.density
            val statusBarInsetPx = ViewCompat.getRootWindowInsets(mapView)
                ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top
                ?: (24 * density).toInt()
            val isLandscape = context.resources.configuration.orientation ==
                Configuration.ORIENTATION_LANDSCAPE
            // In landscape the button bar now sits at the top-left, so the compass
            // stays at the top-right on the same row, next to the buttons.
            val compassTopMargin = if (isLandscape) {
                statusBarInsetPx + (6 * density).toInt()
            } else {
                (56 * density).toInt()
            }
            readyMap.uiSettings.apply {
                isLogoEnabled = false
                isAttributionEnabled = false
                isCompassEnabled = true
                setCompassGravity(Gravity.TOP or Gravity.END)
                setCompassMargins(0, compassTopMargin, (12 * density).toInt(), 0)
                isTiltGesturesEnabled = false
            }
            readyMap.setInfoWindowAdapter { marker ->
                val place = selectedSearchPlace
                if (marker !== searchMarker || place == null) {
                    null
                } else {
                    searchCallout(
                        context = context,
                        place = place,
                        onSave = {
                            removeSearchMarker()
                            onSaveSearchPlace(place)
                        },
                        onDismiss = {
                            removeSearchMarker()
                            onDismissSearchPlace()
                        },
                    )
                }
            }
            readyMap.setLatLngBoundsForCameraTarget(
                LatLngBounds.Builder()
                    .include(LatLng(40.0, -10.0))
                    .include(LatLng(76.0, 50.0))
                    .build(),
            )
            readyMap.cameraPosition = CameraPosition.Builder()
                .target(LatLng(65.0, 16.5))
                .zoom(3.4)
                .build()
            fun reportCameraPosition() {
                val camera = readyMap.cameraPosition
                val latitude = camera.target?.latitude ?: 0.0
                onCameraChanged(camera, readyMap.projection.getMetersPerPixelAtLatitude(latitude))
            }
            reportCameraPosition()
            readyMap.addOnCameraMoveListener {
                reportCameraPosition()
                updateDistanceMarkers()
            }
            readyMap.setOnMarkerClickListener { marker ->
                val pin = pinMarkers.entries
                    .firstOrNull { it.value.id == marker.id }
                    ?.key
                    ?.let { id -> pins.firstOrNull { it.id == id } }
                if (pin != null) {
                    onPinSelected(pin.id)
                    true
                } else {
                    false
                }
            }
            readyMap.addOnMapLongClickListener { coordinate ->
                if (measuring || offlinePreview) return@addOnMapLongClickListener false
                onDropPin(GeoCoordinate(coordinate.latitude, coordinate.longitude))
                true
            }
            readyMap.setStyle(Style.Builder().fromJson(MapStyle.json())) {
                addMeasurementLayers(it)
                setSlopeVisible(slopeVisible)
                setTracking(tracking)
                setMeasuring(measuring)
                renderRoute()
                syncPinMarkers()
            }
        }
        return container
    }

    fun detach() {
        map = null
        mapView = null
        container = null
        pins = emptyList()
        pinMarkers = emptyMap()
        searchMarker = null
        selectedSearchPlace = null
        distanceMarkerIconCache = emptySet()
    }

    fun setSlopeVisible(visible: Boolean) {
        slopeVisible = visible
        val visibility = if (visible) Property.VISIBLE else Property.NONE
        map?.style?.getLayerAs<RasterLayer>(MapStyle.NORWAY_SLOPE_LAYER)
            ?.setProperties(org.maplibre.android.style.layers.PropertyFactory.visibility(visibility))
        map?.style?.getLayerAs<RasterLayer>(MapStyle.SWEDEN_SLOPE_LAYER)
            ?.setProperties(org.maplibre.android.style.layers.PropertyFactory.visibility(visibility))
    }

    fun setMeasuring(enabled: Boolean) {
        measuring = enabled
        map?.uiSettings?.apply {
            // Single-finger gestures must stay disabled while measuring so they don't
            // fight with the capture view's own line-drawing touch handling. Pinch-zoom
            // and two-finger rotate are safe to keep native since they're only ever
            // triggered with 2+ pointers, distinct from the single-finger draw gesture.
            isScrollGesturesEnabled = !enabled
            isZoomGesturesEnabled = true
            isRotateGesturesEnabled = true
            isDoubleTapGesturesEnabled = !enabled
            isQuickZoomGesturesEnabled = !enabled
        }

        container?.captureView?.apply {
            anchor = routeCoordinates.lastOrNull()
            setMeasuring(enabled)
        }
    }

    fun setOfflinePreview(visible: Boolean) {
        offlinePreview = visible
    }

    fun currentOfflineBounds(): LatLngBounds? {
        val bounds = map?.projection?.visibleRegion?.latLngBounds ?: return null
        val latitudeInset = bounds.latitudeSpan * 0.1
        val longitudeInset = bounds.longitudeSpan * 0.1
        return LatLngBounds.from(
            bounds.latitudeNorth - latitudeInset,
            bounds.longitudeEast - longitudeInset,
            bounds.latitudeSouth + latitudeInset,
            bounds.longitudeWest + longitudeInset,
        )
    }

    fun updateRoute(coordinates: List<GeoCoordinate>, meters: Double) {
        if (routeCoordinates == coordinates && routeMeters == meters) return
        routeCoordinates = coordinates
        routeMeters = meters
        container?.captureView?.anchor = coordinates.lastOrNull()
        renderRoute()
    }

    private fun renderRoute() {
        val coordinates = routeCoordinates
        val style = map?.style ?: return
        val routeSource = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE) ?: return
        if (coordinates.size >= 2) {
            routeSource.setGeoJson(
                LineString.fromLngLats(coordinates.map { Point.fromLngLat(it.longitude, it.latitude) }),
            )
            style.getSourceAs<GeoJsonSource>(START_SOURCE)?.setGeoJson(
                Point.fromLngLat(coordinates.first().longitude, coordinates.first().latitude),
            )
            style.getSourceAs<GeoJsonSource>(END_SOURCE)?.setGeoJson(
                Point.fromLngLat(coordinates.last().longitude, coordinates.last().latitude),
            )
        } else {
            val empty = FeatureCollection.fromFeatures(arrayOf())
            routeSource.setGeoJson(empty)
            style.getSourceAs<GeoJsonSource>(START_SOURCE)?.setGeoJson(empty)
            style.getSourceAs<GeoJsonSource>(END_SOURCE)?.setGeoJson(empty)
        }
        updateDistanceMarkers()
    }

    private fun updateDistanceMarkers() {
        val readyMap = map ?: return
        val style = readyMap.style ?: return
        val spacing = DistanceMeasurement.markerSpacing(routeMeters, readyMap.cameraPosition.zoom)
        val markers = DistanceMeasurement.markers(routeCoordinates, spacing)
        val context = mapView?.context
        if (context != null) {
            markers.map { DistanceMeasurement.markerLabel(it.meters) }.distinct().forEach { label ->
                if (label !in distanceMarkerIconCache) {
                    style.addImage(distanceMarkerImageId(label), distanceMarkerBitmap(context, label))
                    distanceMarkerIconCache += label
                }
            }
        }
        val features = markers.map { marker ->
            Feature.fromGeometry(
                Point.fromLngLat(marker.coordinate.longitude, marker.coordinate.latitude),
            ).apply {
                addStringProperty("icon", distanceMarkerImageId(DistanceMeasurement.markerLabel(marker.meters)))
            }
        }
        style.getSourceAs<GeoJsonSource>(DISTANCE_MARKERS_SOURCE)
            ?.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    private fun distanceMarkerImageId(label: String) = "distance-marker-$label"

    private fun distanceMarkerBitmap(context: android.content.Context, label: String): Bitmap {
        val density = context.resources.displayMetrics.density
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textAlign = Paint.Align.CENTER
            textSize = 11 * density * context.resources.configuration.fontScale
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val width = (textPaint.measureText(label) + 14 * density)
        val height = 24 * density
        val bitmap = createBitmap(
            width.toInt().coerceAtLeast(1),
            height.toInt().coerceAtLeast(1),
        )
        val canvas = android.graphics.Canvas(bitmap)
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(255, 149, 0)
            style = Paint.Style.STROKE
            strokeWidth = 2 * density
        }
        val radius = height / 2
        val inset = strokePaint.strokeWidth / 2
        canvas.drawRoundRect(inset, inset, width - inset, height - inset, radius, radius, circlePaint)
        canvas.drawRoundRect(inset, inset, width - inset, height - inset, radius, radius, strokePaint)
        val baseline = height / 2 - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(label, width / 2, baseline, textPaint)
        return bitmap
    }

    private fun addMeasurementLayers(style: Style) {
        val routeSource = GeoJsonSource(ROUTE_SOURCE)
        val startSource = GeoJsonSource(START_SOURCE)
        val endSource = GeoJsonSource(END_SOURCE)
        val distanceMarkersSource = GeoJsonSource(DISTANCE_MARKERS_SOURCE)
        style.addSource(routeSource)
        style.addSource(startSource)
        style.addSource(endSource)
        style.addSource(distanceMarkersSource)
        distanceMarkerIconCache = emptySet()

        style.addLayer(
            LineLayer("route-casing", ROUTE_SOURCE).withProperties(
                lineColor(android.graphics.Color.WHITE),
                lineWidth(7f),
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )
        style.addLayer(
            LineLayer("route-line", ROUTE_SOURCE).withProperties(
                lineColor(android.graphics.Color.rgb(255, 149, 0)),
                lineWidth(4f),
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )
        style.addLayer(
            CircleLayer("route-start-layer", START_SOURCE).withProperties(
                circleRadius(6f),
                circleColor(android.graphics.Color.WHITE),
                circleStrokeColor(android.graphics.Color.rgb(255, 149, 0)),
                circleStrokeWidth(2.5f),
            ),
        )
        style.addLayer(
            CircleLayer("route-end-layer", END_SOURCE).withProperties(
                circleRadius(6f),
                circleColor(android.graphics.Color.rgb(255, 149, 0)),
                circleStrokeColor(android.graphics.Color.WHITE),
                circleStrokeWidth(2.5f),
            ),
        )
        style.addLayer(
            SymbolLayer("distance-markers-layer", DISTANCE_MARKERS_SOURCE).withProperties(
                iconImage(Expression.get("icon")),
                iconAllowOverlap(true),
                iconIgnorePlacement(true),
            ),
        )
    }

    fun updatePins(pins: List<SavedPin>) {
        if (this.pins == pins && pinMarkers.size == pins.size) return
        this.pins = pins
        syncPinMarkers()
    }

    private fun syncPinMarkers() {
        val readyMap = map ?: return
        pinMarkers.values.forEach(readyMap::removeMarker)
        val context = mapView?.context ?: return
        val icon = savedPinMarkerIcon(context)
        pinMarkers = pins.associate { pin ->
            pin.id to readyMap.addMarker(
                MarkerOptions()
                    .position(LatLng(pin.coordinate.latitude, pin.coordinate.longitude))
                    .icon(icon),
            )
        }
    }

    private fun savedPinMarkerIcon(
        context: android.content.Context,
    ): org.maplibre.android.annotations.Icon {
        val density = context.resources.displayMetrics.density
        fun pixels(dp: Float) = dp * density
        val size = pixels(36f).toInt()
        val center = pixels(18f)
        val bitmap = createBitmap(size, size)
        val canvas = android.graphics.Canvas(bitmap)

        canvas.drawCircle(
            center,
            center,
            pixels(14f),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(88, 86, 214)
                setShadowLayer(pixels(2f), 0f, pixels(1f), 0x40000000)
            },
        )
        canvas.drawCircle(
            center,
            center,
            pixels(13f),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = pixels(2f)
            },
        )
        canvas.drawPath(
            android.graphics.Path().apply {
                moveTo(pixels(13f), pixels(11f))
                lineTo(pixels(23f), pixels(11f))
                lineTo(pixels(23f), pixels(25f))
                lineTo(center, pixels(22.8f))
                lineTo(pixels(13f), pixels(25f))
                close()
            },
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.FILL
            },
        )
        return IconFactory.getInstance(context).fromBitmap(bitmap)
    }

    fun showSearchPlace(place: PlaceResult?, token: Int) {
        val readyMap = map ?: return
        if (shownSearchPlaceToken == token) return
        shownSearchPlaceToken = token
        searchMarker?.let(readyMap::removeMarker)
        searchMarker = null
        selectedSearchPlace = place
        if (place != null) {
            searchMarker = readyMap.addMarker(
                MarkerOptions()
                    .position(LatLng(place.coordinate.latitude, place.coordinate.longitude))
                    .title(place.name)
                    .snippet(place.subtitle.takeIf(String::isNotBlank))
                    .icon(searchMarkerIcon(mapView?.context ?: return)),
            )
            searchMarker?.let(readyMap::selectMarker)
            readyMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(place.coordinate.latitude, place.coordinate.longitude),
                    12.0,
                ),
                700,
            )
        }
    }

    private fun searchMarkerIcon(context: android.content.Context): org.maplibre.android.annotations.Icon {
        val density = context.resources.displayMetrics.density
        fun pixels(dp: Float) = dp * density
        val size = pixels(36f).toInt()
        val bitmap = createBitmap(size, size)
        val canvas = android.graphics.Canvas(bitmap)
        val center = pixels(18f)

        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(242, 140, 40)
            setShadowLayer(pixels(2f), 0f, pixels(1f), 0x40000000)
        }
        canvas.drawCircle(center, center, pixels(14f), fill)
        canvas.drawCircle(
            center,
            center,
            pixels(14f),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = pixels(2f)
            },
        )

        val glyph = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
        canvas.drawCircle(center, pixels(15f), pixels(5.5f), glyph)
        canvas.drawPath(
            android.graphics.Path().apply {
                moveTo(pixels(13.8f), pixels(18f))
                lineTo(center, pixels(27f))
                lineTo(pixels(22.2f), pixels(18f))
                close()
            },
            glyph,
        )
        canvas.drawCircle(
            center,
            pixels(15f),
            pixels(2.3f),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(242, 140, 40)
            },
        )
        return IconFactory.getInstance(context).fromBitmap(bitmap)
    }

    private fun searchCallout(
        context: android.content.Context,
        place: PlaceResult,
        onSave: () -> Unit,
        onDismiss: () -> Unit,
    ): android.view.View {
        val density = context.resources.displayMetrics.density
        fun pixels(dp: Int) = (dp * density).toInt()

        fun actionButton(
            imageResource: Int,
            description: String,
            onClick: () -> Unit,
        ) = ImageButton(context).apply {
            setImageResource(imageResource)
            contentDescription = description
            background = null
            scaleType = ImageView.ScaleType.CENTER
            setPadding(pixels(8), pixels(8), pixels(8), pixels(8))
            layoutParams = LinearLayout.LayoutParams(pixels(40), pixels(40))
            setOnClickListener { onClick() }
        }

        val textColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(pixels(190), ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(TextView(context).apply {
                text = place.name
                setTextColor(android.graphics.Color.BLACK)
                textSize = 16f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            })
            if (place.subtitle.isNotBlank()) {
                addView(TextView(context).apply {
                    text = place.subtitle
                    setTextColor(android.graphics.Color.DKGRAY)
                    textSize = 12f
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                })
            }
        }

        val bubble = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = ViewGroup.LayoutParams(pixels(286), pixels(56))
            minimumWidth = pixels(286)
            minimumHeight = pixels(56)
            elevation = pixels(4).toFloat()
            setPadding(pixels(4), pixels(6), pixels(4), pixels(6))
            background = GradientDrawable().apply {
                color = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
                cornerRadius = pixels(12).toFloat()
                setStroke(pixels(1), 0x26000000)
            }
            addView(actionButton(R.drawable.ic_callout_bookmark, "Save place", onSave))
            addView(textColumn)
            addView(actionButton(R.drawable.ic_callout_close, "Close place", onDismiss))
        }

        return bubble
    }

    private fun removeSearchMarker() {
        searchMarker?.let { marker -> map?.removeMarker(marker) }
        searchMarker = null
        selectedSearchPlace = null
    }

    fun fitRoute(coordinates: List<GeoCoordinate>, version: Int) {
        if (version == lastFitVersion || coordinates.size < 2) return
        val readyMap = map ?: return
        lastFitVersion = version
        val bounds = LatLngBounds.Builder().also { builder ->
            coordinates.forEach { builder.include(LatLng(it.latitude, it.longitude)) }
        }.build()
        readyMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120), 700)
    }

    @Suppress("MissingPermission")
    fun setTracking(enabled: Boolean) {
        tracking = enabled
        val readyMap = map ?: return
        val style = readyMap.style ?: return
        val component = readyMap.locationComponent
        if (enabled) {
            component.activateLocationComponent(
                LocationComponentActivationOptions.builder(mapView?.context ?: return, style).build(),
            )
            component.isLocationComponentEnabled = true
            component.renderMode = RenderMode.COMPASS
            // Automatically done on iOS
            if (readyMap.cameraPosition.zoom < MINIMUM_ZOOM_LEVEL_FOR_TRACKING) {
                component.setCameraMode(
                    CameraMode.TRACKING,
                    object : OnLocationCameraTransitionListener {
                        override fun onLocationCameraTransitionFinished(cameraMode: Int) {
                            component.zoomWhileTracking(DEFAULT_ZOOM_LEVEL_FOR_TRACKING)
                        }

                        override fun onLocationCameraTransitionCanceled(cameraMode: Int) = Unit
                    },
                )
            } else {
                component.cameraMode = CameraMode.TRACKING
            }
        } else if (component.isLocationComponentActivated) {
            component.cameraMode = CameraMode.NONE
            component.isLocationComponentEnabled = false
        }
    }

    companion object {
        private const val ROUTE_SOURCE = "route"
        private const val START_SOURCE = "route-start"
        private const val END_SOURCE = "route-end"
        private const val DISTANCE_MARKERS_SOURCE = "distance-markers"

        // Matches MapLibre iOS's MLNMinimumZoomLevelForUserTracking / MLNDefaultZoomLevelForUserTracking.
        private const val MINIMUM_ZOOM_LEVEL_FOR_TRACKING = 10.5
        private const val DEFAULT_ZOOM_LEVEL_FOR_TRACKING = 14.0
    }
}
