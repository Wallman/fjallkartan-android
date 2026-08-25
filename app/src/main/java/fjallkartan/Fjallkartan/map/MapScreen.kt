package fjallkartan.fjallkartan.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fjallkartan.fjallkartan.measurement.DistanceMeasurement
import fjallkartan.fjallkartan.measurement.GeoCoordinate
import fjallkartan.fjallkartan.measurement.MeasurementState
import kotlin.math.cos
import org.maplibre.android.camera.CameraPosition
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.GeoJsonSource

@Composable
fun MapScreen(viewModel: MapViewModel = viewModel()) {
    val slopeVisible by viewModel.slopeVisible.collectAsStateWithLifecycle()
    val trackingEnabled by viewModel.trackingEnabled.collectAsStateWithLifecycle()
    val measurement by viewModel.measurement.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var latitude by remember { mutableDoubleStateOf(67.0) }
    var zoom by remember { mutableDoubleStateOf(3.4) }
    val mapState = remember { MapHolder() }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions.values.any { it }) {
            mapState.setTracking(true)
        }
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
            onCameraChanged = { camera ->
                latitude = camera.target?.latitude ?: latitude
                zoom = camera.zoom
            },
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MapControlButton(onClick = viewModel::toggleTracking) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = "Track my location",
                    tint = if (trackingEnabled) MaterialTheme.colorScheme.primary else Color.Black,
                )
            }
            MapControlButton(onClick = viewModel::toggleSlope) {
                Icon(
                    Icons.Default.Layers,
                    contentDescription = "Show slope shading",
                    tint = if (slopeVisible) Color(0xFFF28C28) else Color.Black,
                )
            }
            MapControlButton(onClick = viewModel::toggleMeasuring) {
                Icon(
                    Icons.Default.Straighten,
                    contentDescription = "Measure distance",
                    tint = if (measurement.isMeasuring) Color(0xFFF28C28) else Color.Black,
                )
            }
            if (measurement.isMeasuring) {
                MapControlButton(
                    onClick = viewModel::undoMeasurement,
                    enabled = measurement.canUndo,
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo last stroke")
                }
                MapControlButton(
                    onClick = viewModel::clearMeasurement,
                    enabled = !measurement.isEmpty,
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear measurement")
                }
            }
        }

        if (measurement.totalMeters > 0) {
            Text(
                text = DistanceMeasurement.formatDistance(measurement.totalMeters),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 44.dp)
                    .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                color = Color.Black,
                style = MaterialTheme.typography.titleSmall,
            )
        }

        ScaleBar(
            latitude = latitude,
            zoom = zoom,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 44.dp),
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
                .background(Color.White.copy(alpha = 0.82f), CircleShape)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                "Kartverket • Lantmäteriet • NVE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black,
            )
        }
    }
}

@Composable
private fun MapControlButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Surface(shape = CircleShape, shadowElevation = 4.dp, color = Color.White) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(48.dp),
            content = content,
        )
    }
}

@Composable
private fun ScaleBar(latitude: Double, zoom: Double, modifier: Modifier = Modifier) {
    val metersPerPixel = 156543.03392 * cos(Math.toRadians(latitude)) / (1 shl zoom.toInt())
    val targetPixels = 90.0
    val rawMeters = metersPerPixel * targetPixels
    val magnitude = Math.pow(10.0, kotlin.math.floor(kotlin.math.log10(rawMeters)))
    val normalized = rawMeters / magnitude
    val nice = when {
        normalized >= 5 -> 5.0
        normalized >= 2 -> 2.0
        else -> 1.0
    } * magnitude
    val label = if (nice >= 1_000) "${(nice / 1_000).toInt()} km" else "${nice.toInt()} m"

    Text(
        text = label,
        modifier = modifier
            .background(Color.White.copy(alpha = 0.85f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = Color.Black,
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
private fun MapLibreView(
    holder: MapHolder,
    measurement: MeasurementState,
    onStrokeFinished: (List<GeoCoordinate>) -> Unit,
    onPreviewChanged: (Double?) -> Unit,
    onCameraChanged: (CameraPosition) -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    val container = remember {
        holder.createContainer(
            context = context,
            onCameraChanged = onCameraChanged,
            onStrokeFinished = onStrokeFinished,
            onPreviewChanged = onPreviewChanged,
        )
    }
    val mapView = container.mapView

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
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

    fun createContainer(
        context: android.content.Context,
        onCameraChanged: (CameraPosition) -> Unit,
        onStrokeFinished: (List<GeoCoordinate>) -> Unit,
        onPreviewChanged: (Double?) -> Unit,
    ): MapContainerView {
        val mapView = MapView(context).also { it.onCreate(Bundle()) }
        val markerOverlay = DistanceMarkerOverlay(context)
        val captureView = MeasureCaptureView(context).apply {
            this.onStrokeFinished = onStrokeFinished
            this.onPreviewChanged = onPreviewChanged
        }
        val container = MapContainerView(context, mapView, markerOverlay, captureView)
        this.mapView = mapView
        this.container = container
        mapView.getMapAsync { readyMap ->
            map = readyMap
            container.bindMap(readyMap)
            readyMap.uiSettings.apply {
                isLogoEnabled = false
                isAttributionEnabled = false
                isTiltGesturesEnabled = false
            }
            readyMap.setLatLngBoundsForCameraTarget(
                LatLngBounds.Builder()
                    .include(LatLng(40.0, -10.0))
                    .include(LatLng(76.0, 50.0))
                    .build(),
            )
            readyMap.cameraPosition = CameraPosition.Builder()
                .target(LatLng(67.0, 16.0))
                .zoom(3.4)
                .build()
            readyMap.addOnCameraMoveListener {
                onCameraChanged(readyMap.cameraPosition)
                updateDistanceMarkers()
                container.invalidateMarkers()
            }
            readyMap.setStyle(Style.Builder().fromJson(MapStyle.json())) {
                addMeasurementLayers(it)
                setSlopeVisible(slopeVisible)
                setTracking(tracking)
                setMeasuring(measuring)
                renderRoute()
            }
        }
        return container
    }

    fun detach() {
        map = null
        mapView = null
        container = null
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
            isScrollGesturesEnabled = !enabled
            isZoomGesturesEnabled = !enabled
            isRotateGesturesEnabled = !enabled
            isDoubleTapGesturesEnabled = !enabled
            isQuickZoomGesturesEnabled = !enabled
        }
        container?.captureView?.apply {
            anchor = routeCoordinates.lastOrNull()
            setMeasuring(enabled)
        }
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
        val spacing = DistanceMeasurement.markerSpacing(routeMeters, readyMap.cameraPosition.zoom)
        container?.updateMarkers(DistanceMeasurement.markers(routeCoordinates, spacing))
    }

    private fun addMeasurementLayers(style: Style) {
        val routeSource = GeoJsonSource(ROUTE_SOURCE)
        val startSource = GeoJsonSource(START_SOURCE)
        val endSource = GeoJsonSource(END_SOURCE)
        style.addSource(routeSource)
        style.addSource(startSource)
        style.addSource(endSource)

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
                lineColor(android.graphics.Color.rgb(11, 110, 79)),
                lineWidth(4f),
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )
        style.addLayer(
            CircleLayer("route-start-layer", START_SOURCE).withProperties(
                circleRadius(6f),
                circleColor(android.graphics.Color.WHITE),
                circleStrokeColor(android.graphics.Color.rgb(11, 110, 79)),
                circleStrokeWidth(2.5f),
            ),
        )
        style.addLayer(
            CircleLayer("route-end-layer", END_SOURCE).withProperties(
                circleRadius(6f),
                circleColor(android.graphics.Color.rgb(11, 110, 79)),
                circleStrokeColor(android.graphics.Color.WHITE),
                circleStrokeWidth(2.5f),
            ),
        )
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
            component.cameraMode = CameraMode.TRACKING
            component.renderMode = RenderMode.COMPASS
        } else if (component.isLocationComponentActivated) {
            component.cameraMode = CameraMode.NONE
            component.isLocationComponentEnabled = false
        }
    }

    companion object {
        private const val ROUTE_SOURCE = "route"
        private const val START_SOURCE = "route-start"
        private const val END_SOURCE = "route-end"
    }
}
